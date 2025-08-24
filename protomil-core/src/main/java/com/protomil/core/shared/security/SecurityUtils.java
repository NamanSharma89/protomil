package com.protomil.core.shared.security;

import com.protomil.core.shared.exception.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the current authenticated user's ID
     *
     * @return Current user ID as Long
     * @throws SecurityException if no user is authenticated or user ID cannot be extracted
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        // Handle different types of principal objects
        if (principal instanceof UserTokenClaims) {
            UserTokenClaims claims = (UserTokenClaims) principal;
            return parseLongSafely(claims.getUserId());
        }

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();

            // Try to parse username as user ID if it's numeric
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.debug("Username '{}' is not numeric, attempting to extract user ID", username);
                // If username is email, you might need to look up user ID from database
                throw new SecurityException("Cannot extract user ID from username: " + username);
            }
        }

        if (principal instanceof String) {
            String principalString = (String) principal;
            try {
                return Long.parseLong(principalString);
            } catch (NumberFormatException e) {
                log.debug("Principal string '{}' is not a valid user ID", principalString);
                throw new SecurityException("Cannot parse user ID from principal: " + principalString);
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
    public static Optional<Long> getCurrentUserIdOptional() {
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
            return claims.getEmail();
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
            String firstName = claims.getFirstName() != null ? claims.getFirstName() : "";
            String lastName = claims.getLastName() != null ? claims.getLastName() : "";
            return (firstName + " " + lastName).trim();
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
    public static boolean canAccessResource(Long resourceOwnerId) {
        if (resourceOwnerId == null) {
            return false;
        }

        try {
            Long currentUserId = getCurrentUserId();

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
    public static void validateResourceAccess(Long resourceOwnerId) {
        if (!canAccessResource(resourceOwnerId)) {
            throw new SecurityException("Access denied to resource owned by user: " + resourceOwnerId);
        }
    }

    /**
     * Helper method to safely parse string to Long
     *
     * @param value String value to parse
     * @return Parsed Long value
     * @throws SecurityException if parsing fails
     */
    private static Long parseLongSafely(Object value) {
        if (value == null) {
            throw new SecurityException("User ID is null");
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new SecurityException("Invalid user ID format: " + value);
            }
        }

        throw new SecurityException("Cannot convert user ID to Long: " + value.getClass().getName());
    }

    /**
     * Gets user ID for auditing purposes - returns system user ID if no authenticated user
     *
     * @return User ID for audit logging
     */
    public static Long getUserIdForAudit() {
        try {
            return getCurrentUserId();
        } catch (SecurityException e) {
            // Return system user ID for system operations
            log.debug("No authenticated user for audit, using system user ID");
            return -1L; // System user ID
        }
    }

    /**
     * Checks if the current operation is being performed by the system
     *
     * @return true if system operation
     */
    public static boolean isSystemOperation() {
        try {
            Long userId = getCurrentUserId();
            return userId != null && userId.equals(-1L);
        } catch (SecurityException e) {
            return true; // No authenticated user = system operation
        }
    }
}