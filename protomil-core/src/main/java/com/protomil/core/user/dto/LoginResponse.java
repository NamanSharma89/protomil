// src/main/java/com/protomil/core/user/dto/LoginResponse.java
package com.protomil.core.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User login response")
public class LoginResponse {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "User email", example = "john.doe@company.com")
    private String email;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User department", example = "Manufacturing")
    private String department;

    @Schema(description = "User roles", example = "[\"TECHNICIAN\", \"VIEWER\"]")
    private List<String> roles;

    @Schema(description = "JWT access token")
    private String accessToken;

    @Schema(description = "JWT refresh token")
    private String refreshToken;

    @Schema(description = "Token expiration time in seconds", example = "1800")
    private Integer expiresIn;

    @Schema(description = "Login timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "Remember me option", example = "true")
    private Boolean rememberMe;

    @Schema(description = "Redirect URL after login", example = "/wireframes/dashboard")
    private String redirectUrl;
}