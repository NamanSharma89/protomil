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
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Job card response with full details")
public class JobCardResponse {

    @Schema(description = "Job card ID", example = "1")
    private Long id;

    @Schema(description = "Unique job number", example = "JC-2025-001")
    private String jobNumber;

    @Schema(description = "Job card title", example = "Motor Assembly - Batch 001")
    private String title;

    @Schema(description = "Job card description")
    private String description;

    @Schema(description = "Current status", example = "IN_PROGRESS")
    private JobStatus status;

    @Schema(description = "Priority level", example = "HIGH")
    private Priority priority;

    @Schema(description = "Template information")
    private TemplateInfo template;

    @Schema(description = "Assignment information")
    private AssignmentInfo assignment;

    @Schema(description = "Timing information")
    private TimingInfo timing;

    @Schema(description = "Progress information")
    private ProgressInfo progress;

    @Schema(description = "Dynamic field values")
    private Map<String, Object> dynamicFields;

    @Schema(description = "Work instructions")
    private List<WorkInstructionDto> workInstructions;

    @Schema(description = "Attachments")
    private List<AttachmentInfo> attachments;

    @Schema(description = "Comments")
    private List<CommentInfo> comments;

    @Schema(description = "Production records")
    private List<ProductionRecordInfo> productionRecords;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        private Long id;
        private String templateName;
        private String templateCode;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentInfo {
        private Long assignedTo;
        private String assignedToName;
        private Long assignedBy;
        private String assignedByName;
        private LocalDateTime assignedAt;
        private MachineInfo machine;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MachineInfo {
        private Long id;
        private String machineCode;
        private String machineName;
        private String sectionCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimingInfo {
        private Integer estimatedDurationMinutes;
        private Integer actualDurationMinutes;
        private LocalDateTime targetCompletionDate;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        private Integer totalSteps;
        private Integer completedSteps;
        private Double completionPercentage;
        private String currentStep;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String description;
        private LocalDateTime uploadedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentInfo {
        private Long id;
        private String commentText;
        private String commentType;
        private String createdByName;
        private LocalDateTime createdAt;
        private Boolean isInternal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionRecordInfo {
        private Long id;
        private String machineCode;
        private Integer productionQuantity;
        private Boolean isValidated;
        private LocalDateTime entryDate;
    }
}