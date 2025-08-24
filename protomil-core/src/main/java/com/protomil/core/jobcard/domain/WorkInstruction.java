package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.InstructionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_instructions",
        uniqueConstraints = @UniqueConstraint(name = "unique_step_per_job",
                columnNames = {"job_card_id", "step_number"}),
        indexes = {
                @Index(name = "idx_work_instructions_job_card", columnList = "job_card_id"),
                @Index(name = "idx_work_instructions_step_order", columnList = "job_card_id, step_number")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "instruction_type", nullable = false, length = 50)
    @Builder.Default
    private InstructionType instructionType = InstructionType.TEXT;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @Column(name = "is_quality_checkpoint")
    @Builder.Default
    private Boolean isQualityCheckpoint = false;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Helper methods
    public boolean isCompleted() {
        return this.completedAt != null;
    }

    public boolean isQualityStep() {
        return Boolean.TRUE.equals(this.isQualityCheckpoint);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}