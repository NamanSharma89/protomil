package com.protomil.core.jobcard.dto;

import com.protomil.core.jobcard.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to create a new job card")
public class JobCardCreateRequest {

    @NotNull(message = "Template ID is required")
    @Schema(description = "Template ID to base the job card on", example = "1")
    private Long templateId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Schema(description = "Job card title", example = "Motor Assembly - Batch 001")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Job card description", example = "Complete assembly of motor unit X200")
    private String description;

    @Schema(description = "Job priority level", example = "HIGH")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Positive(message = "Estimated duration must be positive")
    @Schema(description = "Estimated duration in minutes", example = "120")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Target completion date and time")
    private LocalDateTime targetCompletionDate;

    @Schema(description = "Dynamic field values based on template")
    private Map<String, Object> dynamicFields;

    @Schema(description = "Additional notes or instructions")
    private String notes;
}