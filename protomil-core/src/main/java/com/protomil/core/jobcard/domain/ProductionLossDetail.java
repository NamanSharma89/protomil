package com.protomil.core.jobcard.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_loss_details",
        uniqueConstraints = @UniqueConstraint(name = "unique_loss_per_production",
                columnNames = {"production_master_id", "loss_sequence"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionLossDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_master_id", nullable = false)
    private ProductionMaster productionMaster;

    @Column(name = "loss_sequence", nullable = false)
    private Integer lossSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loss_category_id")
    private LossTimeCategory lossCategory;

    @Column(name = "loss_time_minutes", nullable = false)
    private Integer lossTimeMinutes;

    @Column(name = "loss_reason", columnDefinition = "TEXT")
    private String lossReason;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}