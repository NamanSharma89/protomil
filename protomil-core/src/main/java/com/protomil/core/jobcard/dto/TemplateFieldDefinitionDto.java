package com.protomil.core.jobcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.protomil.core.jobcard.domain.enums.FieldType;
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
@Schema(description = "Template field definition with configuration")
public class TemplateFieldDefinitionDto {

    @Schema(description = "Field definition ID", example = "1")
    private Long id;

    @Schema(description = "Template ID this field belongs to", example = "1")
    private Long templateId;

    @Schema(description = "Field name (used as key)", example = "motor_model")
    private String fieldName;

    @Schema(description = "Display label for the field", example = "Motor Model")
    private String fieldLabel;

    @Schema(description = "Field type", example = "DROPDOWN")
    private FieldType fieldType;

    @Schema(description = "Field group for organization", example = "Product Details")
    private String fieldGroup;

    @Schema(description = "Display order within template", example = "1")
    private Integer displayOrder;

    @Schema(description = "Is field required", example = "true")
    private Boolean isRequired;

    @Schema(description = "Field configuration (validation, options, etc.)")
    private Map<String, Object> fieldConfig;

    @Schema(description = "Help text for the field", example = "Select the motor model for this job")
    private String helpText;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Field options for dropdown/checkbox fields")
    private List<FieldOptionDto> fieldOptions;

    @Schema(description = "Validation rules")
    private FieldValidationDto validation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field option for dropdown/checkbox fields")
    public static class FieldOptionDto {

        @Schema(description = "Option ID", example = "1")
        private Long id;

        @Schema(description = "Option value (stored value)", example = "X200")
        private String optionValue;

        @Schema(description = "Option label (displayed value)", example = "Model X200")
        private String optionLabel;

        @Schema(description = "Display order", example = "1")
        private Integer displayOrder;

        @Schema(description = "Is option active", example = "true")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field validation configuration")
    public static class FieldValidationDto {

        @Schema(description = "Minimum value/length", example = "1")
        private Object min;

        @Schema(description = "Maximum value/length", example = "1000")
        private Object max;

        @Schema(description = "Regular expression pattern")
        private String pattern;

        @Schema(description = "Custom validation message")
        private String message;

        @Schema(description = "Is field required", example = "true")
        private Boolean required;

        @Schema(description = "Default value")
        private Object defaultValue;

        @Schema(description = "Placeholder text", example = "Enter quantity...")
        private String placeholder;
    }

    // Helper methods
    public boolean requiresOptions() {
        return fieldType != null && fieldType.requiresOptions();
    }

    public boolean hasOptions() {
        return fieldOptions != null && !fieldOptions.isEmpty();
    }

    public boolean isNumericField() {
        return fieldType != null && fieldType.isNumericType();
    }

    public boolean isDateField() {
        return fieldType != null && fieldType.isDateType();
    }

    public boolean isTextualField() {
        return fieldType != null && fieldType.isTextType();
    }

    public String getHtmlInputType() {
        return fieldType != null ? fieldType.getHtmlType() : "text";
    }

    public Object getValidationRule(String ruleName) {
        if (validation == null || fieldConfig == null) {
            return null;
        }
        return fieldConfig.get(ruleName);
    }

    public boolean hasValidationPattern() {
        return validation != null && validation.getPattern() != null && !validation.getPattern().trim().isEmpty();
    }

    public String getEffectiveHelpText() {
        if (helpText != null && !helpText.trim().isEmpty()) {
            return helpText;
        }
        if (fieldType != null) {
            return "Enter " + fieldType.getDisplayName().toLowerCase();
        }
        return "Enter value for " + fieldLabel;
    }
}