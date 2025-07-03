// com/protomil/core/user/controller/UserController.java
package com.protomil.core.user.controller;

import com.protomil.core.shared.dto.ErrorResponse;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.shared.logging.LogSensitiveData;
import com.protomil.core.user.dto.UserRegistrationRequest;
import com.protomil.core.user.dto.UserRegistrationResponse;
import com.protomil.core.user.service.UserRegistrationService;
import com.protomil.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
@Tag(name = "User Management", description = "User registration and management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRegistrationService userRegistrationService;

    public UserController(UserService userService, UserRegistrationService userRegistrationService) {
        this.userService = userService;
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Register a new user in the system. User will be in pending verification status until email is verified and admin approval is granted.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully",
                            content = @Content(schema = @Schema(implementation = UserRegistrationResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "User already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @LogExecutionTime
    @LogSensitiveData(exclude = {"password"})
    public ResponseEntity<com.protomil.core.shared.dto.ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest) {

        MDC.put("operation", "user_registration");
        MDC.put("email", request.getEmail());
        MDC.put("clientIP", getClientIP(httpRequest));

        try {
            log.info("Processing user registration request for email: {}", request.getEmail());

            UserRegistrationResponse response = userRegistrationService.registerUser(request);

            log.info("User registration successful for email: {}, userId: {}",
                    request.getEmail(), response.getUserId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.protomil.core.shared.dto.ApiResponse.<UserRegistrationResponse>builder()
                            .success(true)
                            .message("User registered successfully")
                            .data(response)
                            .timestamp(LocalDateTime.now())
                            .build());

        } catch (Exception e) {
            log.error("User registration failed for email: {}", request.getEmail(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}