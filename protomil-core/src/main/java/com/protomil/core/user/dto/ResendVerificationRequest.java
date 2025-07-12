package com.protomil.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resend verification code request")
public class ResendVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User email address", example = "john.doe@company.com", required = true)
    private String email;
}