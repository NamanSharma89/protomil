package com.protomil.core.jobcard.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_card_assignments", indexes = {
        @Index(name = "idx_job_card_assignments_job", columnList = "job_card_id"),
        @Index(name = "idx_job_card_assignments_user", columnList = "assigned_to"),
        @Index(name = "idx_job_card_assignments_active", columnList = "is_active"),
        @Index(name = "idx_job_card_assignments_assigned_by", columnList = "assigned_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private com.protomil.core.jobcard.domain.Machine machine;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    @Column(name = "assigned_at")
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "unassignment_reason", columnDefinition = "TEXT")
    private String unassignmentReason;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Helper methods
    public boolean isCurrentlyActive() {
        return Boolean.TRUE.equals(isActive) && unassignedAt == null;
    }

    public void unassign(String reason) {
        this.isActive = false;
        this.unassignedAt = LocalDateTime.now();
        this.unassignmentReason = reason;
    }

    public long getAssignmentDurationMinutes() {
        LocalDateTime endTime = unassignedAt != null ? unassignedAt : LocalDateTime.now();
        return java.time.Duration.between(assignedAt, endTime).toMinutes();
    }
}