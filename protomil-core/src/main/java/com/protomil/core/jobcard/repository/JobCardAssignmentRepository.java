package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.JobCardAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobCardAssignmentRepository extends JpaRepository<JobCardAssignment, Long> {

    List<JobCardAssignment> findByJobCardIdOrderByAssignedAtDesc(Long jobCardId);

    List<JobCardAssignment> findByAssignedToAndIsActiveTrueOrderByAssignedAtDesc(Long assignedTo);

    Optional<JobCardAssignment> findByJobCardIdAndIsActiveTrue(Long jobCardId);

    List<JobCardAssignment> findByAssignedByAndAssignedAtBetween(Long assignedBy, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(jca) FROM JobCardAssignment jca WHERE jca.assignedTo = :userId AND jca.isActive = true")
    Long countActiveAssignmentsByUser(@Param("userId") Long userId);
}