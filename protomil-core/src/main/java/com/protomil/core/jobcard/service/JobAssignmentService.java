package com.protomil.core.jobcard.service;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.JobCardAssignment;
import com.protomil.core.jobcard.domain.Machine;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.dto.JobAssignmentRequest;
import com.protomil.core.jobcard.dto.JobCardResponse;
import com.protomil.core.jobcard.events.JobCardAssignedEvent;
import com.protomil.core.jobcard.exception.JobCardNotFoundException;
import com.protomil.core.jobcard.mapper.JobCardMapper;
import com.protomil.core.jobcard.repository.JobCardAssignmentRepository;
import com.protomil.core.jobcard.repository.JobCardRepository;
import com.protomil.core.jobcard.repository.MachineRepository;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.logging.LogExecutionTime;
import com.protomil.core.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JobAssignmentService {

    private final JobCardRepository jobCardRepository;
    private final JobCardAssignmentRepository assignmentRepository;
    private final MachineRepository machineRepository;
    private final JobCardMapper jobCardMapper;
    private final ApplicationEventPublisher eventPublisher;

    @LogExecutionTime
    public JobCardResponse assignJobCard(Long jobCardId, JobAssignmentRequest request) {
        log.debug("Assigning job card {} to user {}", jobCardId, request.getAssignedTo());

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new JobCardNotFoundException("Job card not found with ID: " + jobCardId));

        if (!jobCard.canBeAssigned()) {
            throw new BusinessException(
                    String.format("Cannot assign job card in status %s", jobCard.getStatus())
            );
        }

        // Unassign current assignment if exists
        unassignJobCard(jobCardId, "Reassigning to new personnel");

        // Validate machine availability if specified
        Machine machine = null;
        if (request.getMachineId() != null) {
            machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new BusinessException("Machine not found with ID: " + request.getMachineId()));

            if (!machine.isAvailable()) {
                throw new BusinessException("Machine " + machine.getMachineCode() + " is not available");
            }
        }

        // Create new assignment
        JobCardAssignment assignment = JobCardAssignment.builder()
                .jobCard(jobCard)
                .assignedTo(request.getAssignedTo())
                .assignedBy(SecurityUtils.getCurrentUserId())
                .machine(machine)
                .assignmentReason(request.getAssignmentReason())
                .isActive(true)
                .build();

        assignmentRepository.save(assignment);

        // Update job card
        jobCard.setAssignedTo(request.getAssignedTo());
        jobCard.setStatus(JobStatus.ASSIGNED);

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Assigned job card {} to user {}", jobCard.getJobNumber(), request.getAssignedTo());

        // Publish assignment event
        eventPublisher.publishEvent(new JobCardAssignedEvent(
                this, savedJobCard, request.getAssignedTo(), machine
        ));

        return jobCardMapper.toResponse(savedJobCard);
    }

    @LogExecutionTime
    public JobCardResponse unassignJobCard(Long jobCardId, String reason) {
        log.debug("Unassigning job card {} for reason: {}", jobCardId, reason);

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new JobCardNotFoundException("Job card not found with ID: " + jobCardId));

        // Find active assignment
        assignmentRepository.findByJobCardIdAndIsActiveTrue(jobCardId)
                .ifPresent(assignment -> {
                    assignment.unassign(reason);
                    assignmentRepository.save(assignment);
                });

        // Update job card
        jobCard.setAssignedTo(null);
        if (jobCard.getStatus() == JobStatus.ASSIGNED) {
            jobCard.setStatus(JobStatus.READY);
        }

        JobCard savedJobCard = jobCardRepository.save(jobCard);

        log.info("Unassigned job card {} - Reason: {}", jobCard.getJobNumber(), reason);

        return jobCardMapper.toResponse(savedJobCard);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardAssignment> getAssignmentHistory(Long jobCardId) {
        log.debug("Retrieving assignment history for job card: {}", jobCardId);
        return assignmentRepository.findByJobCardIdOrderByAssignedAtDesc(jobCardId);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<JobCardAssignment> getActiveAssignmentsByUser(UUID userId) {
        log.debug("Retrieving active assignments for user: {}", userId);
        return assignmentRepository.findByAssignedToAndIsActiveTrueOrderByAssignedAtDesc(userId);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public Long getActiveAssignmentCountByUser(UUID userId) {
        log.debug("Counting active assignments for user: {}", userId);
        return assignmentRepository.countActiveAssignmentsByUser(userId);
    }
}