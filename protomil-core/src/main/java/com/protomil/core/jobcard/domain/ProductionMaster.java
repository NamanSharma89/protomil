package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.Shift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "production_master", indexes = {
        @Index(name = "idx_production_master_job_card", columnList = "job_card_id"),
        @Index(name = "idx_production_master_entry_date", columnList = "entry_date"),
        @Index(name = "idx_production_master_machine", columnList = "machine_code"),
        @Index(name = "idx_production_master_part", columnList = "part_number"),
        @Index(name = "idx_production_master_operator", columnList = "employee_number"),
        @Index(name = "idx_production_master_shift", columnList = "shift"),
        @Index(name = "idx_production_master_validation", columnList = "is_validated")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    // Entry Information
    @Column(name = "entry_date", nullable = false)
    @Builder.Default
    private LocalDate entryDate = LocalDate.now();

    @Column(name = "entry_timestamp")
    @Builder.Default
    private LocalDateTime entryTimestamp = LocalDateTime.now();

    // Machine Information
    @Column(name = "machine_code", length = 50)
    private String machineCode;

    @Column(name = "machine_name", length = 200)
    private String machineName;

    @Column(name = "section_code", length = 50)
    private String sectionCode;

    // Time Information
    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "stop_date_time")
    private LocalDateTime stopDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20)
    private Shift shift;

    @Column(name = "total_time_available_minutes")
    private Integer totalTimeAvailableMinutes;

    // Loss Time Details (8 separate fields as per Excel)
    @Column(name = "loss_time_1_minutes")
    @Builder.Default
    private Integer lossTime1Minutes = 0;

    @Column(name = "loss_time_2_minutes")
    @Builder.Default
    private Integer lossTime2Minutes = 0;

    @Column(name = "loss_time_3_minutes")
    @Builder.Default
    private Integer lossTime3Minutes = 0;

    @Column(name = "loss_time_4_minutes")
    @Builder.Default
    private Integer lossTime4Minutes = 0;

    @Column(name = "loss_time_5_minutes")
    @Builder.Default
    private Integer lossTime5Minutes = 0;

    @Column(name = "loss_time_6_minutes")
    @Builder.Default
    private Integer lossTime6Minutes = 0;

    @Column(name = "loss_time_7_minutes")
    @Builder.Default
    private Integer lossTime7Minutes = 0;

    @Column(name = "loss_time_8_minutes")
    @Builder.Default
    private Integer lossTime8Minutes = 0;

    // Break Information
    @Column(name = "lunch_tea_break_minutes")
    @Builder.Default
    private Integer lunchTeaBreakMinutes = 0;

    // Net time is calculated by the database
    @Column(name = "net_time_available_minutes", insertable = false, updatable = false)
    private Integer netTimeAvailableMinutes;

    // Personnel Information
    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(name = "operator_name", length = 200)
    private String operatorName;

    @Column(name = "team", length = 100)
    private String team;

    // Part/Operation Information
    @Column(name = "part_number", length = 100)
    private String partNumber;

    @Column(name = "part_name", length = 200)
    private String partName;

    @Column(name = "operation_number", length = 50)
    private String operationNumber;

    @Column(name = "operation_code", length = 50)
    private String operationCode;

    @Column(name = "next_operation_number", length = 50)
    private String nextOperationNumber;

    // Operation Timing
    @Column(name = "defined_operation_time_minutes", precision = 10, scale = 2)
    private BigDecimal definedOperationTimeMinutes;

    @Column(name = "actual_operation_time_minutes", precision = 10, scale = 2)
    private BigDecimal actualOperationTimeMinutes;

    // Production Quantities
    @Column(name = "production_quantity")
    @Builder.Default
    private Integer productionQuantity = 0;

    @Column(name = "production_loss_quantity")
    @Builder.Default
    private Integer productionLossQuantity = 0;

    @Column(name = "rejection_inprocess_quantity")
    @Builder.Default
    private Integer rejectionInprocessQuantity = 0;

    @Column(name = "rejection_finalstage_quantity")
    @Builder.Default
    private Integer rejectionFinalstageQuantity = 0;

    // Efficiency Metrics (calculated by database)
    @Column(name = "operator_efficiency_percentage", insertable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal operatorEfficiencyPercentage;

    @Column(name = "equipment_efficiency_percentage", insertable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal equipmentEfficiencyPercentage;

    // Additional Information
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "breakdown_idle_reason", columnDefinition = "TEXT")
    private String breakdownIdleReason;

    // System Fields
    @Column(name = "data_source", length = 50)
    @Builder.Default
    private String dataSource = "JOB_CARD";

    @Column(name = "is_validated")
    @Builder.Default
    private Boolean isValidated = false;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @OneToMany(mappedBy = "productionMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductionLossDetail> lossDetails = new ArrayList<>();

    // Helper methods
    public Integer getTotalLossTimeMinutes() {
        return (lossTime1Minutes != null ? lossTime1Minutes : 0) +
                (lossTime2Minutes != null ? lossTime2Minutes : 0) +
                (lossTime3Minutes != null ? lossTime3Minutes : 0) +
                (lossTime4Minutes != null ? lossTime4Minutes : 0) +
                (lossTime5Minutes != null ? lossTime5Minutes : 0) +
                (lossTime6Minutes != null ? lossTime6Minutes : 0) +
                (lossTime7Minutes != null ? lossTime7Minutes : 0) +
                (lossTime8Minutes != null ? lossTime8Minutes : 0);
    }

    public Integer getTotalRejectionQuantity() {
        return (rejectionInprocessQuantity != null ? rejectionInprocessQuantity : 0) +
                (rejectionFinalstageQuantity != null ? rejectionFinalstageQuantity : 0);
    }

    public boolean isValidated() {
        return Boolean.TRUE.equals(this.isValidated);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}