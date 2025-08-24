package com.protomil.core.jobcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to assign job card to personnel")
public class JobAssignmentRequest {

    @NotNull(message = "Assigned user ID is required")
    @Schema(description = "User ID to assign the job card to", example = "123")
    private Long assignedTo;

    @Schema(description = "Machine ID to assign (optional)", example = "456")
    private Long machineId;

    @Size(max = 500, message = "Assignment reason must not exceed 500 characters")
    @Schema(description = "Reason for assignment", example = "User has required skills and is available")
    private String assignmentReason;

    @Schema(description = "Force assignment even if user is at capacity", example = "false")
    @Builder.Default
    private Boolean forceAssignment = false;
}