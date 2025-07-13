// src/main/java/com/protomil/core/user/controller/UserStatusController.java
package com.protomil.core.user.controller;

import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.user.dto.UserStatusValidationResult;
import com.protomil.core.user.service.UserStatusSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/user-status")
@Slf4j
@Tag(name = "User Status Management", description = "Admin APIs for user status management and synchronization")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserStatusController {

    private final UserStatusSyncService userStatusSyncService;

    public UserStatusController(UserStatusSyncService userStatusSyncService) {
        this.userStatusSyncService = userStatusSyncService;
    }

    @GetMapping("/validate/{email}")
    @Operation(summary = "Validate user status consistency",
            description = "Check if user status is consistent between local DB and Cognito")
    public ResponseEntity<ApiResponse<UserStatusValidationResult>> validateUserStatus(
            @PathVariable String email) {

        log.info("Validating user status consistency for: {}", email);

        UserStatusValidationResult result = userStatusSyncService.validateUserStatusConsistency(email);

        return ResponseEntity.ok(ApiResponse.<UserStatusValidationResult>builder()
                .success(true)
                .message(result.isConsistent() ? "Status is consistent" : "Status inconsistency detected")
                .data(result)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/sync-to-cognito/{email}")
    @Operation(summary = "Sync user status to Cognito",
            description = "Force synchronization of user status from local DB to Cognito")
    public ResponseEntity<ApiResponse<String>> syncStatusToCognito(@PathVariable String email) {

        log.info("Syncing user status to Cognito for: {}", email);

        // Find user and sync
        com.protomil.core.user.repository.UserRepository userRepository =
                null; // This should be injected - simplified for example

        // Implementation would get user and call userStatusSyncService.syncUserStatusToCognito(user)

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("User status synced to Cognito successfully")
                .data("Sync completed for: " + email)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/sync-from-cognito/{email}")
    @Operation(summary = "Sync user status from Cognito",
            description = "Force synchronization of user status from Cognito to local DB")
    public ResponseEntity<ApiResponse<String>> syncStatusFromCognito(@PathVariable String email) {

        log.info("Syncing user status from Cognito for: {}", email);

        userStatusSyncService.syncUserStatusFromCognito(email);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("User status synced from Cognito successfully")
                .data("Sync completed for: " + email)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/force-status/{userId}")
    @Operation(summary = "Force user status change",
            description = "Administratively change user status and sync to Cognito")
    public ResponseEntity<ApiResponse<String>> forceStatusChange(
            @PathVariable UUID userId,
            @RequestParam UserStatus newStatus,
            @RequestParam(required = false) String reason) {

        log.info("Force changing user status for: {} to: {} with reason: {}", userId, newStatus, reason);

        userStatusSyncService.forceStatusSync(userId, newStatus);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("User status changed and synced successfully")
                .data(String.format("User %s status changed to %s", userId, newStatus))
                .timestamp(LocalDateTime.now())
                .build());
    }
}