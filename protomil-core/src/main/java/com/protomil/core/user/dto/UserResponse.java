// com/protomil/core/user/dto/UserResponse.java
package com.protomil.core.user.dto;

import com.protomil.core.shared.domain.enums.UserStatus;
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
@Schema(description = "User information response")
public class UserResponse {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Cognito User Sub", example = "cognito-user-sub-123")
    private String cognitoUserSub;

    @Schema(description = "User email", example = "john.doe@company.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Employee ID", example = "EMP001")
    private String employeeId;

    @Schema(description = "Department", example = "Manufacturing")
    private String department;

    @Schema(description = "Phone number", example = "+919876543210")
    private String phoneNumber;

    @Schema(description = "User status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Assigned roles")
    private List<String> roles;
}