package com.protomil.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Email verification response")
public class EmailVerificationResponse {

    @Schema(description = "User email address", example = "john.doe@company.com")
    private String email;

    @Schema(description = "Whether email was verified successfully", example = "true")
    private Boolean verified;

    @Schema(description = "Response message", example = "Email verified successfully")
    private String message;

    @Schema(description = "Next step in the process", example = "PENDING_APPROVAL")
    private String nextStep;

    @Schema(description = "Verification timestamp")
    private LocalDateTime timestamp;
}