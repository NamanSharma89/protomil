// src/main/java/com/protomil/core/user/events/UserStatusEventListener.java
package com.protomil.core.user.events;

import com.protomil.core.user.service.UserStatusSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserStatusEventListener {

    private final UserStatusSyncService userStatusSyncService;

    public UserStatusEventListener(UserStatusSyncService userStatusSyncService) {
        this.userStatusSyncService = userStatusSyncService;
    }

    @EventListener
    @Async
    public void handleUserStatusChanged(UserStatusChangedEvent event) {
        log.info("Processing user status change event for user: {} from {} to {}",
                event.getUser().getEmail(), event.getPreviousStatus(), event.getNewStatus());

        try {
            // Sync status change to Cognito
            userStatusSyncService.syncUserStatusToCognito(event.getUser());

            log.info("User status change synced to Cognito for user: {}", event.getUser().getEmail());

        } catch (Exception e) {
            log.error("Failed to sync status change to Cognito for user: {} - {}",
                    event.getUser().getEmail(), e.getMessage());
        }
    }

    @EventListener
    @Async
    public void handleUserApproved(UserApprovedEvent event) {
        log.info("Processing user approved event for user: {}", event.getUser().getEmail());

        try {
            // Ensure status is synced after approval
            userStatusSyncService.syncUserStatusToCognito(event.getUser());

            log.info("User approval synced to Cognito for user: {}", event.getUser().getEmail());

        } catch (Exception e) {
            log.error("Failed to sync user approval to Cognito for user: {} - {}",
                    event.getUser().getEmail(), e.getMessage());
        }
    }
}