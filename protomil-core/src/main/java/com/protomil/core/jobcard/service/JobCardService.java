package com.protomil.core.jobcard.service;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.dto.JobCardCreateRequest;
import com.protomil.core.jobcard.dto.JobCardResponse;
import com.protomil.core.jobcard.dto.JobCardSummary;
import com.protomil.core.jobcard.dto.JobCardUpdateRequest;
import com.protomil.core.shared.logging.LogExecutionTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface JobCardService {

    @LogExecutionTime
    JobCardResponse createJobCard(JobCardCreateRequest request);

    @LogExecutionTime
    JobCardResponse updateJobCard(Long jobCardId, JobCardUpdateRequest request);

    @LogExecutionTime
    JobCardResponse getJobCardById(Long jobCardId);

    @LogExecutionTime
    JobCardResponse getJobCardByJobNumber(String jobNumber);

    @LogExecutionTime
    Page<JobCardSummary> getJobCards(JobStatus status, UUID assignedTo, UUID createdBy, Pageable pageable);

    @LogExecutionTime
    Page<JobCardSummary> searchJobCards(String searchTerm, Pageable pageable);

    @LogExecutionTime
    List<JobCardSummary> getActiveJobCardsByUser(UUID userId);

    @LogExecutionTime
    List<JobCardSummary> getOverdueJobCards();

    @LogExecutionTime
    JobCardResponse startJobCard(Long jobCardId);

    @LogExecutionTime
    JobCardResponse completeJobCard(Long jobCardId);

    @LogExecutionTime
    JobCardResponse cancelJobCard(Long jobCardId, String reason);

    @LogExecutionTime
    JobCardResponse changeJobCardStatus(Long jobCardId, JobStatus newStatus, String reason);

    @LogExecutionTime
    void deleteJobCard(Long jobCardId);

    @LogExecutionTime
    Map<JobStatus, Long> getJobCardStatusCounts();

    @LogExecutionTime
    List<JobCardSummary> getJobCardsByTemplate(Long templateId);

    @LogExecutionTime
    Double getAverageCompletionTime();

    @LogExecutionTime
    List<JobCardSummary> getJobCardsExceedingEstimatedTime();
}