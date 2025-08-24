package com.protomil.core.jobcard.dto;

import com.protomil.core.jobcard.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update an existing job card")
public class JobCardUpdateRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Schema(description = "Job card title", example = "Motor Assembly - Batch 001 (Updated)")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Job card description")
    private String description;

    @Schema(description = "Job priority level", example = "HIGH")
    private Priority priority;

    @Positive(message = "Estimated duration must be positive")
    @Schema(description = "Estimated duration in minutes", example = "150")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Target completion date and time")
    private LocalDateTime targetCompletionDate;

    @Schema(description = "Dynamic field values based on template")
    private Map<String, Object> dynamicFields;

    @Schema(description = "Additional notes or instructions")
    private String notes;
}