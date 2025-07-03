// com/protomil/core/user/service/UserRegistrationService.java
package com.protomil.core.user.service;

import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.events.UserRegisteredEvent;
import com.protomil.core.user.repository.UserRepository;
import com.protomil.core.shared.domain.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final CognitoIdentityProviderClient cognitoClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    public UserRegistrationService(UserRepository userRepository,
                                   CognitoIdentityProviderClient cognitoClient,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.eventPublisher = eventPublisher;
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
            // Create user in Cognito
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
                    .status(UserStatus.PENDING_APPROVAL) // Admin approval still required
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
                    .emailVerificationRequired(true) // Handled by Cognito
                    .adminApprovalRequired(true)
                    .build();

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to create Cognito user for email: {}", request.getEmail(), e);
            throw new BusinessException("Failed to register user: " + e.awsErrorDetails().errorMessage());
        }
    }

    private String createCognitoUser(UserRegistrationRequest request) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", request.getEmail());
        attributes.put("given_name", request.getFirstName());
        attributes.put("family_name", request.getLastName());
        attributes.put("phone_number", request.getPhoneNumber());

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
                .build();

        AdminCreateUserResponse response = cognitoClient.adminCreateUser(cognitoRequest);
        return response.user().attributes().stream()
                .filter(attr -> "sub".equals(attr.name()))
                .findFirst()
                .map(AttributeType::value)
                .orElseThrow(() -> new BusinessException("Failed to get Cognito user sub"));
    }
}