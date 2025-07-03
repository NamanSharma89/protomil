// com/protomil/core/user/service/UserService.java
package com.protomil.core.user.service;

import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.exception.ExceptionUtils;
import com.protomil.core.shared.exception.ResourceNotFoundException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.Role;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.domain.UserRole;
import com.protomil.core.user.dto.UserApprovalRequest;
import com.protomil.core.user.dto.UserResponse;
import com.protomil.core.user.events.UserApprovedEvent;
import com.protomil.core.user.events.UserRoleAssignedEvent;
import com.protomil.core.user.repository.RoleRepository;
import com.protomil.core.user.repository.UserRepository;
import com.protomil.core.user.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CognitoIdentityProviderClient cognitoClient;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       CognitoIdentityProviderClient cognitoClient,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.cognitoClient = cognitoClient;
        this.eventPublisher = eventPublisher;
    }

    @LogExecutionTime
    public UserResponse approveUser(UUID userId, UserApprovalRequest request, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BusinessException("User is not in pending approval status");
        }

        // Update user status
        user.setStatus(UserStatus.ACTIVE);

        // Assign initial roles
        assignRolesToUser(user, request.getRoleIds(), getCurrentUserId(authentication));

        // Enable user in Cognito
        enableUserInCognito(user);

        User savedUser = userRepository.save(user);

        // Publish approval event
        eventPublisher.publishEvent(new UserApprovedEvent(savedUser, getCurrentUserId(authentication)));

        log.info("User approved: userId={}, approvedBy={}", userId, authentication.getName());

        return convertToUserResponse(savedUser);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString())
                        .addDetail("searchType", "id")
                        .addDetail("operation", "getUserById"));
        return convertToUserResponse(user);
    }


    @LogExecutionTime
    @Transactional(readOnly = true)
    public UserResponse getUserByCognitoSub(String cognitoSub) {
        User user = userRepository.findByCognitoUserSub(cognitoSub)
                .orElseThrow(() -> ExceptionUtils.userNotFoundByCognitoSub(cognitoSub));
        return convertToUserResponse(user);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToUserResponse);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable)
                .map(this::convertToUserResponse);
    }

    @LogExecutionTime
    public UserResponse suspendUser(UUID userId, String reason, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException("User is already suspended");
        }

        user.setStatus(UserStatus.SUSPENDED);
        User savedUser = userRepository.save(user);

        // Disable user in Cognito
        disableUserInCognito(user);

        log.info("User suspended: userId={}, reason={}, suspendedBy={}",
                userId, reason, authentication.getName());

        return convertToUserResponse(savedUser);
    }

    @LogExecutionTime
    public UserResponse reactivateUser(UUID userId, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getStatus() != UserStatus.SUSPENDED && user.getStatus() != UserStatus.INACTIVE) {
            throw new BusinessException("User is not in suspended or inactive status");
        }

        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        // Enable user in Cognito
        enableUserInCognito(user);

        log.info("User reactivated: userId={}, reactivatedBy={}", userId, authentication.getName());

        return convertToUserResponse(savedUser);
    }

    @LogExecutionTime
    public void assignRoleToUser(UUID userId, UUID roleId, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        // Check if user already has this role
        boolean hasRole = userRoleRepository.existsByUserAndRoleAndStatus(
                user, role, com.protomil.core.shared.domain.enums.UserRoleStatus.ACTIVE);

        if (hasRole) {
            throw new BusinessException("User already has this role assigned");
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(getCurrentUserId(authentication))
                .assignedAt(LocalDateTime.now())
                .status(com.protomil.core.shared.domain.enums.UserRoleStatus.ACTIVE)
                .build();

        userRoleRepository.save(userRole);

        // Publish role assignment event
        eventPublisher.publishEvent(new UserRoleAssignedEvent(user, role, getCurrentUserId(authentication)));

        log.info("Role assigned: userId={}, roleId={}, assignedBy={}",
                userId, roleId, authentication.getName());
    }

    @LogExecutionTime
    public void revokeRoleFromUser(UUID userId, UUID roleId, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        UserRole userRole = userRoleRepository.findByUserAndRoleAndStatus(
                        user, role, com.protomil.core.shared.domain.enums.UserRoleStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("User does not have this role assigned"));

        userRole.setStatus(com.protomil.core.shared.domain.enums.UserRoleStatus.INACTIVE);
        userRoleRepository.save(userRole);

        log.info("Role revoked: userId={}, roleId={}, revokedBy={}",
                userId, roleId, authentication.getName());
    }

    private void assignRolesToUser(User user, List<UUID> roleIds, UUID assignedBy) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("One or more roles not found");
        }

        List<UserRole> userRoles = roles.stream()
                .map(role -> UserRole.builder()
                        .user(user)
                        .role(role)
                        .assignedBy(assignedBy)
                        .assignedAt(LocalDateTime.now())
                        .status(com.protomil.core.shared.domain.enums.UserRoleStatus.ACTIVE)
                        .build())
                .collect(Collectors.toList());

        userRoleRepository.saveAll(userRoles);
    }

    private void enableUserInCognito(User user) {
        try {
            AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                    .userPoolId(getCognitoUserPoolId())
                    .username(user.getEmail())
                    .build();

            cognitoClient.adminEnableUser(request);
            log.info("User enabled in Cognito: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to enable user in Cognito: {}", user.getEmail(), e);
            // Don't fail the operation, but log for manual intervention
        }
    }

    private void disableUserInCognito(User user) {
        try {
            // Implementation for disabling user in Cognito
            log.info("User disabled in Cognito: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to disable user in Cognito: {}", user.getEmail(), e);
        }
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .cognitoUserSub(user.getCognitoUserSub())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeId(user.getEmployeeId())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(user.getUserRoles().stream()
                        .filter(ur -> ur.getStatus() == com.protomil.core.shared.domain.enums.UserRoleStatus.ACTIVE)
                        .map(ur -> ur.getRole().getName())
                        .collect(Collectors.toList()))
                .build();
    }

    private UUID getCurrentUserId(Authentication authentication) {
        // Implementation depends on your JWT token structure
        // This is a placeholder - you'll need to extract user ID from JWT claims
        return UUID.randomUUID(); // Replace with actual implementation
    }

    private String getCognitoUserPoolId() {
        // Return from configuration
        return "your-user-pool-id"; // Replace with actual value from config
    }
}