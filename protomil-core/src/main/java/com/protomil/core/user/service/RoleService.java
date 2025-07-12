// src/main/java/com/protomil/core/user/service/RoleService.java (Enhanced)
package com.protomil.core.user.service;

import com.protomil.core.shared.domain.enums.UserRoleStatus;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.Role;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.domain.UserRole;
import com.protomil.core.user.repository.RoleRepository;
import com.protomil.core.user.repository.UserRepository;
import com.protomil.core.user.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class RoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(UserRoleRepository userRoleRepository,
                       RoleRepository roleRepository,
                       UserRepository userRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @LogExecutionTime
    public List<String> getUserRoleNames(UUID userId) {
        log.debug("Getting role names for user: {}", userId);

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndStatus(userId, UserRoleStatus.ACTIVE);

        List<String> roleNames = userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());

        log.debug("Found {} active roles for user {}: {}", roleNames.size(), userId, roleNames);

        return roleNames;
    }

    @LogExecutionTime
    public List<Role> getUserRoles(UUID userId) {
        log.debug("Getting roles for user: {}", userId);

        List<UserRole> userRoles = userRoleRepository.findByUserIdAndStatus(userId, UserRoleStatus.ACTIVE);

        return userRoles.stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public boolean hasRole(UUID userId, String roleName) {
        log.debug("Checking if user {} has role: {}", userId, roleName);

        List<String> userRoles = getUserRoleNames(userId);
        boolean hasRole = userRoles.contains(roleName);

        log.debug("User {} has role {}: {}", userId, roleName, hasRole);

        return hasRole;
    }

    @LogExecutionTime
    public boolean hasAnyRole(UUID userId, String... roleNames) {
        log.debug("Checking if user {} has any of roles: {}", userId, List.of(roleNames));

        List<String> userRoles = getUserRoleNames(userId);
        Set<String> requiredRoles = Set.of(roleNames);

        boolean hasAnyRole = userRoles.stream()
                .anyMatch(requiredRoles::contains);

        log.debug("User {} has any of roles {}: {}", userId, List.of(roleNames), hasAnyRole);

        return hasAnyRole;
    }

    @LogExecutionTime
    public boolean hasPermission(UUID userId, String resource, String action) {
        // TODO: Implement permission-based authorization in Phase 3
        log.debug("Checking permission for user {} on resource {} with action {}", userId, resource, action);

        List<String> userRoles = getUserRoleNames(userId);

        // Basic role hierarchy for now
        if (userRoles.contains("ADMIN")) {
            return true; // Admin has all permissions
        }

        if (userRoles.contains("SUPERVISOR")) {
            // Supervisor has limited permissions
            return !action.equals("DELETE") || resource.equals("USER_MANAGEMENT");
        }

        if (userRoles.contains("TECHNICIAN")) {
            // Technician has read/write on job cards, read on equipment
            return resource.equals("JOB_CARD") ||
                    (resource.equals("EQUIPMENT") && action.equals("READ"));
        }

        if (userRoles.contains("PENDING_USER")) {
            // Pending users can only access profile
            return resource.equals("USER_PROFILE") && action.equals("READ");
        }

        log.debug("Permission denied for user {} on resource {} with action {}", userId, resource, action);
        return false;
    }

    @LogExecutionTime
    public String getHighestRole(UUID userId) {
        List<String> userRoles = getUserRoleNames(userId);

        // Role hierarchy (highest to lowest)
        if (userRoles.contains("ADMIN")) return "ADMIN";
        if (userRoles.contains("SUPERVISOR")) return "SUPERVISOR";
        if (userRoles.contains("TECHNICIAN")) return "TECHNICIAN";
        if (userRoles.contains("VIEWER")) return "VIEWER";
        if (userRoles.contains("PENDING_USER")) return "PENDING_USER";

        return "NO_ROLE";
    }
}