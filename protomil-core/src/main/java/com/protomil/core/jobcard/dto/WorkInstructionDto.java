package com.protomil.core.jobcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.protomil.core.jobcard.domain.enums.InstructionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Work instruction details")
public class WorkInstructionDto {

    @Schema(description = "Work instruction ID")
    private Long id;

    @Schema(description = "Step number")
    private Integer stepNumber;

    @Schema(description = "Instruction title")
    private String title;

    @Schema(description = "Instruction description")
    private String description;

    @Schema(description = "Instruction type")
    private InstructionType instructionType;

    @Schema(description = "Instruction content")
    private String content;

    @Schema(description = "Attachment file paths")
    private List<String> attachments;

    @Schema(description = "Is quality checkpoint")
    private Boolean isQualityCheckpoint;

    @Schema(description = "Estimated duration in minutes")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Actual duration in minutes")
    private Integer actualDurationMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;

    @Schema(description = "Completed by user ID")
    private Long completedBy;

    @Schema(description = "Completed by user name")
    private String completedByName;

    @Schema(description = "Completion notes")
    private String notes;

    @Schema(description = "Is completed")
    private Boolean isCompleted;
}