package com.protomil.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Email verification request")
public class EmailVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User email address", example = "john.doe@company.com", required = true)
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
    @Schema(description = "6-digit verification code", example = "123456", required = true)
    private String verificationCode;
}