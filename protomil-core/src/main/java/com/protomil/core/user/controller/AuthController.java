package com.protomil.core.user.controller;

import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.user.dto.EmailVerificationRequest;
import com.protomil.core.user.dto.EmailVerificationResponse;
import com.protomil.core.user.dto.ResendVerificationRequest;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.service.EmailVerificationService;
import com.protomil.core.user.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "Authentication", description = "User authentication and verification APIs")
public class AuthController {

    private final UserRegistrationService userRegistrationService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            UserRegistrationService userRegistrationService,
            EmailVerificationService emailVerificationService) {
        this.userRegistrationService = userRegistrationService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user and initiate email verification if required")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        log.info("Processing user registration request for email: {}", request.getEmail());

        UserRegistrationResponse response = userRegistrationService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserRegistrationResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Verify user email using verification code")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {

        log.info("Processing email verification for: {}", request.getEmail());

        emailVerificationService.verifyEmail(request.getEmail(), request.getVerificationCode());

        EmailVerificationResponse response = EmailVerificationResponse.builder()
                .email(request.getEmail())
                .verified(true)
                .message("Email verified successfully")
                .nextStep("PENDING_APPROVAL")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.<EmailVerificationResponse>builder()
                .success(true)
                .message("Email verified successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code", description = "Resend email verification code")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(
            @Valid @RequestBody ResendVerificationRequest request) {

        log.info("Resending verification code for: {}", request.getEmail());

        emailVerificationService.resendVerificationCode(request.getEmail());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Verification code sent successfully")
                .data("Please check your email for the verification code")
                .timestamp(LocalDateTime.now())
                .build());
    }
}