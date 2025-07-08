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
                triggerEmailVerification(request.getEmail());
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
            Map<String, String> attributes = buildUserAttributes(request);

            AdminCreateUserRequest cognitoRequest = AdminCreateUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(request.getEmail())
                    .userAttributes(attributes.entrySet().stream()
                            .map(entry -> AttributeType.builder()
                                    .name(entry.getKey())
                                    .value(entry.getValue())
                                    .build())
                            .toList())
                    .temporaryPassword(request.getPassword())
                    .messageAction(MessageActionType.SUPPRESS) // We'll handle email verification separately
                    .forceAliasCreation(false)
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            log.debug("Creating Cognito user for email: {}", request.getEmail());
            AdminCreateUserResponse response = cognitoClient.get().adminCreateUser(cognitoRequest);

            String userSub = response.user().attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .findFirst()
                    .map(AttributeType::value)
                    .orElseThrow(() -> new ExternalServiceException(
                            "Failed to get Cognito user sub from response", "AWS Cognito"));

            log.info("Successfully created Cognito user with sub: {} for email: {}", userSub, request.getEmail());
            return userSub;

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito service error for email: {} - Error: {}",
                    request.getEmail(), e.awsErrorDetails().errorMessage(), e);

            String errorCode = e.awsErrorDetails().errorCode();
            String errorMessage = mapCognitoError(errorCode, e);
            throw new ExternalServiceException(errorMessage, "AWS Cognito", e);
        }
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

    private Map<String, String> buildUserAttributes(UserRegistrationRequest request) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", request.getEmail());
        attributes.put("email_verified", "false"); // Will be verified via email
        attributes.put("given_name", request.getFirstName());
        attributes.put("family_name", request.getLastName());

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String formattedPhone = formatPhoneNumber(request.getPhoneNumber());
            attributes.put("phone_number", formattedPhone);
            attributes.put("phone_number_verified", "false");
        }

        // Custom attributes for Protomil
        if (request.getEmployeeId() != null) {
            attributes.put("custom:employee_id", request.getEmployeeId());
        }
        if (request.getDepartment() != null) {
            attributes.put("custom:department", request.getDepartment());
        }

        return attributes;
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