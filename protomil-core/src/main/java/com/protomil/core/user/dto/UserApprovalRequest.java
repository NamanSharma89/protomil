// com/protomil/core/user/dto/UserApprovalRequest.java
package com.protomil.core.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User approval request with role assignments")
public class UserApprovalRequest {

    @NotEmpty(message = "At least one role must be assigned")
    @Schema(description = "List of role IDs to assign to the user", required = true)
    private List<UUID> roleIds;

    @Schema(description = "Additional notes for the approval", example = "Initial role assignment for new employee")
    private String notes;
}