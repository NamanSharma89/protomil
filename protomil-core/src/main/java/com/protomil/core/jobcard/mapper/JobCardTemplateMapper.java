package com.protomil.core.jobcard.mapper;

import com.protomil.core.jobcard.domain.FieldOption;
import com.protomil.core.jobcard.domain.JobCardTemplate;
import com.protomil.core.jobcard.domain.TemplateFieldDefinition;
import com.protomil.core.jobcard.dto.JobCardTemplateResponse;
import com.protomil.core.jobcard.dto.TemplateFieldDefinitionDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JobCardTemplateMapper {

    public JobCardTemplateResponse toResponse(JobCardTemplate template) {
        if (template == null) return null;

        return JobCardTemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .templateCode(template.getTemplateCode())
                .description(template.getDescription())
                .category(template.getCategory())
                .version(template.getVersion())
                .isActive(template.getIsActive())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .fieldDefinitions(mapFieldDefinitions(template.getFieldDefinitions()))
                .build();
    }

    public TemplateFieldDefinitionDto toFieldDefinitionDto(TemplateFieldDefinition field) {
        if (field == null) return null;

        return TemplateFieldDefinitionDto.builder()
                .id(field.getId())
                .templateId(field.getTemplate() != null ? field.getTemplate().getId() : null)
                .fieldName(field.getFieldName())
                .fieldLabel(field.getFieldLabel())
                .fieldType(field.getFieldType())
                .fieldGroup(field.getFieldGroup())
                .displayOrder(field.getDisplayOrder())
                .isRequired(field.getIsRequired())
                .fieldConfig(field.getFieldConfig())
                .helpText(field.getHelpText())
                .createdAt(field.getCreatedAt())
                .fieldOptions(mapFieldOptions(field.getFieldOptions()))
                .validation(mapValidation(field.getFieldConfig()))
                .build();
    }

    private List<TemplateFieldDefinitionDto> mapFieldDefinitions(List<TemplateFieldDefinition> fieldDefinitions) {
        if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return List.of();
        }

        return fieldDefinitions.stream()
                .map(this::toFieldDefinitionDto)
                .collect(Collectors.toList());
    }

    private List<TemplateFieldDefinitionDto.FieldOptionDto> mapFieldOptions(List<FieldOption> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }

        return options.stream()
                .map(this::mapFieldOption)
                .collect(Collectors.toList());
    }

    private TemplateFieldDefinitionDto.FieldOptionDto mapFieldOption(FieldOption option) {
        return TemplateFieldDefinitionDto.FieldOptionDto.builder()
                .id(option.getId())
                .optionValue(option.getOptionValue())
                .optionLabel(option.getOptionLabel())
                .displayOrder(option.getDisplayOrder())
                .isActive(option.getIsActive())
                .build();
    }

    private TemplateFieldDefinitionDto.FieldValidationDto mapValidation(Map<String, Object> fieldConfig) {
        if (fieldConfig == null || fieldConfig.isEmpty()) {
            return null;
        }

        return TemplateFieldDefinitionDto.FieldValidationDto.builder()
                .min(fieldConfig.get("min"))
                .max(fieldConfig.get("max"))
                .pattern((String) fieldConfig.get("pattern"))
                .message((String) fieldConfig.get("message"))
                .required((Boolean) fieldConfig.get("required"))
                .defaultValue(fieldConfig.get("defaultValue"))
                .placeholder((String) fieldConfig.get("placeholder"))
                .build();
    }
}