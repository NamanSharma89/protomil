// src/main/java/com/protomil/core/user/service/AuthenticationService.java
package com.protomil.core.user.service;

import com.protomil.core.config.CognitoProperties;
import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.exception.AuthenticationException;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.exception.ExternalServiceException;
import com.protomil.core.shared.exception.ResourceNotFoundException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.shared.security.JwtTokenManager;
import com.protomil.core.shared.security.SessionManager;
import com.protomil.core.shared.security.TokenPair;
import com.protomil.core.shared.security.UserTokenClaims;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.dto.LoginRequest;
import com.protomil.core.user.dto.LoginResponse;
import com.protomil.core.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final Optional<CognitoIdentityProviderClient> cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final RoleService roleService;
    private final JwtTokenManager jwtTokenManager;
    private final SessionManager sessionManager;


    public AuthenticationService(
            UserRepository userRepository,
            Optional<CognitoIdentityProviderClient> cognitoClient,
            CognitoProperties cognitoProperties,
            RoleService roleService,
            JwtTokenManager jwtTokenManager,
            SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.cognitoProperties = cognitoProperties;
        this.roleService = roleService;
        this.jwtTokenManager = jwtTokenManager;
        this.sessionManager = sessionManager;
    }

    @LogExecutionTime
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Starting authentication for user: {}", loginRequest.getEmail());

        try {
            // Step 1: Authenticate with Cognito
            CognitoAuthResult cognitoResult = authenticateWithCognito(loginRequest);

            // Step 2: Validate user in local database
            User user = validateLocalUser(loginRequest.getEmail());

            // Step 3: Sync user status between Cognito and local DB
            syncUserStatusWithCognito(user, cognitoResult.getCognitoSub());

            // Step 4: Load user roles and permissions
            List<String> userRoles = roleService.getUserRoleNames(user.getId());

            // Step 5: Create user token claims
            UserTokenClaims userClaims = UserTokenClaims.builder()
                    .cognitoSub(user.getCognitoUserSub())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .department(user.getDepartment())
                    .roles(userRoles)
                    .tokenType("access")
                    .build();

            // Step 6: Generate JWT token pair
            TokenPair tokenPair = jwtTokenManager.generateTokenPair(userClaims);

            // Step 7: Create session
            sessionManager.createSession(user.getId(), userClaims);

            // Step 8: Update last login time
            updateLastLoginTime(user);

            // Step 9: Build successful login response
            LoginResponse response = LoginResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .department(user.getDepartment())
                    .roles(userRoles)
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .expiresIn(tokenPair.getAccessTokenExpiresIn())
                    .loginTime(LocalDateTime.now())
                    .rememberMe(loginRequest.getRememberMe())
                    .build();

            log.info("Authentication successful for user: {} with roles: {}",
                    loginRequest.getEmail(), userRoles);

            return response;

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito authentication failed for user: {} - Error: {}",
                    loginRequest.getEmail(), e.awsErrorDetails().errorMessage());
            throw mapCognitoAuthenticationError(e, loginRequest.getEmail());

        } catch (BusinessException | ResourceNotFoundException e) {
            log.warn("Authentication failed for user: {} - {}", loginRequest.getEmail(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during authentication for user: {}", loginRequest.getEmail(), e);
            throw new AuthenticationException("Authentication failed due to system error");
        }
    }

    @LogExecutionTime
    public LoginResponse refreshToken(String refreshToken, String userId) {
        log.info("Refreshing token for user: {}", userId);

        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            throw new ExternalServiceException("Token refresh not available - Cognito disabled", "Cognito");
        }

        try {
            // Refresh token with Cognito
            InitiateAuthRequest refreshRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(cognitoProperties.getClientId())
                    .authParameters(Map.of("REFRESH_TOKEN", refreshToken))
                    .build();

            InitiateAuthResponse refreshResponse = cognitoClient.get().initiateAuth(refreshRequest);

            // Get user from database
            User user = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

            // Load current user roles
            List<String> userRoles = roleService.getUserRoleNames(user.getId());

            LoginResponse response = LoginResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .department(user.getDepartment())
                    .roles(userRoles)
                    .accessToken(refreshResponse.authenticationResult().accessToken())
                    .refreshToken(refreshResponse.authenticationResult().refreshToken())
                    .expiresIn(refreshResponse.authenticationResult().expiresIn())
                    .loginTime(LocalDateTime.now())
                    .rememberMe(true) // Assume true for refresh
                    .build();

            log.info("Token refresh successful for user: {}", userId);
            return response;

        } catch (CognitoIdentityProviderException e) {
            log.error("Token refresh failed for user: {} - Error: {}", userId, e.awsErrorDetails().errorMessage());
            throw new AuthenticationException("Token refresh failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    private CognitoAuthResult authenticateWithCognito(LoginRequest loginRequest) {
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - simulating authentication for: {}", loginRequest.getEmail());
            return createMockCognitoResult();
        }

        try {
            log.debug("Authenticating with Cognito for user: {}", loginRequest.getEmail());

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", loginRequest.getEmail());
            authParams.put("PASSWORD", loginRequest.getPassword());

            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .clientId(cognitoProperties.getClientId())
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(authParams)
                    .build();

            AdminInitiateAuthResponse authResponse = cognitoClient.get().adminInitiateAuth(authRequest);

            if (authResponse.authenticationResult() == null) {
                throw new AuthenticationException("Authentication failed - no result from Cognito");
            }

            log.debug("Cognito authentication successful for user: {}", loginRequest.getEmail());

            return CognitoAuthResult.builder()
                    .cognitoSub(extractCognitoSub(authResponse))
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .refreshToken(authResponse.authenticationResult().refreshToken())
                    .idToken(authResponse.authenticationResult().idToken())
                    .expiresIn(authResponse.authenticationResult().expiresIn())
                    .build();

        } catch (UserNotConfirmedException e) {
            log.warn("User email not confirmed for: {}", loginRequest.getEmail());
            throw new BusinessException("Email not verified. Please check your email for verification code.");

        } catch (NotAuthorizedException e) {
            log.warn("Invalid credentials for user: {}", loginRequest.getEmail());
            throw new AuthenticationException("Invalid email or password");

        } catch (UserNotFoundException e) {
            log.warn("User not found in Cognito: {}", loginRequest.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }
    }

    private User validateLocalUser(String email) {
        log.debug("Validating user in local database: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found in local database: {}", email);
                    return new AuthenticationException("Invalid email or password");
                });

        // Validate user status
        switch (user.getStatus()) {
            case PENDING_VERIFICATION:
                log.warn("User attempting login with unverified email: {}", email);
                throw new BusinessException("Email not verified. Please check your email for verification code.");

            case PENDING_APPROVAL:
                log.warn("User attempting login while pending approval: {}", email);
                throw new BusinessException("Your account is pending administrator approval.");

            case SUSPENDED:
                log.warn("Suspended user attempting login: {}", email);
                throw new BusinessException("Your account has been suspended. Please contact support.");

            case DELETED:
                log.warn("Deleted user attempting login: {}", email);
                throw new AuthenticationException("Invalid email or password");

            case INACTIVE:
                log.warn("Inactive user attempting login: {}", email);
                throw new BusinessException("Your account is inactive. Please contact support.");

            case ACTIVE:
                log.debug("User status validation successful: {}", email);
                break;

            default:
                log.error("Unknown user status for user: {} - Status: {}", email, user.getStatus());
                throw new BusinessException("Account status error. Please contact support.");
        }

        return user;
    }

    private void syncUserStatusWithCognito(User user, String cognitoSub) {
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.debug("Skipping Cognito sync - service disabled");
            return;
        }

        try {
            log.debug("Syncing user status with Cognito for user: {}", user.getEmail());

            // Get current user roles
            List<String> userRoles = roleService.getUserRoleNames(user.getId());
            String rolesString = String.join(",", userRoles);

            // Update custom attributes in Cognito
            List<AttributeType> attributes = List.of(
                    AttributeType.builder()
                            .name("custom:approval_status")
                            .value(user.getStatus().name())
                            .build(),
                    AttributeType.builder()
                            .name("custom:local_user_id")
                            .value(user.getId().toString())
                            .build(),
                    AttributeType.builder()
                            .name("custom:user_roles")
                            .value(rolesString)
                            .build()
            );

            AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(user.getEmail())
                    .userAttributes(attributes)
                    .build();

            cognitoClient.get().adminUpdateUserAttributes(updateRequest);

            log.debug("User status synced successfully with Cognito for user: {}", user.getEmail());

        } catch (CognitoIdentityProviderException e) {
            log.warn("Failed to sync user status with Cognito for user: {} - Error: {}",
                    user.getEmail(), e.awsErrorDetails().errorMessage());
            // Don't fail login for sync issues, just log the warning
        }
    }

    private void updateLastLoginTime(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        log.debug("Updated last login time for user: {}", user.getEmail());
    }

    private String extractCognitoSub(AdminInitiateAuthResponse authResponse) {
        // Extract subject from ID token claims
        // This is a simplified version - in production, you'd decode the JWT properly
        return "cognito-sub-" + System.currentTimeMillis();
    }

    private CognitoAuthResult createMockCognitoResult() {
        return CognitoAuthResult.builder()
                .cognitoSub("mock-cognito-sub-" + System.currentTimeMillis())
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .idToken("mock-id-token")
                .expiresIn(1800) // 30 minutes
                .build();
    }

    private AuthenticationException mapCognitoAuthenticationError(CognitoIdentityProviderException e, String email) {
        String errorCode = e.awsErrorDetails().errorCode();

        return switch (errorCode) {
            case "NotAuthorizedException" -> {
                log.warn("Authentication failed - invalid credentials for: {}", email);
                yield new AuthenticationException("Invalid email or password");
            }
            case "UserNotConfirmedException" -> {
                log.warn("Authentication failed - user not confirmed for: {}", email);
                yield new AuthenticationException("Email not verified. Please check your email for verification code.");
            }
            case "UserNotFoundException" -> {
                log.warn("Authentication failed - user not found for: {}", email);
                yield new AuthenticationException("Invalid email or password");
            }
            case "PasswordResetRequiredException" -> {
                log.warn("Authentication failed - password reset required for: {}", email);
                yield new AuthenticationException("Password reset required. Please contact support.");
            }
            case "TooManyRequestsException" -> {
                log.warn("Authentication failed - too many requests for: {}", email);
                yield new AuthenticationException("Too many login attempts. Please try again later.");
            }
            default -> {
                log.error("Authentication failed - Cognito error for: {} - Error: {}", email, e.awsErrorDetails().errorMessage());
                yield new AuthenticationException("Authentication failed: " + e.awsErrorDetails().errorMessage());
            }
        };
    }

    @LogExecutionTime
    public void logout(UUID userId) {
        log.info("Processing logout for user: {}", userId);

        try {
            // Invalidate session
            sessionManager.invalidateSession(userId);

            // TODO: Revoke tokens in Cognito if needed

            log.info("Logout completed successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error during logout for user: {}", userId, e);
            // Don't throw exception for logout errors
        }
    }

    // Inner class for Cognito authentication result
    private static class CognitoAuthResult {
        private final String cognitoSub;
        private final String accessToken;
        private final String refreshToken;
        private final String idToken;
        private final Integer expiresIn;

        private CognitoAuthResult(String cognitoSub, String accessToken, String refreshToken, String idToken, Integer expiresIn) {
            this.cognitoSub = cognitoSub;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.idToken = idToken;
            this.expiresIn = expiresIn;
        }

        public static CognitoAuthResultBuilder builder() {
            return new CognitoAuthResultBuilder();
        }

        public String getCognitoSub() { return cognitoSub; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getIdToken() { return idToken; }
        public Integer getExpiresIn() { return expiresIn; }

        public static class CognitoAuthResultBuilder {
            private String cognitoSub;
            private String accessToken;
            private String refreshToken;
            private String idToken;
            private Integer expiresIn;

            public CognitoAuthResultBuilder cognitoSub(String cognitoSub) {
                this.cognitoSub = cognitoSub;
                return this;
            }

            public CognitoAuthResultBuilder accessToken(String accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public CognitoAuthResultBuilder refreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public CognitoAuthResultBuilder idToken(String idToken) {
                this.idToken = idToken;
                return this;
            }

            public CognitoAuthResultBuilder expiresIn(Integer expiresIn) {
                this.expiresIn = expiresIn;
                return this;
            }

            public CognitoAuthResult build() {
                return new CognitoAuthResult(cognitoSub, accessToken, refreshToken, idToken, expiresIn);
            }
        }
    }
}