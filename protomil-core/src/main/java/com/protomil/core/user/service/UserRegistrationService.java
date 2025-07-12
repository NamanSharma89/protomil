package com.protomil.core.user.service;

import com.protomil.core.config.CognitoProperties;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.exception.ExternalServiceException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.events.UserRegisteredEvent;
import com.protomil.core.user.repository.UserRepository;
import com.protomil.core.shared.domain.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final Optional<CognitoIdentityProviderClient> cognitoClient;
    private final ApplicationEventPublisher eventPublisher;
    private final CognitoProperties cognitoProperties;

    public UserRegistrationService(
            UserRepository userRepository,
            Optional<CognitoIdentityProviderClient> cognitoClient,
            ApplicationEventPublisher eventPublisher,
            CognitoProperties cognitoProperties) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.eventPublisher = eventPublisher;
        this.cognitoProperties = cognitoProperties;
    }

    @LogExecutionTime
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        // Check for existing users
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt for existing email: {}", request.getEmail());
            throw new BusinessException("User with this email already exists");
        }

        if (request.getEmployeeId() != null &&
                userRepository.existsByEmployeeId(request.getEmployeeId())) {
            log.warn("Registration attempt for existing employee ID: {}", request.getEmployeeId());
            throw new BusinessException("User with this employee ID already exists");
        }

        try {
            String cognitoUserSub = createCognitoUser(request);

            User user = User.builder()
                    .cognitoUserSub(cognitoUserSub)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .employeeId(request.getEmployeeId())
                    .department(request.getDepartment())
                    .status(determineInitialUserStatus())
                    .build();

            User savedUser = userRepository.save(user);
            log.info("User created with ID: {} and Cognito Sub: {}", savedUser.getId(), cognitoUserSub);

            // Trigger email verification if Cognito is enabled
            if (cognitoProperties.isEnabled() && cognitoProperties.getEmail().isVerificationRequired()) {
                // triggerEmailVerification(request.getEmail());
            }

            eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));

            return UserRegistrationResponse.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .status(savedUser.getStatus())
                    .registeredAt(savedUser.getCreatedAt())
                    .emailVerificationRequired(cognitoProperties.isEnabled() &&
                            cognitoProperties.getEmail().isVerificationRequired())
                    .adminApprovalRequired(cognitoProperties.isEnabled())
                    .build();

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to create Cognito user for email: {}", request.getEmail(), e);
            throw new ExternalServiceException(
                    "Failed to register user with Cognito: " + e.awsErrorDetails().errorMessage(),
                    "AWS Cognito", e);
        } catch (Exception e) {
            log.error("Unexpected error during user registration for email: {}", request.getEmail(), e);
            throw new BusinessException("Failed to register user due to system error");
        }
    }

    private String createCognitoUser(UserRegistrationRequest request) {
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - generating mock user sub for email: {}", request.getEmail());
            return "mock-cognito-sub-" + UUID.randomUUID().toString();
        }

        if (cognitoProperties.getUserPoolId() == null || cognitoProperties.getUserPoolId().trim().isEmpty()) {
            log.error("AWS Cognito User Pool ID is not configured");
            throw new BusinessException("Cognito configuration is incomplete");
        }

        try {
            // Use SignUp instead of AdminCreateUser for proper email verification flow
            Map<String, String> attributes = buildUserAttributes(request);

            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(request.getEmail())
                    .password(request.getPassword())
                    .userAttributes(attributes.entrySet().stream()
                            .map(entry -> AttributeType.builder()
                                    .name(entry.getKey())
                                    .value(entry.getValue())
                                    .build())
                            .toList())
                    .build();

            log.info("Creating Cognito user via SignUp for email: {}", request.getEmail());

            SignUpResponse response = cognitoClient.get().signUp(signUpRequest);

            log.info("SignUp successful. User Sub: {}, Confirmation required: {}",
                    response.userSub(), !response.userConfirmed());

            // Check if email was sent
            if (response.codeDeliveryDetails() != null) {
                log.info("Verification code delivery details - Medium: {}, Destination: {}",
                        response.codeDeliveryDetails().deliveryMedium(),
                        response.codeDeliveryDetails().destination());
            } else {
                log.warn("No code delivery details returned - email may not have been sent");
            }

            return response.userSub();

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito SignUp error for email: {} - Error Code: {}, Error Message: {}",
                    request.getEmail(), e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);

            String errorMessage = mapCognitoError(e.awsErrorDetails().errorCode(), e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e);
        }
    }

    private Map<String, String> buildUserAttributes(UserRegistrationRequest request) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", request.getEmail());
        attributes.put("given_name", request.getFirstName());
        attributes.put("family_name", request.getLastName());

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String formattedPhone = formatPhoneNumber(request.getPhoneNumber());
            attributes.put("phone_number", formattedPhone);
        }

        // Custom attributes with proper prefix
        if (request.getEmployeeId() != null) {
            attributes.put("custom:employee_id", request.getEmployeeId());
        }

        if (request.getDepartment() != null) {
            attributes.put("custom:department", request.getDepartment());
        }

        // Set initial status
        attributes.put("custom:approval_status", UserStatus.PENDING_VERIFICATION.name());

        // Placeholder for local user ID (will be updated after user creation)
        attributes.put("custom:local_user_id", "");

        // Initial empty roles
        attributes.put("custom:user_roles", "");

        return attributes;
    }

    private void triggerEmailVerification(String email) {
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - skipping email verification trigger for: {}", email);
            return;
        }

        try {
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .clientId(cognitoProperties.getClientId())
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .authParameters(Map.of("USERNAME", email))
                    .build();

            // This will trigger the email verification flow
            log.info("Triggering email verification for user: {}", email);

            // Alternative approach: Use ResendConfirmationCode
            ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .build();

            cognitoClient.get().resendConfirmationCode(resendRequest);
            log.info("Email verification code sent to: {}", email);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to trigger email verification for: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);
            // Don't throw exception here - user is already created, this is just verification
        } catch (Exception e) {
            log.error("Unexpected error triggering email verification for: {}", email, e);
        }
    }

    private UserStatus determineInitialUserStatus() {
        if (!cognitoProperties.isEnabled()) {
            return UserStatus.ACTIVE; // Skip verification in dev mode
        }

        if (cognitoProperties.getEmail().isVerificationRequired()) {
            return UserStatus.PENDING_VERIFICATION;
        }

        return UserStatus.PENDING_APPROVAL;
    }

    private String formatPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("91") && cleaned.length() == 12) {
                cleaned = "+" + cleaned;
            } else if (cleaned.length() == 10) {
                cleaned = "+91" + cleaned;
            } else {
                cleaned = "+" + cleaned;
            }
        }

        return cleaned;
    }

    private String mapCognitoError(String errorCode, CognitoIdentityProviderException e) {
        return switch (errorCode) {
            case "UsernameExistsException" ->
                    "User with this email already exists in the system";
            case "InvalidPasswordException" ->
                    "Password does not meet security requirements";
            case "InvalidParameterException" ->
                    "Invalid user information provided";
            case "UserPoolTaggingException" ->
                    "User pool configuration error";
            case "CodeDeliveryFailureException" ->
                    "Failed to send verification email. Please check your email address.";
            case "InvalidEmailRoleAccessPolicyException" ->
                    "Email service configuration error. Please contact support.";
            default ->
                    "User registration failed: " + e.awsErrorDetails().errorMessage();
        };
    }
}