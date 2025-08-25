package com.protomil.core.jobcard.service;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.JobCardTemplate;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.dto.JobCardCreateRequest;
import com.protomil.core.jobcard.dto.JobCardResponse;
import com.protomil.core.jobcard.dto.JobCardSummary;
import com.protomil.core.jobcard.dto.JobCardUpdateRequest;
import com.protomil.core.jobcard.events.JobCardCreatedEvent;
import com.protomil.core.jobcard.events.JobCardStatusChangedEvent;
import com.protomil.core.jobcard.exception.InvalidJobStatusTransitionException;
import com.protomil.core.jobcard.exception.JobCardNotFoundException;
import com.protomil.core.jobcard.mapper.JobCardMapper;
import com.protomil.core.jobcard.repository.JobCardRepository;
import com.protomil.core.jobcard.repository.JobCardTemplateRepository;
import com.protomil.core.jobcard.specification.JobCardSpecifications;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository jobCardRepository;
    private final JobCardTemplateRepository templateRepository;
    private final JobCardMapper jobCardMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final JobNumberService jobNumberService;

    @Override
    @LogExecutionTime
    public JobCardResponse createJobCard(JobCardCreateRequest request) {
        log.debug("Creating job card with template ID: {}", request.getTemplateId());

        // Validate template exists
        JobCardTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new BusinessException("Template not found with ID: " + request.getTemplateId()));

        if (!template.getIsActive()) {
            throw new BusinessException("Cannot create job card from inactive template");
        }

        // Generate job number
        String jobNumber = jobNumberService.generateJobNumber(template.getCategory());

        // Create job card
        JobCard jobCard = JobCard.builder()
                .jobNumber(jobNumber)
                .template(template)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .targetCompletionDate(request.getTargetCompletionDate())
                .dynamicFields(request.getDynamicFields() != null ? request.getDynamicFields() : Map.of())
                .createdBy(SecurityUtils.getCurrentUserId())
                .status(JobStatus.DRAFT)
                .build();

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Created job card {} with ID: {}", jobNumber, savedJobCard.getId());

        // Publish event
        eventPublisher.publishEvent(new JobCardCreatedEvent(this, savedJobCard));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @Override
    @LogExecutionTime
    public JobCardResponse updateJobCard(Long jobCardId, JobCardUpdateRequest request) {
        log.debug("Updating job card with ID: {}", jobCardId);

        JobCard jobCard = getJobCardEntityById(jobCardId);

        // Only allow updates for DRAFT status
        if (jobCard.getStatus() != JobStatus.DRAFT) {
            throw new BusinessException("Can only update job cards in DRAFT status");
        }

        // Update fields
        if (StringUtils.hasText(request.getTitle())) {
            jobCard.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getDescription())) {
            jobCard.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            jobCard.setPriority(request.getPriority());
        }
        if (request.getEstimatedDurationMinutes() != null) {
            jobCard.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        }
        if (request.getTargetCompletionDate() != null) {
            jobCard.setTargetCompletionDate(request.getTargetCompletionDate());
        }
        if (request.getDynamicFields() != null) {
            jobCard.setDynamicFields(request.getDynamicFields());
        }

        JobCard updatedJobCard = jobCardRepository.save(jobCard);

        log.info("Updated job card with ID: {}", jobCardId);

        return jobCardMapper.toResponse(updatedJobCard);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public JobCardResponse getJobCardById(Long jobCardId) {
        log.debug("Retrieving job card with ID: {}", jobCardId);

        JobCard jobCard = jobCardRepository.findByIdWithDetails(jobCardId)
                .orElseThrow(() -> new JobCardNotFoundException("Job card not found with ID: " + jobCardId));

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public JobCardResponse getJobCardByJobNumber(String jobNumber) {
        log.debug("Retrieving job card with job number: {}", jobNumber);

        JobCard jobCard = jobCardRepository.findByJobNumber(jobNumber)
                .orElseThrow(() -> new JobCardNotFoundException("Job card not found with job number: " + jobNumber));

        return jobCardMapper.toResponse(jobCard);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public Page<JobCardSummary> getJobCards(JobStatus status, UUID assignedTo, UUID createdBy, Pageable pageable) {
        log.debug("Retrieving job cards with filters - status: {}, assignedTo: {}, createdBy: {}",
                status, assignedTo, createdBy);

        Page<JobCard> jobCards;

        if (status != null && assignedTo != null) {
            // Custom query needed for multiple filters
            jobCards = jobCardRepository.findAll(
                    JobCardSpecifications.hasStatusAndAssignedTo(status, assignedTo),
                    pageable
            );
        } else if (status != null) {
            jobCards = jobCardRepository.findByStatus(status, pageable);
        } else if (assignedTo != null) {
            jobCards = jobCardRepository.findByAssignedTo(assignedTo, pageable);
        } else if (createdBy != null) {
            jobCards = jobCardRepository.findByCreatedBy(createdBy).stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> new PageImpl<>(list, pageable, list.size())
                    ));
        } else {
            jobCards = jobCardRepository.findAll(pageable);
        }

        return jobCards.map(jobCardMapper::toSummary);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public Page<JobCardSummary> searchJobCards(String searchTerm, Pageable pageable) {
        log.debug("Searching job cards with term: {}", searchTerm);

        Page<JobCard> jobCards = jobCardRepository.searchJobCards(searchTerm, pageable);
        return jobCards.map(jobCardMapper::toSummary);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardSummary> getActiveJobCardsByUser(UUID userId) {
        log.debug("Retrieving active job cards for user: {}", userId);

        List<JobCard> jobCards = jobCardRepository.findActiveJobCardsByUser(userId);
        return jobCards.stream()
                .map(jobCardMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardSummary> getOverdueJobCards() {
        log.debug("Retrieving overdue job cards");

        List<JobCard> jobCards = jobCardRepository.findOverdueJobCards(LocalDateTime.now());
        return jobCards.stream()
                .map(jobCardMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @LogExecutionTime
    public JobCardResponse startJobCard(Long jobCardId) {
        log.debug("Starting job card with ID: {}", jobCardId);

        JobCard jobCard = getJobCardEntityById(jobCardId);

        if (!jobCard.canBeStarted()) {
            throw new InvalidJobStatusTransitionException(
                    String.format("Cannot start job card in status %s", jobCard.getStatus())
            );
        }

        JobStatus previousStatus = jobCard.getStatus();
        jobCard.setStatus(JobStatus.IN_PROGRESS);
        jobCard.setStartedAt(LocalDateTime.now());

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Started job card {} (ID: {})", jobCard.getJobNumber(), jobCardId);

        // Publish status change event
        eventPublisher.publishEvent(new JobCardStatusChangedEvent(
                this, savedJobCard, previousStatus, JobStatus.IN_PROGRESS
        ));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @Override
    @LogExecutionTime
    public JobCardResponse completeJobCard(Long jobCardId) {
        log.debug("Completing job card with ID: {}", jobCardId);

        JobCard jobCard = getJobCardEntityById(jobCardId);

        if (jobCard.getStatus() != JobStatus.IN_PROGRESS && jobCard.getStatus() != JobStatus.PENDING_REVIEW) {
            throw new InvalidJobStatusTransitionException(
                    String.format("Cannot complete job card in status %s", jobCard.getStatus())
            );
        }

        JobStatus previousStatus = jobCard.getStatus();
        jobCard.setStatus(JobStatus.COMPLETED);
        jobCard.setCompletedAt(LocalDateTime.now());

        // Calculate actual duration
        if (jobCard.getStartedAt() != null) {
            long durationMinutes = java.time.Duration.between(
                    jobCard.getStartedAt(),
                    jobCard.getCompletedAt()
            ).toMinutes();
            jobCard.setActualDurationMinutes((int) durationMinutes);
        }

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Completed job card {} (ID: {})", jobCard.getJobNumber(), jobCardId);

        // Publish status change event
        eventPublisher.publishEvent(new JobCardStatusChangedEvent(
                this, savedJobCard, previousStatus, JobStatus.COMPLETED
        ));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @Override
    @LogExecutionTime
    public JobCardResponse cancelJobCard(Long jobCardId, String reason) {
        log.debug("Cancelling job card with ID: {} for reason: {}", jobCardId, reason);

        JobCard jobCard = getJobCardEntityById(jobCardId);

        if (jobCard.getStatus().isFinalStatus()) {
            throw new InvalidJobStatusTransitionException(
                    String.format("Cannot cancel job card in final status %s", jobCard.getStatus())
            );
        }

        JobStatus previousStatus = jobCard.getStatus();
        jobCard.setStatus(JobStatus.CANCELLED);

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Cancelled job card {} (ID: {}) - Reason: {}", jobCard.getJobNumber(), jobCardId, reason);

        // Publish status change event
        eventPublisher.publishEvent(new JobCardStatusChangedEvent(
                this, savedJobCard, previousStatus, JobStatus.CANCELLED
        ));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @Override
    @LogExecutionTime
    public JobCardResponse changeJobCardStatus(Long jobCardId, JobStatus newStatus, String reason) {
        log.debug("Changing job card {} status to: {} for reason: {}", jobCardId, newStatus, reason);

        JobCard jobCard = getJobCardEntityById(jobCardId);
        JobStatus currentStatus = jobCard.getStatus();

        // Validate status transition
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidJobStatusTransitionException(
                    String.format("Cannot transition from %s to %s", currentStatus, newStatus)
            );
        }

        jobCard.setStatus(newStatus);

        // Update timing fields based on status
        switch (newStatus) {
            case IN_PROGRESS:
                if (jobCard.getStartedAt() == null) {
                    jobCard.setStartedAt(LocalDateTime.now());
                }
                break;
            case COMPLETED:
                if (jobCard.getCompletedAt() == null) {
                    jobCard.setCompletedAt(LocalDateTime.now());
                    if (jobCard.getStartedAt() != null) {
                        long durationMinutes = java.time.Duration.between(
                                jobCard.getStartedAt(),
                                jobCard.getCompletedAt()
                        ).toMinutes();
                        jobCard.setActualDurationMinutes((int) durationMinutes);
                    }
                }
                break;
        }

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Changed job card {} status from {} to {}",
                jobCard.getJobNumber(), currentStatus, newStatus);

        // Publish status change event
        eventPublisher.publishEvent(new JobCardStatusChangedEvent(
                this, savedJobCard, currentStatus, newStatus
        ));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @Override
    @LogExecutionTime
    public void deleteJobCard(Long jobCardId) {
        log.debug("Deleting job card with ID: {}", jobCardId);

        JobCard jobCard = getJobCardEntityById(jobCardId);

        // Only allow deletion of DRAFT status job cards
        if (jobCard.getStatus() != JobStatus.DRAFT) {
            throw new BusinessException("Can only delete job cards in DRAFT status");
        }

        jobCardRepository.delete(jobCard);

        log.info("Deleted job card {} (ID: {})", jobCard.getJobNumber(), jobCardId);
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public Map<JobStatus, Long> getJobCardStatusCounts() {
        log.debug("Retrieving job card status counts");

        return Arrays.stream(JobStatus.values())
                .collect(Collectors.toMap(
                        status -> status,
                        status -> jobCardRepository.countByStatus(status)
                ));
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardSummary> getJobCardsByTemplate(Long templateId) {
        log.debug("Retrieving job cards for template: {}", templateId);

        List<JobCard> jobCards = jobCardRepository.findByTemplateId(templateId);
        return jobCards.stream()
                .map(jobCardMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public Double getAverageCompletionTime() {
        log.debug("Calculating average completion time");
        return jobCardRepository.findAverageCompletionTime();
    }

    @Override
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardSummary> getJobCardsExceedingEstimatedTime() {
        log.debug("Retrieving job cards exceeding estimated time");

        List<JobCard> jobCards = jobCardRepository.findJobCardsExceedingEstimatedTime();
        return jobCards.stream()
                .map(jobCardMapper::toSummary)
                .collect(Collectors.toList());
    }

    private JobCard getJobCardEntityById(Long jobCardId) {
        return jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new JobCardNotFoundException("Job card not found with ID: " + jobCardId));
    }
}