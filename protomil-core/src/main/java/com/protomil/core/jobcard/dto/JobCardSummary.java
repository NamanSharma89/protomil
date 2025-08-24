package com.protomil.core.jobcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.domain.enums.Priority;
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
@Schema(description = "Job card summary for list views")
public class JobCardSummary {

    @Schema(description = "Job card ID", example = "1")
    private Long id;

    @Schema(description = "Unique job number", example = "JC-2025-001")
    private String jobNumber;

    @Schema(description = "Job card title", example = "Motor Assembly - Batch 001")
    private String title;

    @Schema(description = "Current status", example = "IN_PROGRESS")
    private JobStatus status;

    @Schema(description = "Priority level", example = "HIGH")
    private Priority priority;

    @Schema(description = "Template name")
    private String templateName;

    @Schema(description = "Template category")
    private String templateCategory;

    @Schema(description = "Assigned user ID")
    private Long assignedTo;

    @Schema(description = "Assigned user name")
    private String assignedToName;

    @Schema(description = "Created by user ID")
    private Long createdBy;

    @Schema(description = "Created by user name")
    private String createdByName;

    @Schema(description = "Estimated duration in minutes")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Actual duration in minutes")
    private Integer actualDurationMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Target completion date")
    private LocalDateTime targetCompletionDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Started timestamp")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Completed timestamp")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Progress percentage")
    private Double progressPercentage;

    @Schema(description = "Is overdue")
    private Boolean isOverdue;

    @Schema(description = "Machine code if assigned")
    private String machineCode;

    @Schema(description = "Total work instructions")
    private Integer totalInstructions;

    @Schema(description = "Completed work instructions")
    private Integer completedInstructions;
}