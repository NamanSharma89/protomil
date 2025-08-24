package com.protomil.core.jobcard.mapper;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.JobCardAssignment;
import com.protomil.core.jobcard.domain.Machine;
import com.protomil.core.jobcard.domain.WorkInstruction;
import com.protomil.core.jobcard.dto.JobCardResponse;
import com.protomil.core.jobcard.dto.JobCardSummary;
import com.protomil.core.jobcard.dto.WorkInstructionDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobCardMapper {

    public JobCardResponse toResponse(JobCard jobCard) {
        if (jobCard == null) return null;

        return JobCardResponse.builder()
                .id(jobCard.getId())
                .jobNumber(jobCard.getJobNumber())
                .title(jobCard.getTitle())
                .description(jobCard.getDescription())
                .status(jobCard.getStatus())
                .priority(jobCard.getPriority())
                .template(mapTemplate(jobCard))
                .assignment(mapAssignment(jobCard))
                .timing(mapTiming(jobCard))
                .progress(mapProgress(jobCard))
                .dynamicFields(jobCard.getDynamicFields())
                .workInstructions(mapWorkInstructions(jobCard.getWorkInstructions()))
                .createdAt(jobCard.getCreatedAt())
                .updatedAt(jobCard.getUpdatedAt())
                .build();
    }

    public JobCardSummary toSummary(JobCard jobCard) {
        if (jobCard == null) return null;

        return JobCardSummary.builder()
                .id(jobCard.getId())
                .jobNumber(jobCard.getJobNumber())
                .title(jobCard.getTitle())
                .status(jobCard.getStatus())
                .priority(jobCard.getPriority())
                .templateName(jobCard.getTemplate() != null ? jobCard.getTemplate().getTemplateName() : null)
                .templateCategory(jobCard.getTemplate() != null ? jobCard.getTemplate().getCategory() : null)
                .assignedTo(jobCard.getAssignedTo())
                .createdBy(jobCard.getCreatedBy())
                .estimatedDurationMinutes(jobCard.getEstimatedDurationMinutes())
                .actualDurationMinutes(jobCard.getActualDurationMinutes())
                .targetCompletionDate(jobCard.getTargetCompletionDate())
                .startedAt(jobCard.getStartedAt())
                .completedAt(jobCard.getCompletedAt())
                .createdAt(jobCard.getCreatedAt())
                .progressPercentage(calculateProgressPercentage(jobCard))
                .isOverdue(isOverdue(jobCard))
                .totalInstructions(jobCard.getWorkInstructions() != null ? jobCard.getWorkInstructions().size() : 0)
                .completedInstructions(countCompletedInstructions(jobCard))
                .build();
    }

    private JobCardResponse.TemplateInfo mapTemplate(JobCard jobCard) {
        if (jobCard.getTemplate() == null) return null;

        return JobCardResponse.TemplateInfo.builder()
                .id(jobCard.getTemplate().getId())
                .templateName(jobCard.getTemplate().getTemplateName())
                .templateCode(jobCard.getTemplate().getTemplateCode())
                .category(jobCard.getTemplate().getCategory())
                .build();
    }

    private JobCardResponse.AssignmentInfo mapAssignment(JobCard jobCard) {
        if (jobCard.getAssignedTo() == null && jobCard.getAssignments().isEmpty()) return null;

        // Get current active assignment
        JobCardAssignment activeAssignment = jobCard.getAssignments().stream()
                .filter(JobCardAssignment::isCurrentlyActive)
                .findFirst()
                .orElse(null);

        return JobCardResponse.AssignmentInfo.builder()
                .assignedTo(jobCard.getAssignedTo())
                .assignedBy(activeAssignment != null ? activeAssignment.getAssignedBy() : null)
                .assignedAt(activeAssignment != null ? activeAssignment.getAssignedAt() : null)
                .machine(activeAssignment != null && activeAssignment.getMachine() != null ?
                        mapMachine(activeAssignment.getMachine()) : null)
                .build();
    }

    private JobCardResponse.MachineInfo mapMachine(Machine machine) {
        if (machine == null) return null;

        return JobCardResponse.MachineInfo.builder()
                .id(machine.getId())
                .machineCode(machine.getMachineCode())
                .machineName(machine.getMachineName())
                .sectionCode(machine.getSectionCode())
                .build();
    }

    private JobCardResponse.TimingInfo mapTiming(JobCard jobCard) {
        return JobCardResponse.TimingInfo.builder()
                .estimatedDurationMinutes(jobCard.getEstimatedDurationMinutes())
                .actualDurationMinutes(jobCard.getActualDurationMinutes())
                .targetCompletionDate(jobCard.getTargetCompletionDate())
                .startedAt(jobCard.getStartedAt())
                .completedAt(jobCard.getCompletedAt())
                .build();
    }

    private JobCardResponse.ProgressInfo mapProgress(JobCard jobCard) {
        int totalSteps = jobCard.getWorkInstructions() != null ? jobCard.getWorkInstructions().size() : 0;
        int completedSteps = countCompletedInstructions(jobCard);
        double progressPercentage = calculateProgressPercentage(jobCard);

        return JobCardResponse.ProgressInfo.builder()
                .totalSteps(totalSteps)
                .completedSteps(completedSteps)
                .completionPercentage(progressPercentage)
                .currentStep(getCurrentStep(jobCard))
                .build();
    }

    private List<WorkInstructionDto> mapWorkInstructions(List<WorkInstruction> workInstructions) {
        if (workInstructions == null || workInstructions.isEmpty()) {
            return List.of();
        }

        return workInstructions.stream()
                .map(this::mapWorkInstruction)
                .collect(Collectors.toList());
    }

    private WorkInstructionDto mapWorkInstruction(WorkInstruction instruction) {
        return WorkInstructionDto.builder()
                .id(instruction.getId())
                .stepNumber(instruction.getStepNumber())
                .title(instruction.getTitle())
                .description(instruction.getDescription())
                .instructionType(instruction.getInstructionType())
                .content(instruction.getContent())
                .attachments(instruction.getAttachments())
                .isQualityCheckpoint(instruction.getIsQualityCheckpoint())
                .estimatedDurationMinutes(instruction.getEstimatedDurationMinutes())
                .actualDurationMinutes(instruction.getActualDurationMinutes())
                .completedAt(instruction.getCompletedAt())
                .completedBy(instruction.getCompletedBy())
                .notes(instruction.getNotes())
                .isCompleted(instruction.isCompleted())
                .build();
    }

    private double calculateProgressPercentage(JobCard jobCard) {
        if (jobCard.getWorkInstructions() == null || jobCard.getWorkInstructions().isEmpty()) {
            return 0.0;
        }

        int totalInstructions = jobCard.getWorkInstructions().size();
        int completedInstructions = countCompletedInstructions(jobCard);

        return (double) completedInstructions / totalInstructions * 100.0;
    }

    private int countCompletedInstructions(JobCard jobCard) {
        if (jobCard.getWorkInstructions() == null) {
            return 0;
        }

        return (int) jobCard.getWorkInstructions().stream()
                .filter(WorkInstruction::isCompleted)
                .count();
    }

    private boolean isOverdue(JobCard jobCard) {
        return jobCard.getTargetCompletionDate() != null &&
                !jobCard.getStatus().isFinalStatus() &&
                LocalDateTime.now().isAfter(jobCard.getTargetCompletionDate());
    }

    private String getCurrentStep(JobCard jobCard) {
        if (jobCard.getWorkInstructions() == null || jobCard.getWorkInstructions().isEmpty()) {
            return null;
        }

        return jobCard.getWorkInstructions().stream()
                .filter(wi -> !wi.isCompleted())
                .findFirst()
                .map(wi -> "Step " + wi.getStepNumber() + ": " + wi.getTitle())
                .orElse("All steps completed");
    }
}