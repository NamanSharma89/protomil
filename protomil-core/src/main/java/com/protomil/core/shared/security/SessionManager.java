// src/main/java/com/protomil/core/shared/security/SessionManager.java
package com.protomil.core.shared.security;

import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.service.RoleService;
import com.protomil.core.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SessionManager {

    private final Map<UUID, UserSessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final RoleService roleService;
    private final UserService userService;

    public SessionManager(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @LogExecutionTime
    public void createSession(UUID userId, UserTokenClaims userClaims) {
        log.debug("Creating session for user: {}", userId);

        UserSessionInfo sessionInfo = UserSessionInfo.builder()
                .userId(userId)
                .userClaims(userClaims)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .active(true)
                .build();

        activeSessions.put(userId, sessionInfo);

        log.debug("Session created successfully for user: {}", userId);
    }

    @LogExecutionTime
    public UserTokenClaims getCurrentUserClaims(UUID userId) {
        log.debug("Getting current user claims for user: {}", userId);

        UserSessionInfo sessionInfo = activeSessions.get(userId);

        if (sessionInfo != null && sessionInfo.isActive()) {
            // Update last accessed time
            sessionInfo.setLastAccessedAt(LocalDateTime.now());

            // Refresh user roles in case they changed
            List<String> currentRoles = roleService.getUserRoleNames(userId);

            // Update roles in cached claims if they changed
            if (!currentRoles.equals(sessionInfo.getUserClaims().getRoles())) {
                log.debug("User roles changed for user: {}, updating session", userId);
                sessionInfo.getUserClaims().setRoles(currentRoles);
            }

            return sessionInfo.getUserClaims();
        }

        log.debug("No active session found for user: {}, fetching fresh data", userId);

        // No active session, create fresh claims from database
        try {
            com.protomil.core.user.dto.UserResponse userResponse = userService.getUserById(userId);
            List<String> userRoles = roleService.getUserRoleNames(userId);

            UserTokenClaims freshClaims = UserTokenClaims.builder()
                    .cognitoSub(userResponse.getCognitoUserSub())
                    .userId(userResponse.getId())
                    .email(userResponse.getEmail())
                    .firstName(userResponse.getFirstName())
                    .lastName(userResponse.getLastName())
                    .department(userResponse.getDepartment())
                    .roles(userRoles)
                    .tokenType("access")
                    .build();

            // Cache the fresh claims
            createSession(userId, freshClaims);

            return freshClaims;

        } catch (Exception e) {
            log.error("Failed to fetch user claims for user: {}", userId, e);
            throw new com.protomil.core.shared.exception.AuthenticationException("Unable to load user session");
        }
    }

    @LogExecutionTime
    public void invalidateSession(UUID userId) {
        log.debug("Invalidating session for user: {}", userId);

        UserSessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null) {
            sessionInfo.setActive(false);
            sessionInfo.setInvalidatedAt(LocalDateTime.now());
        }

        log.debug("Session invalidated for user: {}", userId);
    }

    @LogExecutionTime
    public void removeSession(UUID userId) {
        log.debug("Removing session for user: {}", userId);

        activeSessions.remove(userId);

        log.debug("Session removed for user: {}", userId);
    }

    @LogExecutionTime
    public boolean hasActiveSession(UUID userId) {
        UserSessionInfo sessionInfo = activeSessions.get(userId);
        return sessionInfo != null && sessionInfo.isActive();
    }

    @LogExecutionTime
    public void refreshUserRoles(UUID userId) {
        log.debug("Refreshing user roles for user: {}", userId);

        UserSessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null && sessionInfo.isActive()) {
            List<String> currentRoles = roleService.getUserRoleNames(userId);
            sessionInfo.getUserClaims().setRoles(currentRoles);
            sessionInfo.setLastAccessedAt(LocalDateTime.now());

            log.debug("User roles refreshed for user: {} - New roles: {}", userId, currentRoles);
        }
    }

    @LogExecutionTime
    public int getActiveSessionCount() {
        long activeCount = activeSessions.values().stream()
                .filter(UserSessionInfo::isActive)
                .count();

        return (int) activeCount;
    }

    // Cleanup expired sessions every 30 minutes
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @LogExecutionTime
    public void cleanupExpiredSessions() {
        log.debug("Starting cleanup of expired sessions");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(3); // 3 hours old

        List<UUID> expiredSessions = activeSessions.entrySet().stream()
                .filter(entry -> {
                    UserSessionInfo session = entry.getValue();
                    return !session.isActive() ||
                            session.getLastAccessedAt().isBefore(cutoffTime);
                })
                .map(Map.Entry::getKey)
                .toList();

        expiredSessions.forEach(activeSessions::remove);

        log.info("Cleaned up {} expired sessions. Active sessions remaining: {}",
                expiredSessions.size(), getActiveSessionCount());
    }

    // Session information holder
    private static class UserSessionInfo {
        private final UUID userId;
        private final UserTokenClaims userClaims;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private boolean active;
        private LocalDateTime invalidatedAt;

        private UserSessionInfo(UUID userId, UserTokenClaims userClaims, LocalDateTime createdAt,
                                LocalDateTime lastAccessedAt, boolean active) {
            this.userId = userId;
            this.userClaims = userClaims;
            this.createdAt = createdAt;
            this.lastAccessedAt = lastAccessedAt;
            this.active = active;
        }

        public static UserSessionInfoBuilder builder() {
            return new UserSessionInfoBuilder();
        }

        // Getters and setters
        public UUID getUserId() { return userId; }
        public UserTokenClaims getUserClaims() { return userClaims; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
        public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getInvalidatedAt() { return invalidatedAt; }
        public void setInvalidatedAt(LocalDateTime invalidatedAt) { this.invalidatedAt = invalidatedAt; }

        public static class UserSessionInfoBuilder {
            private UUID userId;
            private UserTokenClaims userClaims;
            private LocalDateTime createdAt;
            private LocalDateTime lastAccessedAt;
            private boolean active;

            public UserSessionInfoBuilder userId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public UserSessionInfoBuilder userClaims(UserTokenClaims userClaims) {
                this.userClaims = userClaims;
                return this;
            }

            public UserSessionInfoBuilder createdAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
                return this;
            }

            public UserSessionInfoBuilder lastAccessedAt(LocalDateTime lastAccessedAt) {
                this.lastAccessedAt = lastAccessedAt;
                return this;
            }

            public UserSessionInfoBuilder active(boolean active) {
                this.active = active;
                return this;
            }

            public UserSessionInfo build() {
                return new UserSessionInfo(userId, userClaims, createdAt, lastAccessedAt, active);
            }
        }
    }
}