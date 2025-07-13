// src/main/java/com/protomil/core/user/service/UserStatusSyncScheduler.java
package com.protomil.core.user.service;

import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.dto.UserStatusValidationResult;
import com.protomil.core.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "protomil.user-status.sync.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class UserStatusSyncScheduler {

    private final UserRepository userRepository;
    private final UserStatusSyncService userStatusSyncService;

    public UserStatusSyncScheduler(UserRepository userRepository,
                                   UserStatusSyncService userStatusSyncService) {
        this.userRepository = userRepository;
        this.userStatusSyncService = userStatusSyncService;
    }

    // Run every hour during business hours (9 AM to 6 PM)
    @Scheduled(cron = "0 0 9-18 * * MON-FRI")
    @LogExecutionTime
    public void validateActiveUsersStatus() {
        log.info("Starting scheduled validation of active users status");

        int pageSize = 50;
        int page = 0;
        int totalProcessed = 0;
        int inconsistenciesFound = 0;

        Page<User> userPage;
        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            userPage = userRepository.findByStatus(UserStatus.ACTIVE, pageable);

            for (User user : userPage.getContent()) {
                try {
                    UserStatusValidationResult result =
                            userStatusSyncService.validateUserStatusConsistency(user.getEmail());

                    if (result.hasIssues()) {
                        log.warn("Status inconsistency found for user: {} - {}",
                                user.getEmail(), result.getSummary());
                        inconsistenciesFound++;

                        // Attempt to fix inconsistency
                        userStatusSyncService.syncUserStatusFromCognito(user.getEmail());
                    }

                    totalProcessed++;

                } catch (Exception e) {
                    log.error("Error validating status for user: {} - {}", user.getEmail(), e.getMessage());
                }
            }

            page++;

        } while (userPage.hasNext());

        log.info("Completed scheduled status validation - Processed: {}, Inconsistencies found: {}",
                totalProcessed, inconsistenciesFound);
    }

    // Run daily at 2 AM to sync all user statuses
    @Scheduled(cron = "0 0 2 * * *")
    @LogExecutionTime
    public void dailyStatusSync() {
        log.info("Starting daily user status synchronization");

        int pageSize = 100;
        int page = 0;
        int totalSynced = 0;
        int errors = 0;

        Page<User> userPage;
        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            userPage = userRepository.findAll(pageable);

            for (User user : userPage.getContent()) {
                try {
                    userStatusSyncService.syncUserStatusToCognito(user);
                    totalSynced++;

                } catch (Exception e) {
                    log.error("Error syncing status for user: {} - {}", user.getEmail(), e.getMessage());
                    errors++;
                }
            }

            page++;

        } while (userPage.hasNext());

        log.info("Completed daily status sync - Synced: {}, Errors: {}", totalSynced, errors);
    }

    // Run every 30 minutes to check for users stuck in pending states
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @LogExecutionTime
    public void checkPendingUsers() {
        log.debug("Checking for users in pending states");

        List<UserStatus> pendingStatuses = List.of(
                UserStatus.PENDING_VERIFICATION,
                UserStatus.PENDING_APPROVAL
        );

        int pendingUsersFound = 0;

        for (UserStatus status : pendingStatuses) {
            List<User> pendingUsers = userRepository.findByStatus(status);
            pendingUsersFound += pendingUsers.size();

            if (!pendingUsers.isEmpty()) {
                log.info("Found {} users in {} status", pendingUsers.size(), status);

                // Check if any have been stuck for too long (e.g., more than 7 days)
                pendingUsers.stream()
                        .filter(user -> user.getCreatedAt().isBefore(
                                java.time.LocalDateTime.now().minusDays(7)))
                        .forEach(user -> {
                            log.warn("User has been in {} status for more than 7 days: {}",
                                    status, user.getEmail());
                        });
            }
        }

        if (pendingUsersFound > 0) {
            log.info("Total pending users found: {}", pendingUsersFound);
        }
    }
}