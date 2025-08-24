package com.protomil.core.shared.security;

import com.protomil.core.shared.exception.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the current authenticated user's ID as UUID
     *
     * @return Current user ID as UUID
     * @throws SecurityException if no user is authenticated or user ID cannot be extracted
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        // Handle UserTokenClaims (primary case)
        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            UUID userId = claims.getUserId();
            if (userId == null) {
                throw new SecurityException("User ID is null in token claims");
            }
            return userId;
        }

        // Handle UserDetails (fallback)
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();

            // Try to parse username as UUID if it looks like one
            try {
                return UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                log.debug("Username '{}' is not a valid UUID", username);
                throw new SecurityException("Cannot extract UUID from username: " + username);
            }
        }

        // Handle String principal
        if (principal instanceof String) {
            String principalString = (String) principal;
            try {
                return UUID.fromString(principalString);
            } catch (IllegalArgumentException e) {
                log.debug("Principal string '{}' is not a valid UUID", principalString);
                throw new SecurityException("Cannot parse UUID from principal: " + principalString);
            }
        }

        log.error("Unknown principal type: {}", principal.getClass().getName());
        throw new SecurityException("Unknown principal type, cannot extract user ID");
    }

    /**
     * Gets the current authenticated user's ID as Optional
     *
     * @return Optional containing user ID, empty if no authenticated user
     */
    public static Optional<UUID> getCurrentUserIdOptional() {
        try {
            return Optional.of(getCurrentUserId());
        } catch (SecurityException e) {
            log.debug("No authenticated user found: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the current authenticated user's email
     *
     * @return Current user's email
     * @throws SecurityException if no user is authenticated or email cannot be extracted
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            String email = claims.getEmail();
            if (email == null || email.trim().isEmpty()) {
                throw new SecurityException("Email is null or empty in token claims");
            }
            return email;
        }

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userDetails.getUsername(); // Assuming username is email
        }

        if (principal instanceof String) {
            return (String) principal; // Assuming principal is email
        }

        throw new SecurityException("Cannot extract email from principal");
    }

    /**
     * Gets the current authenticated user's roles
     *
     * @return List of user roles
     */
    public static List<String> getCurrentUserRoles() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            return claims.getRoles() != null ? claims.getRoles() : List.of();
        }

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .toList();
        }

        return List.of();
    }

    /**
     * Gets the current authentication object
     *
     * @return Current Authentication or null if none exists
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Checks if current user has the specified role
     *
     * @param role Role to check
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        List<String> userRoles = getCurrentUserRoles();
        return userRoles.contains(role) || userRoles.contains("ROLE_" + role);
    }

    /**
     * Checks if current user has any of the specified roles
     *
     * @param roles Roles to check
     * @return true if user has at least one of the roles
     */
    public static boolean hasAnyRole(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (userRoles.contains(role) || userRoles.contains("ROLE_" + role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if current user has all of the specified roles
     *
     * @param roles Roles to check
     * @return true if user has all the roles
     */
    public static boolean hasAllRoles(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (!userRoles.contains(role) && !userRoles.contains("ROLE_" + role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the current user's full name
     *
     * @return User's full name or empty string if not available
     */
    public static String getCurrentUserFullName() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            return claims.getFullName();
        }

        return "";
    }

    /**
     * Gets the current user's department
     *
     * @return User's department or null if not available
     */
    public static String getCurrentUserDepartment() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            return claims.getDepartment();
        }

        return null;
    }

    /**
     * Gets the current user's Cognito subject
     *
     * @return Cognito subject or null if not available
     */
    public static String getCurrentUserCognitoSub() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            return claims.getCognitoSub();
        }

        return null;
    }

    /**
     * Gets the complete user token claims
     *
     * @return UserTokenClaims object or null if not available
     */
    public static UserTokenClaims getCurrentUserClaims() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserTokenClaims) {
            return (UserTokenClaims) principal;
        }

        return null;
    }

    /**
     * Checks if there is an authenticated user
     *
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Checks if the current user can access the specified resource
     *
     * @param resourceOwnerId ID of the resource owner
     * @return true if current user can access the resource
     */
    public static boolean canAccessResource(UUID resourceOwnerId) {
        if (resourceOwnerId == null) {
            return false;
        }

        try {
            UUID currentUserId = getCurrentUserId();

            // User can always access their own resources
            if (currentUserId.equals(resourceOwnerId)) {
                return true;
            }

            // Admin and supervisors can access all resources
            return hasAnyRole("ADMIN", "SUPERVISOR");

        } catch (SecurityException e) {
            log.debug("Cannot determine resource access: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates that the current user can modify the specified resource
     *
     * @param resourceOwnerId ID of the resource owner
     * @throws SecurityException if user cannot modify the resource
     */
    public static void validateResourceAccess(UUID resourceOwnerId) {
        if (!canAccessResource(resourceOwnerId)) {
            throw new SecurityException("Access denied to resource owned by user: " + resourceOwnerId);
        }
    }

    /**
     * Gets user ID for auditing purposes - returns system user ID if no authenticated user
     *
     * @return User ID for audit logging
     */
    public static UUID getUserIdForAudit() {
        try {
            return getCurrentUserId();
        } catch (SecurityException e) {
            // Return system user UUID for system operations
            log.debug("No authenticated user for audit, using system user ID");
            return UUID.fromString("00000000-0000-0000-0000-000000000001"); // System user UUID
        }
    }

    /**
     * Checks if the current operation is being performed by the system
     *
     * @return true if system operation
     */
    public static boolean isSystemOperation() {
        try {
            UUID userId = getCurrentUserId();
            UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            return userId != null && userId.equals(systemUserId);
        } catch (SecurityException e) {
            return true; // No authenticated user = system operation
        }
    }

    /**
     * Checks if current user is accessing their own resource
     *
     * @param resourceOwnerId ID of the resource owner
     * @return true if current user is the resource owner
     */
    public static boolean isOwnResource(UUID resourceOwnerId) {
        if (resourceOwnerId == null) {
            return false;
        }

        try {
            UUID currentUserId = getCurrentUserId();
            return currentUserId.equals(resourceOwnerId);
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Gets current user ID as string (for logging purposes)
     *
     * @return User ID as string or "ANONYMOUS" if not authenticated
     */
    public static String getCurrentUserIdAsString() {
        try {
            UUID userId = getCurrentUserId();
            return userId.toString();
        } catch (SecurityException e) {
            return "ANONYMOUS";
        }
    }

    /**
     * Checks if the current user has supervisor level access
     *
     * @return true if user is supervisor, admin, or higher
     */
    public static boolean hasSupervisorAccess() {
        return hasAnyRole("SUPERVISOR", "ADMIN", "SUPER_ADMIN");
    }

    /**
     * Checks if the current user has admin level access
     *
     * @return true if user is admin or higher
     */
    public static boolean hasAdminAccess() {
        return hasAnyRole("ADMIN", "SUPER_ADMIN");
    }
}