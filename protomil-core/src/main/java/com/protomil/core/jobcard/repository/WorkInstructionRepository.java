package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.WorkInstruction;
import com.protomil.core.jobcard.domain.enums.InstructionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkInstructionRepository extends JpaRepository<WorkInstruction, Long> {

    // Job card based queries
    List<WorkInstruction> findByJobCardIdOrderByStepNumber(Long jobCardId);

    List<WorkInstruction> findByJobCardIdAndCompletedAtIsNullOrderByStepNumber(Long jobCardId);

    List<WorkInstruction> findByJobCardIdAndCompletedAtIsNotNullOrderByStepNumber(Long jobCardId);

    // Step number queries
    Optional<WorkInstruction> findByJobCardIdAndStepNumber(Long jobCardId, Integer stepNumber);

    @Query("SELECT wi FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.stepNumber = " +
            "(SELECT MIN(wi2.stepNumber) FROM WorkInstruction wi2 WHERE wi2.jobCard.id = :jobCardId AND wi2.completedAt IS NULL)")
    Optional<WorkInstruction> findNextIncompleteStep(@Param("jobCardId") Long jobCardId);

    // Quality checkpoint queries
    List<WorkInstruction> findByJobCardIdAndIsQualityCheckpointTrueOrderByStepNumber(Long jobCardId);

    @Query("SELECT wi FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.isQualityCheckpoint = true AND wi.completedAt IS NULL ORDER BY wi.stepNumber")
    List<WorkInstruction> findIncompleteQualityCheckpoints(@Param("jobCardId") Long jobCardId);

    // Instruction type queries
    List<WorkInstruction> findByJobCardIdAndInstructionTypeOrderByStepNumber(Long jobCardId, InstructionType instructionType);

    // Completion tracking
    @Query("SELECT COUNT(wi) FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId")
    Long countByJobCardId(@Param("jobCardId") Long jobCardId);

    @Query("SELECT COUNT(wi) FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.completedAt IS NOT NULL")
    Long countCompletedByJobCardId(@Param("jobCardId") Long jobCardId);

    // Progress calculation
    @Query("SELECT (COUNT(wi) * 100.0 / (SELECT COUNT(wi2) FROM WorkInstruction wi2 WHERE wi2.jobCard.id = :jobCardId)) " +
            "FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.completedAt IS NOT NULL")
    Double calculateCompletionPercentage(@Param("jobCardId") Long jobCardId);

    // Time tracking
    @Query("SELECT SUM(wi.actualDurationMinutes) FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.actualDurationMinutes IS NOT NULL")
    Integer sumActualDurationByJobCardId(@Param("jobCardId") Long jobCardId);

    @Query("SELECT SUM(wi.estimatedDurationMinutes) FROM WorkInstruction wi WHERE wi.jobCard.id = :jobCardId AND wi.estimatedDurationMinutes IS NOT NULL")
    Integer sumEstimatedDurationByJobCardId(@Param("jobCardId") Long jobCardId);

    // User completion tracking
    List<WorkInstruction> findByCompletedByAndCompletedAtBetweenOrderByCompletedAtDesc(
            Long completedBy, LocalDateTime startDate, LocalDateTime endDate);

    // Overdue instructions
    @Query("SELECT wi FROM WorkInstruction wi " +
            "WHERE wi.jobCard.targetCompletionDate < :currentTime " +
            "AND wi.completedAt IS NULL " +
            "AND wi.jobCard.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<WorkInstruction> findOverdueInstructions(@Param("currentTime") LocalDateTime currentTime);
}