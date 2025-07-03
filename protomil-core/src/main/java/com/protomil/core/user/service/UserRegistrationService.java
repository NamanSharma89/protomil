// com/protomil/core/user/service/UserRegistrationService.java
package com.protomil.core.user.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
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
    private final boolean cognitoEnabled;

    @Value("${aws.cognito.userPoolId:}")
    private String userPoolId;

    @Value("${aws.cognito.clientId:}")
    private String clientId;

    public UserRegistrationService(UserRepository userRepository,
                                   Optional<CognitoIdentityProviderClient> cognitoClient,
                                   ApplicationEventPublisher eventPublisher,
                                   @Value("${aws.cognito.enabled:false}") boolean cognitoEnabled) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.eventPublisher = eventPublisher;
        this.cognitoEnabled = cognitoEnabled;
    }

    @LogExecutionTime
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {

        // Check if user already exists in our database
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt for existing email: {}", request.getEmail());
            throw new BusinessException("User with this email already exists");
        }

        // Check employee ID if provided
        if (request.getEmployeeId() != null &&
                userRepository.existsByEmployeeId(request.getEmployeeId())) {
            log.warn("Registration attempt for existing employee ID: {}", request.getEmployeeId());
            throw new BusinessException("User with this employee ID already exists");
        }

        try {
            // Create user in Cognito (if enabled)
            String cognitoUserSub = createCognitoUser(request);

            // Create user in our database
            User user = User.builder()
                    .cognitoUserSub(cognitoUserSub)
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .employeeId(request.getEmployeeId())
                    .department(request.getDepartment())
                    .status(cognitoEnabled ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("User created with ID: {} and Cognito Sub: {}", savedUser.getId(), cognitoUserSub);

            // Publish registration event
            eventPublisher.publishEvent(new UserRegisteredEvent(savedUser));

            return UserRegistrationResponse.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .status(savedUser.getStatus())
                    .registeredAt(savedUser.getCreatedAt())
                    .emailVerificationRequired(cognitoEnabled) // Only if Cognito is enabled
                    .adminApprovalRequired(cognitoEnabled) // Only if Cognito is enabled
                    .build();

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to create Cognito user for email: {}", request.getEmail(), e);
            throw new ExternalServiceException(
                    "Failed to register user with Cognito: " + e.awsErrorDetails().errorMessage(),
                    "AWS Cognito",
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error during user registration for email: {}", request.getEmail(), e);
            throw new BusinessException("Failed to register user due to system error");
        }
    }

    private String createCognitoUser(UserRegistrationRequest request) {
        // If Cognito is disabled, return a mock sub
        if (!cognitoEnabled || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - generating mock user sub for email: {}", request.getEmail());
            return "mock-cognito-sub-" + UUID.randomUUID().toString();
        }

        // Validate required configuration
        if (userPoolId == null || userPoolId.trim().isEmpty()) {
            log.error("AWS Cognito User Pool ID is not configured");
            throw new BusinessException("Cognito configuration is incomplete");
        }

        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("email", request.getEmail());
            attributes.put("given_name", request.getFirstName());
            attributes.put("family_name", request.getLastName());

            // Only add phone number if provided and valid
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                // Ensure phone number is in E.164 format for Cognito
                String formattedPhone = formatPhoneNumber(request.getPhoneNumber());
                attributes.put("phone_number", formattedPhone);
            }

            AdminCreateUserRequest cognitoRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.getEmail())
                    .userAttributes(
                            attributes.entrySet().stream()
                                    .map(entry -> AttributeType.builder()
                                            .name(entry.getKey())
                                            .value(entry.getValue())
                                            .build())
                                    .toList()
                    )
                    .temporaryPassword(request.getPassword())
                    .messageAction(MessageActionType.SUPPRESS) // Don't send welcome email yet
                    .forceAliasCreation(false) // Don't force alias creation
                    .build();

            log.debug("Creating Cognito user for email: {}", request.getEmail());
            AdminCreateUserResponse response = cognitoClient.get().adminCreateUser(cognitoRequest);

            String userSub = response.user().attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .findFirst()
                    .map(AttributeType::value)
                    .orElseThrow(() -> new ExternalServiceException(
                            "Failed to get Cognito user sub from response",
                            "AWS Cognito"
                    ));

            log.info("Successfully created Cognito user with sub: {} for email: {}", userSub, request.getEmail());
            return userSub;

        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito service error for email: {} - Error: {}",
                    request.getEmail(), e.awsErrorDetails().errorMessage(), e);

            // Handle specific Cognito errors
            String errorCode = e.awsErrorDetails().errorCode();
            String errorMessage = switch (errorCode) {
                case "UsernameExistsException" -> "User with this email already exists in the system";
                case "InvalidPasswordException" -> "Password does not meet security requirements";
                case "InvalidParameterException" -> "Invalid user information provided";
                case "UserPoolTaggingException" -> "User pool configuration error";
                default -> "User registration failed: " + e.awsErrorDetails().errorMessage();
            };

            throw new ExternalServiceException(errorMessage, "AWS Cognito", e);
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Basic phone number formatting for Cognito (E.164 format)
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        // If it doesn't start with +, assume it's an Indian number and add +91
        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("91") && cleaned.length() == 12) {
                cleaned = "+" + cleaned;
            } else if (cleaned.length() == 10) {
                cleaned = "+91" + cleaned;
            } else {
                // Default to adding + if not present
                cleaned = "+" + cleaned;
            }
        }

        return cleaned;
    }

    /**
     * Enable a user in Cognito after admin approval
     */
    public void enableCognitoUser(String email) {
        if (!cognitoEnabled || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - skipping user enable for email: {}", email);
            return;
        }

        try {
            AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            cognitoClient.get().adminEnableUser(request);
            log.info("Successfully enabled Cognito user: {}", email);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to enable Cognito user: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);
            throw new ExternalServiceException(
                    "Failed to enable user in Cognito: " + e.awsErrorDetails().errorMessage(),
                    "AWS Cognito",
                    e
            );
        }
    }

    /**
     * Disable a user in Cognito when suspended
     */
    public void disableCognitoUser(String email) {
        if (!cognitoEnabled || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - skipping user disable for email: {}", email);
            return;
        }

        try {
            AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            cognitoClient.get().adminDisableUser(request);
            log.info("Successfully disabled Cognito user: {}", email);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to disable Cognito user: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);
            throw new ExternalServiceException(
                    "Failed to disable user in Cognito: " + e.awsErrorDetails().errorMessage(),
                    "AWS Cognito",
                    e
            );
        }
    }

    /**
     * Delete a user from Cognito when user is deleted from system
     */
    public void deleteCognitoUser(String email) {
        if (!cognitoEnabled || cognitoClient.isEmpty()) {
            log.info("Cognito disabled - skipping user delete for email: {}", email);
            return;
        }

        try {
            AdminDeleteUserRequest request = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            cognitoClient.get().adminDeleteUser(request);
            log.info("Successfully deleted Cognito user: {}", email);

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to delete Cognito user: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage(), e);
            // Don't throw exception for delete operations - log and continue
            log.warn("Continuing despite Cognito delete failure for user: {}", email);
        }
    }
}