package com.protomil.core.user.controller;

import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.user.dto.UserApprovalRequest;
import com.protomil.core.user.dto.UserResponse;
import com.protomil.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@Tag(name = "User Management", description = "User management APIs (Admin)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Remove registration endpoint - moved to AuthController

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve user", description = "Approve a pending user and assign roles")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserApprovalRequest request,
            Authentication authentication) {

        UserResponse response = userService.approveUser(userId, request, authentication);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User approved successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve user information by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // Add other user management endpoints as needed
}