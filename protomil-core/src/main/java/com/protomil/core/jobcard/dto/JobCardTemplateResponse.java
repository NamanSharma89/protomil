package com.protomil.core.jobcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Schema(description = "Job card template response with full details")
public class JobCardTemplateResponse {

    @Schema(description = "Template ID", example = "1")
    private Long id;

    @Schema(description = "Template name", example = "Motor Assembly Template")
    private String templateName;

    @Schema(description = "Unique template code", example = "MOTOR_ASSEMBLY_V1")
    private String templateCode;

    @Schema(description = "Template description", example = "Standard template for motor assembly operations")
    private String description;

    @Schema(description = "Template category", example = "ASSEMBLY")
    private String category;

    @Schema(description = "Template version", example = "1")
    private Integer version;

    @Schema(description = "Is template active", example = "true")
    private Boolean isActive;

    @Schema(description = "Created by user ID", example = "123")
    private Long createdBy;

    @Schema(description = "Created by user name", example = "John Supervisor")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Field definitions for dynamic fields")
    private List<TemplateFieldDefinitionDto> fieldDefinitions;

    @Schema(description = "Template statistics")
    private TemplateStatistics statistics;

    @Schema(description = "Usage information")
    private TemplateUsageInfo usageInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Template usage statistics")
    public static class TemplateStatistics {

        @Schema(description = "Total job cards created from this template", example = "45")
        private Long totalJobCardsCreated;

        @Schema(description = "Active job cards using this template", example = "12")
        private Long activeJobCards;

        @Schema(description = "Completed job cards using this template", example = "33")
        private Long completedJobCards;

        @Schema(description = "Average completion time in minutes", example = "180")
        private Double averageCompletionTimeMinutes;

        @Schema(description = "Success rate percentage", example = "95.5")
        private Double successRatePercentage;

        @Schema(description = "Total field definitions", example = "8")
        private Integer totalFields;

        @Schema(description = "Required field definitions", example = "5")
        private Integer requiredFields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Template usage information")
    public static class TemplateUsageInfo {

        @Schema(description = "Can be used for new job cards", example = "true")
        private Boolean canCreateJobCards;

        @Schema(description = "Reason if cannot be used")
        private String unavailabilityReason;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Last used timestamp")
        private LocalDateTime lastUsedAt;

        @Schema(description = "Most frequent user ID", example = "456")
        private Long mostFrequentUserId;

        @Schema(description = "Most frequent user name", example = "Jane Supervisor")
        private String mostFrequentUserName;

        @Schema(description = "Usage frequency in last 30 days", example = "15")
        private Integer usageCountLast30Days;
    }

    // Helper methods
    public boolean canBeUsedForJobCard() {
        return Boolean.TRUE.equals(isActive) &&
                (usageInfo == null || Boolean.TRUE.equals(usageInfo.getCanCreateJobCards()));
    }

    public int getTotalFieldsCount() {
        return fieldDefinitions != null ? fieldDefinitions.size() : 0;
    }

    public long getRequiredFieldsCount() {
        return fieldDefinitions != null ?
                fieldDefinitions.stream()
                        .filter(TemplateFieldDefinitionDto::getIsRequired)
                        .count() : 0;
    }

    public boolean hasFieldDefinitions() {
        return fieldDefinitions != null && !fieldDefinitions.isEmpty();
    }
}