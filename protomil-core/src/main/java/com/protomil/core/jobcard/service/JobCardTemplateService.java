package com.protomil.core.jobcard.service;

import com.protomil.core.jobcard.domain.JobCardTemplate;
import com.protomil.core.jobcard.domain.TemplateFieldDefinition;
import com.protomil.core.jobcard.dto.JobCardTemplateResponse;
import com.protomil.core.jobcard.dto.TemplateFieldDefinitionDto;
import com.protomil.core.jobcard.exception.TemplateNotFoundException;
import com.protomil.core.jobcard.mapper.JobCardTemplateMapper;
import com.protomil.core.jobcard.repository.JobCardRepository;
import com.protomil.core.jobcard.repository.JobCardTemplateRepository;
import com.protomil.core.jobcard.repository.TemplateFieldDefinitionRepository;
import com.protomil.core.shared.logging.LogExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class JobCardTemplateService {

    private final JobCardTemplateRepository templateRepository;
    private final TemplateFieldDefinitionRepository fieldDefinitionRepository;
    private final JobCardRepository jobCardRepository;
    private final JobCardTemplateMapper templateMapper;

    @LogExecutionTime
    public Page<JobCardTemplateResponse> getTemplates(String category, Pageable pageable) {
        log.debug("Retrieving templates with category filter: {}, page: {}", category, pageable.getPageNumber());

        Page<JobCardTemplate> templates;

        if (StringUtils.hasText(category)) {
            templates = templateRepository.findByCategoryAndIsActiveTrue(category)
                    .stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(
                                    list, pageable, list.size()
                            )
                    ));
        } else {
            templates = templateRepository.findByIsActiveTrue(pageable);
        }

        return templates.map(this::mapToResponseWithStatistics);
    }

    @LogExecutionTime
    public JobCardTemplateResponse getTemplateById(Long templateId) {
        log.debug("Retrieving template with ID: {}", templateId);

        JobCardTemplate template = templateRepository.findByIdWithFieldDefinitions(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with ID: " + templateId));

        if (!template.getIsActive()) {
            log.warn("Requested template {} is inactive", templateId);
        }

        return mapToResponseWithStatistics(template);
    }

    @LogExecutionTime
    public JobCardTemplateResponse getTemplateByCode(String templateCode) {
        log.debug("Retrieving template with code: {}", templateCode);

        JobCardTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with code: " + templateCode));

        return mapToResponseWithStatistics(template);
    }

    @LogExecutionTime
    public List<TemplateFieldDefinitionDto> getTemplateFields(Long templateId) {
        log.debug("Retrieving field definitions for template: {}", templateId);

        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new TemplateNotFoundException("Template not found with ID: " + templateId);
        }

        List<TemplateFieldDefinition> fieldDefinitions =
                fieldDefinitionRepository.findByTemplateIdWithOptions(templateId);

        return fieldDefinitions.stream()
                .map(templateMapper::toFieldDefinitionDto)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public List<TemplateFieldDefinitionDto> getRequiredFields(Long templateId) {
        log.debug("Retrieving required field definitions for template: {}", templateId);

        List<TemplateFieldDefinition> requiredFields =
                fieldDefinitionRepository.findByTemplateIdAndIsRequiredTrueOrderByDisplayOrder(templateId);

        return requiredFields.stream()
                .map(templateMapper::toFieldDefinitionDto)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public List<String> getAllCategories() {
        log.debug("Retrieving all template categories");

        return templateRepository.findDistinctActiveCategories();
    }

    @LogExecutionTime
    public List<JobCardTemplateResponse> getTemplatesByCategory(String category) {
        log.debug("Retrieving templates for category: {}", category);

        List<JobCardTemplate> templates = templateRepository.findByCategoryAndIsActiveTrue(category);

        return templates.stream()
                .map(this::mapToResponseWithStatistics)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public Page<JobCardTemplateResponse> searchTemplates(String searchTerm, Pageable pageable) {
        log.debug("Searching templates with term: {}", searchTerm);

        Page<JobCardTemplate> templates = templateRepository.searchTemplates(searchTerm, pageable);

        return templates.map(this::mapToResponseWithStatistics);
    }

    @LogExecutionTime
    public JobCardTemplateResponse getLatestVersionByName(String templateName) {
        log.debug("Retrieving latest version of template: {}", templateName);

        JobCardTemplate template = templateRepository.findLatestActiveVersionByTemplateName(templateName)
                .orElseThrow(() -> new TemplateNotFoundException("No active template found with name: " + templateName));

        return mapToResponseWithStatistics(template);
    }

    @LogExecutionTime
    public List<JobCardTemplateResponse> getTemplateVersions(String templateName) {
        log.debug("Retrieving all versions of template: {}", templateName);

        List<JobCardTemplate> templates = templateRepository.findByTemplateNameOrderByVersionDesc(templateName);

        return templates.stream()
                .map(templateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public boolean isTemplateInUse(Long templateId) {
        log.debug("Checking if template {} is in use", templateId);

        List<com.protomil.core.jobcard.domain.JobCard> activeJobCards =
                jobCardRepository.findByTemplateIdAndStatus(templateId,
                        com.protomil.core.jobcard.domain.enums.JobStatus.IN_PROGRESS);

        boolean inUse = !activeJobCards.isEmpty();
        log.debug("Template {} is {} in use", templateId, inUse ? "" : "not");

        return inUse;
    }

    @LogExecutionTime
    public Long getTemplateUsageCount(Long templateId) {
        log.debug("Getting usage count for template: {}", templateId);

        return (long) jobCardRepository.findByTemplateId(templateId).size();
    }

    @LogExecutionTime
    public JobCardTemplateResponse.TemplateStatistics getTemplateStatistics(Long templateId) {
        log.debug("Calculating statistics for template: {}", templateId);

        List<com.protomil.core.jobcard.domain.JobCard> allJobCards = jobCardRepository.findByTemplateId(templateId);

        long totalJobCards = allJobCards.size();
        long activeJobCards = allJobCards.stream()
                .filter(jc -> jc.getStatus().isActiveStatus())
                .count();
        long completedJobCards = allJobCards.stream()
                .filter(jc -> jc.getStatus() == com.protomil.core.jobcard.domain.enums.JobStatus.COMPLETED)
                .count();

        // Calculate average completion time
        Double averageCompletionTime = allJobCards.stream()
                .filter(jc -> jc.getActualDurationMinutes() != null && jc.getActualDurationMinutes() > 0)
                .mapToInt(com.protomil.core.jobcard.domain.JobCard::getActualDurationMinutes)
                .average()
                .orElse(0.0);

        // Calculate success rate (completed vs cancelled)
        long cancelledJobCards = allJobCards.stream()
                .filter(jc -> jc.getStatus() == com.protomil.core.jobcard.domain.enums.JobStatus.CANCELLED)
                .count();
        double successRate = totalJobCards > 0 ?
                ((double) completedJobCards / (completedJobCards + cancelledJobCards)) * 100.0 : 0.0;

        // Get field counts
        Long totalFields = fieldDefinitionRepository.countByTemplateId(templateId);
        Long requiredFields = fieldDefinitionRepository.countRequiredFieldsByTemplateId(templateId);

        return JobCardTemplateResponse.TemplateStatistics.builder()
                .totalJobCardsCreated(totalJobCards)
                .activeJobCards(activeJobCards)
                .completedJobCards(completedJobCards)
                .averageCompletionTimeMinutes(averageCompletionTime)
                .successRatePercentage(successRate)
                .totalFields(totalFields.intValue())
                .requiredFields(requiredFields.intValue())
                .build();
    }

    @LogExecutionTime
    public JobCardTemplateResponse.TemplateUsageInfo getTemplateUsageInfo(Long templateId) {
        log.debug("Getting usage info for template: {}", templateId);

        JobCardTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with ID: " + templateId));

        boolean canCreateJobCards = template.getIsActive();
        String unavailabilityReason = !canCreateJobCards ? "Template is inactive" : null;

        // Get last used timestamp
        LocalDateTime lastUsed = jobCardRepository.findByTemplateId(templateId).stream()
                .map(com.protomil.core.jobcard.domain.JobCard::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Get usage count in last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int usageCountLast30Days = (int) jobCardRepository.findByTemplateId(templateId).stream()
                .filter(jc -> jc.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        return JobCardTemplateResponse.TemplateUsageInfo.builder()
                .canCreateJobCards(canCreateJobCards)
                .unavailabilityReason(unavailabilityReason)
                .lastUsedAt(lastUsed)
                .usageCountLast30Days(usageCountLast30Days)
                .build();
    }

    private JobCardTemplateResponse mapToResponseWithStatistics(JobCardTemplate template) {
        JobCardTemplateResponse response = templateMapper.toResponse(template);

        // Add statistics and usage info
        response.setStatistics(getTemplateStatistics(template.getId()));
        response.setUsageInfo(getTemplateUsageInfo(template.getId()));

        return response;
    }

    @LogExecutionTime
    public List<String> getFieldGroupsByTemplate(Long templateId) {
        log.debug("Retrieving field groups for template: {}", templateId);

        return fieldDefinitionRepository.findDistinctFieldGroupsByTemplateId(templateId);
    }

    @LogExecutionTime
    public List<TemplateFieldDefinitionDto> getFieldsByGroup(Long templateId, String fieldGroup) {
        log.debug("Retrieving fields for template: {} and group: {}", templateId, fieldGroup);

        List<TemplateFieldDefinition> fields =
                fieldDefinitionRepository.findByTemplateIdAndFieldGroupOrderByDisplayOrder(templateId, fieldGroup);

        return fields.stream()
                .map(templateMapper::toFieldDefinitionDto)
                .collect(Collectors.toList());
    }

    @LogExecutionTime
    public boolean validateTemplateForJobCard(Long templateId) {
        log.debug("Validating template {} for job card creation", templateId);

        JobCardTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found with ID: " + templateId));

        if (!template.getIsActive()) {
            log.warn("Template {} is inactive", templateId);
            return false;
        }

        // Check if template has required fields properly configured
        List<TemplateFieldDefinition> requiredFields =
                fieldDefinitionRepository.findByTemplateIdAndIsRequiredTrueOrderByDisplayOrder(templateId);

        boolean hasValidRequiredFields = requiredFields.stream()
                .allMatch(field -> {
                    boolean hasValidOptions = !field.getFieldType().requiresOptions() ||
                            (field.getFieldOptions() != null && !field.getFieldOptions().isEmpty());
                    return hasValidOptions;
                });

        if (!hasValidRequiredFields) {
            log.warn("Template {} has required fields with missing options", templateId);
            return false;
        }

        return true;
    }
}