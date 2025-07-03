package com.protomil.core.user.dto;

import com.protomil.core.shared.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// com/protomil/core/user/dto/UserRegistrationResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User registration response")
public class UserRegistrationResponse {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "User email", example = "john.doe@company.com")
    private String email;

    @Schema(description = "Registration status", example = "PENDING_VERIFICATION")
    private UserStatus status;

    @Schema(description = "Registration timestamp")
    private LocalDateTime registeredAt;

    @Schema(description = "Email verification required", example = "true")
    private Boolean emailVerificationRequired;

    @Schema(description = "Admin approval required", example = "true")
    private Boolean adminApprovalRequired;
}
