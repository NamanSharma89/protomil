package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.JobStatus;
import com.protomil.core.jobcard.domain.enums.Priority;
import com.protomil.core.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "job_cards", indexes = {
        @Index(name = "idx_job_cards_status", columnList = "status"),
        @Index(name = "idx_job_cards_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_job_cards_created_by", columnList = "created_by"),
        @Index(name = "idx_job_cards_template", columnList = "template_id"),
        @Index(name = "idx_job_cards_job_number", columnList = "job_number"),
        @Index(name = "idx_job_cards_target_date", columnList = "target_completion_date"),
        @Index(name = "idx_job_cards_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "job_number", unique = true, nullable = false, length = 50)
    private String jobNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private JobCardTemplate template;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "target_completion_date")
    private LocalDateTime targetCompletionDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_fields", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> dynamicFields = Map.of();

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    // Relationships
    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkInstruction> workInstructions = new ArrayList<>();

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobCardAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
        private List<JobCardAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobCardStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobCardComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "jobCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductionMaster> productionRecords = new ArrayList<>();

    // Helper methods
    public boolean isInProgress() {
        return this.status == JobStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this.status == JobStatus.COMPLETED;
    }

    public boolean canBeAssigned() {
        return this.status == JobStatus.DRAFT || this.status == JobStatus.READY;
    }

    public boolean canBeStarted() {
        return this.status == JobStatus.ASSIGNED && this.assignedTo != null;
    }

    public void addWorkInstruction(WorkInstruction workInstruction) {
        this.workInstructions.add(workInstruction);
        workInstruction.setJobCard(this);
    }

    public void addComment(JobCardComment comment) {
        this.comments.add(comment);
        comment.setJobCard(this);
    }
}