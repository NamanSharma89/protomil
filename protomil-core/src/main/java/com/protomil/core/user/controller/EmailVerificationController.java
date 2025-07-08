package com.protomil.core.user.controller;

import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.user.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "Email Verification", description = "Email verification APIs")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address",
            description = "Verify user email address using the verification code sent to their email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        log.info("Email verification request for: {}", request.getEmail());

        emailVerificationService.verifyEmail(request.getEmail(), request.getVerificationCode());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Email verified successfully")
                .data("Email verification completed")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code",
            description = "Resend email verification code to the user's email address")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@Valid @RequestBody ResendVerificationRequest request) {
        log.info("Resend verification code request for: {}", request.getEmail());

        emailVerificationService.resendVerificationCode(request.getEmail());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Verification code sent successfully")
                .data("Please check your email for the verification code")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Data
    public static class EmailVerificationRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Verification code is required")
        private String verificationCode;
    }

    @Data
    public static class ResendVerificationRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
    }
}