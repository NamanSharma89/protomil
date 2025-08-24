package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "job_card_status_history", indexes = {
        @Index(name = "idx_job_card_status_history_job", columnList = "job_card_id"),
        @Index(name = "idx_job_card_status_history_date", columnList = "changed_at"),
        @Index(name = "idx_job_card_status_history_user", columnList = "changed_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private JobStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private JobStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changed_at")
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> additionalData = Map.of();

    // Helper methods
    public boolean isStatusUpgrade() {
        if (fromStatus == null || toStatus == null) return false;
        return toStatus.ordinal() > fromStatus.ordinal();
    }

    public boolean isStatusDowngrade() {
        if (fromStatus == null || toStatus == null) return false;
        return toStatus.ordinal() < fromStatus.ordinal();
    }

    public String getStatusTransitionDescription() {
        String from = fromStatus != null ? fromStatus.name() : "INITIAL";
        return String.format("%s → %s", from, toStatus.name());
    }
}