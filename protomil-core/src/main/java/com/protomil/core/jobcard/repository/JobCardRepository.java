package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.JobCard;
import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.domain.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobCardRepository extends JpaRepository<JobCard, Long>, JpaSpecificationExecutor<JobCard> {

    // Basic finders
    Optional<JobCard> findByJobNumber(String jobNumber);

    boolean existsByJobNumber(String jobNumber);

    // Status-based queries
    List<JobCard> findByStatus(JobStatus status);

    List<JobCard> findByStatusIn(List<JobStatus> statuses);

    Page<JobCard> findByStatus(JobStatus status, Pageable pageable);

    // Assignment queries
    List<JobCard> findByAssignedTo(Long assignedTo);

    Page<JobCard> findByAssignedTo(Long assignedTo, Pageable pageable);

    List<JobCard> findByCreatedBy(Long createdBy);

    // Priority and date queries
    List<JobCard> findByPriorityOrderByCreatedAtDesc(Priority priority);

    List<JobCard> findByTargetCompletionDateBefore(LocalDateTime date);

    List<JobCard> findByTargetCompletionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Template-based queries
    List<JobCard> findByTemplateId(Long templateId);

    @Query("SELECT jc FROM JobCard jc WHERE jc.template.id = :templateId AND jc.status = :status")
    List<JobCard> findByTemplateIdAndStatus(@Param("templateId") Long templateId, @Param("status") JobStatus status);

    // Active job cards (excluding completed and cancelled)
    @Query("SELECT jc FROM JobCard jc WHERE jc.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<JobCard> findActiveJobCards();

    @Query("SELECT jc FROM JobCard jc WHERE jc.status NOT IN ('COMPLETED', 'CANCELLED')")
    Page<JobCard> findActiveJobCards(Pageable pageable);

    // Overdue job cards
    @Query("SELECT jc FROM JobCard jc WHERE jc.targetCompletionDate < :currentTime AND jc.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<JobCard> findOverdueJobCards(@Param("currentTime") LocalDateTime currentTime);

    // Dashboard queries
    @Query("SELECT jc FROM JobCard jc WHERE jc.assignedTo = :userId AND jc.status IN ('ASSIGNED', 'IN_PROGRESS')")
    List<JobCard> findActiveJobCardsByUser(@Param("userId") Long userId);

    @Query("SELECT jc FROM JobCard jc WHERE jc.createdBy = :supervisorId AND jc.status = :status")
    List<JobCard> findJobCardsBySupervisorAndStatus(@Param("supervisorId") Long supervisorId, @Param("status") JobStatus status);

    // Statistics queries
    @Query("SELECT COUNT(jc) FROM JobCard jc WHERE jc.status = :status")
    Long countByStatus(@Param("status") JobStatus status);

    @Query("SELECT COUNT(jc) FROM JobCard jc WHERE jc.assignedTo = :userId AND jc.status = :status")
    Long countByAssignedToAndStatus(@Param("userId") Long userId, @Param("status") JobStatus status);

    @Query("SELECT COUNT(jc) FROM JobCard jc WHERE jc.createdAt >= :startDate AND jc.createdAt <= :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Efficiency queries
    @Query("SELECT AVG(jc.actualDurationMinutes) FROM JobCard jc WHERE jc.status = 'COMPLETED' AND jc.actualDurationMinutes IS NOT NULL")
    Double findAverageCompletionTime();

    @Query("SELECT jc FROM JobCard jc WHERE jc.actualDurationMinutes > jc.estimatedDurationMinutes AND jc.status = 'COMPLETED'")
    List<JobCard> findJobCardsExceedingEstimatedTime();

    // Search functionality
    @Query("SELECT jc FROM JobCard jc WHERE " +
            "LOWER(jc.jobNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(jc.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(jc.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<JobCard> searchJobCards(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Complex queries with joins
    @Query("SELECT jc FROM JobCard jc " +
            "LEFT JOIN FETCH jc.template t " +
            "LEFT JOIN FETCH jc.workInstructions wi " +
            "WHERE jc.id = :id")
    Optional<JobCard> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT jc FROM JobCard jc " +
            "LEFT JOIN FETCH jc.productionRecords pr " +
            "WHERE jc.status = 'COMPLETED' AND pr.isValidated = true")
    List<JobCard> findCompletedAndValidatedJobCards();
}