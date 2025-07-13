// src/main/java/com/protomil/core/user/service/UserStatusSyncService.java
package com.protomil.core.user.service;

import com.protomil.core.config.CognitoProperties;
import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.exception.ExternalServiceException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.dto.UserStatusValidationResult;
import com.protomil.core.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserStatusSyncService {

    private final UserRepository userRepository;
    private final Optional<CognitoIdentityProviderClient> cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final RoleService roleService;

    public UserStatusSyncService(UserRepository userRepository,
                                 Optional<CognitoIdentityProviderClient> cognitoClient,
                                 CognitoProperties cognitoProperties,
                                 RoleService roleService) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
        this.cognitoProperties = cognitoProperties;
        this.roleService = roleService;
    }

    @LogExecutionTime
    public void syncUserStatusToCognito(User user) {
        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.debug("Skipping Cognito sync - service disabled for user: {}", user.getEmail());
            return;
        }

        log.debug("Syncing user status to Cognito for user: {} with status: {}",
                user.getEmail(), user.getStatus());

        try {
            // Update custom attributes in Cognito
            updateCognitoUserAttributes(user);

            // Update user enabled/disabled status based on local status
            updateCognitoUserEnabledStatus(user);

            log.info("User status synced successfully to Cognito for user: {}", user.getEmail());

        } catch (UserNotFoundException e) {
            log.error("User not found in Cognito during sync: {} - This indicates data inconsistency",
                    user.getEmail());
            throw new ExternalServiceException("User not found in Cognito: " + user.getEmail(), "Cognito");

        } catch (CognitoIdentityProviderException e) {
            log.error("Failed to sync user status to Cognito for user: {} - Error: {}",
                    user.getEmail(), e.awsErrorDetails().errorMessage());
            throw new ExternalServiceException("Cognito sync failed: " + e.awsErrorDetails().errorMessage(), "Cognito");
        }
    }

    @LogExecutionTime
    public UserStatusValidationResult validateUserStatusConsistency(String email) {
        log.debug("Validating user status consistency for: {}", email);

        // Get user from local database
        Optional<User> localUser = userRepository.findByEmail(email);
        if (localUser.isEmpty()) {
            log.warn("User not found in local database: {}", email);
            return UserStatusValidationResult.builder()
                    .email(email)
                    .consistent(false)
                    .localUserExists(false)
                    .cognitoUserExists(false)
                    .issue("User not found in local database")
                    .build();
        }

        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.debug("Cognito disabled, skipping remote validation for: {}", email);
            return UserStatusValidationResult.builder()
                    .email(email)
                    .consistent(true)
                    .localUserExists(true)
                    .localStatus(localUser.get().getStatus())
                    .cognitoUserExists(false)
                    .issue("Cognito service disabled")
                    .build();
        }

        // Get user from Cognito
        CognitoUserInfo cognitoInfo = getCognitoUserInfo(email);

        if (!cognitoInfo.exists()) {
            log.warn("User not found in Cognito: {} - Data inconsistency detected", email);
            return UserStatusValidationResult.builder()
                    .email(email)
                    .consistent(false)
                    .localUserExists(true)
                    .localStatus(localUser.get().getStatus())
                    .cognitoUserExists(false)
                    .issue("User exists in local DB but not in Cognito")
                    .build();
        }

        // Compare statuses
        boolean consistent = isStatusConsistent(localUser.get(), cognitoInfo);

        UserStatusValidationResult result = UserStatusValidationResult.builder()
                .email(email)
                .consistent(consistent)
                .localUserExists(true)
                .localStatus(localUser.get().getStatus())
                .cognitoUserExists(true)
                .cognitoStatus(cognitoInfo.getUserStatus())
                .cognitoEnabled(cognitoInfo.isEnabled())
                .cognitoApprovalStatus(cognitoInfo.getApprovalStatus())
                .build();

        if (!consistent) {
            result.setIssue(buildInconsistencyMessage(localUser.get(), cognitoInfo));
        }

        log.debug("User status validation completed for: {} - Consistent: {}", email, consistent);

        return result;
    }

    @LogExecutionTime
    @Async
    public void syncUserStatusFromCognito(String email) {
        log.debug("Syncing user status from Cognito to local DB for: {}", email);

        if (!cognitoProperties.isEnabled() || cognitoClient.isEmpty()) {
            log.debug("Skipping Cognito sync - service disabled");
            return;
        }

        try {
            Optional<User> localUser = userRepository.findByEmail(email);
            if (localUser.isEmpty()) {
                log.warn("Cannot sync from Cognito - user not found in local DB: {}", email);
                return;
            }

            CognitoUserInfo cognitoInfo = getCognitoUserInfo(email);
            if (!cognitoInfo.exists()) {
                log.warn("Cannot sync from Cognito - user not found in Cognito: {}", email);
                return;
            }

            // Update local user based on Cognito status
            User user = localUser.get();
            boolean updated = false;

            // Update approval status if different
            UserStatus cognitoApprovalStatus = mapCognitoToLocalStatus(cognitoInfo);
            if (user.getStatus() != cognitoApprovalStatus) {
                log.info("Updating local user status from {} to {} based on Cognito for: {}",
                        user.getStatus(), cognitoApprovalStatus, email);
                user.setStatus(cognitoApprovalStatus);
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
                log.info("User status synced from Cognito to local DB for: {}", email);
            }

        } catch (Exception e) {
            log.error("Failed to sync user status from Cognito for: {}", email, e);
        }
    }

    @LogExecutionTime
    public void forceStatusSync(UUID userId, UserStatus newStatus) {
        log.info("Forcing status sync for user: {} to status: {}", userId, newStatus);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);

        // Sync to Cognito
        try {
            syncUserStatusToCognito(user);
            log.info("Force status sync completed successfully for user: {} from {} to {}",
                    userId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Force status sync to Cognito failed for user: {}", userId, e);
            // Don't rollback local change, but log the inconsistency
        }
    }

    private void updateCognitoUserAttributes(User user) {
        List<String> userRoles = roleService.getUserRoleNames(user.getId());
        String rolesString = String.join(",", userRoles);

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
                        .build(),
                AttributeType.builder()
                        .name("custom:department")
                        .value(user.getDepartment() != null ? user.getDepartment() : "")
                        .build()
        );

        AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(cognitoProperties.getUserPoolId())
                .username(user.getEmail())
                .userAttributes(attributes)
                .build();

        cognitoClient.get().adminUpdateUserAttributes(updateRequest);

        log.debug("Updated Cognito user attributes for: {} with roles: {}", user.getEmail(), rolesString);
    }

    private void updateCognitoUserEnabledStatus(User user) {
        boolean shouldBeEnabled = user.getStatus() == UserStatus.ACTIVE;

        if (shouldBeEnabled) {
            AdminEnableUserRequest enableRequest = AdminEnableUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(user.getEmail())
                    .build();

            cognitoClient.get().adminEnableUser(enableRequest);
            log.debug("Enabled user in Cognito: {}", user.getEmail());

        } else {
            AdminDisableUserRequest disableRequest = AdminDisableUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(user.getEmail())
                    .build();

            cognitoClient.get().adminDisableUser(disableRequest);
            log.debug("Disabled user in Cognito: {}", user.getEmail());
        }
    }

    private CognitoUserInfo getCognitoUserInfo(String email) {
        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build();

            AdminGetUserResponse response = cognitoClient.get().adminGetUser(getUserRequest);

            // Extract custom attributes
            Map<String, String> attributes = response.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));

            String approvalStatus = attributes.get("custom:approval_status");

            return CognitoUserInfo.builder()
                    .exists(true)
                    .userStatus(response.userStatus())
                    .enabled(response.enabled())
                    .approvalStatus(approvalStatus != null ? approvalStatus : "UNKNOWN")
                    .attributes(attributes)
                    .build();

        } catch (UserNotFoundException e) {
            log.debug("User not found in Cognito: {}", email);
            return CognitoUserInfo.builder()
                    .exists(false)
                    .build();

        } catch (CognitoIdentityProviderException e) {
            log.error("Error getting user info from Cognito for: {} - Error: {}",
                    email, e.awsErrorDetails().errorMessage());
            throw new ExternalServiceException("Failed to get user info from Cognito", "Cognito", e);
        }
    }

    private boolean isStatusConsistent(User localUser, CognitoUserInfo cognitoInfo) {
        // Check if local status matches Cognito approval status
        String cognitoApprovalStatus = cognitoInfo.getApprovalStatus();
        boolean approvalStatusMatch = localUser.getStatus().name().equals(cognitoApprovalStatus);

        // Check if enabled status is consistent
        boolean enabledStatusMatch = (localUser.getStatus() == UserStatus.ACTIVE) == cognitoInfo.isEnabled();

        // Check Cognito user status
        boolean cognitoStatusConsistent = isCognitoUserStatusConsistent(localUser, cognitoInfo);

        return approvalStatusMatch && enabledStatusMatch && cognitoStatusConsistent;
    }

    private boolean isCognitoUserStatusConsistent(User localUser, CognitoUserInfo cognitoInfo) {
        UserStatusType cognitoStatus = cognitoInfo.getUserStatus();

        return switch (localUser.getStatus()) {
            case PENDING_VERIFICATION -> cognitoStatus == UserStatusType.UNCONFIRMED;
            case PENDING_APPROVAL -> cognitoStatus == UserStatusType.CONFIRMED && !cognitoInfo.isEnabled();
            case ACTIVE -> cognitoStatus == UserStatusType.CONFIRMED && cognitoInfo.isEnabled();
            case SUSPENDED -> cognitoStatus == UserStatusType.CONFIRMED && !cognitoInfo.isEnabled();
            case INACTIVE -> cognitoStatus == UserStatusType.CONFIRMED && !cognitoInfo.isEnabled();
            case DELETED -> false; // Deleted users shouldn't exist in Cognito
            case REJECTED -> false;
            case COGNITO_SYNC_FAILURE -> false;
        };
    }

    private UserStatus mapCognitoToLocalStatus(CognitoUserInfo cognitoInfo) {
        String approvalStatus = cognitoInfo.getApprovalStatus();

        try {
            return UserStatus.valueOf(approvalStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown approval status in Cognito: {}, defaulting to PENDING_APPROVAL", approvalStatus);
            return UserStatus.PENDING_APPROVAL;
        }
    }

    private String buildInconsistencyMessage(User localUser, CognitoUserInfo cognitoInfo) {
        StringBuilder message = new StringBuilder("Status inconsistency detected: ");

        message.append(String.format("Local status: %s, ", localUser.getStatus()));
        message.append(String.format("Cognito approval status: %s, ", cognitoInfo.getApprovalStatus()));
        message.append(String.format("Cognito user status: %s, ", cognitoInfo.getUserStatus()));
        message.append(String.format("Cognito enabled: %s", cognitoInfo.isEnabled()));

        return message.toString();
    }

    // Inner classes for data transfer
    private static class CognitoUserInfo {
        private final boolean exists;
        private final UserStatusType userStatus;
        private final boolean enabled;
        private final String approvalStatus;
        private final Map<String, String> attributes;

        private CognitoUserInfo(boolean exists, UserStatusType userStatus, boolean enabled,
                                String approvalStatus, Map<String, String> attributes) {
            this.exists = exists;
            this.userStatus = userStatus;
            this.enabled = enabled;
            this.approvalStatus = approvalStatus;
            this.attributes = attributes;
        }

        public static CognitoUserInfoBuilder builder() {
            return new CognitoUserInfoBuilder();
        }

        // Getters
        public boolean exists() { return exists; }
        public UserStatusType getUserStatus() { return userStatus; }
        public boolean isEnabled() { return enabled; }
        public String getApprovalStatus() { return approvalStatus; }
        public Map<String, String> getAttributes() { return attributes; }

        public static class CognitoUserInfoBuilder {
            private boolean exists;
            private UserStatusType userStatus;
            private boolean enabled;
            private String approvalStatus;
            private Map<String, String> attributes;

            public CognitoUserInfoBuilder exists(boolean exists) {
                this.exists = exists;
                return this;
            }

            public CognitoUserInfoBuilder userStatus(UserStatusType userStatus) {
                this.userStatus = userStatus;
                return this;
            }

            public CognitoUserInfoBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public CognitoUserInfoBuilder approvalStatus(String approvalStatus) {
                this.approvalStatus = approvalStatus;
                return this;
            }

            public CognitoUserInfoBuilder attributes(Map<String, String> attributes) {
                this.attributes = attributes;
                return this;
            }

            public CognitoUserInfo build() {
                return new CognitoUserInfo(exists, userStatus, enabled, approvalStatus, attributes);
            }
        }
    }
}