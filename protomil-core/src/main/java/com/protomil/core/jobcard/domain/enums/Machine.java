package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.MachineStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "machines", indexes = {
        @Index(name = "idx_machines_code", columnList = "machine_code"),
        @Index(name = "idx_machines_section", columnList = "section_code"),
        @Index(name = "idx_machines_status", columnList = "status"),
        @Index(name = "idx_machines_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "machine_code", unique = true, nullable = false, length = 50)
    private String machineCode;

    @Column(name = "machine_name", nullable = false, length = 200)
    private String machineName;

    @Column(name = "section_code", length = 50)
    private String sectionCode;

    @Column(name = "section_name", length = 200)
    private String sectionName;

    @Column(name = "machine_type", length = 100)
    private String machineType;

    @Column(name = "capacity_per_hour")
    private Integer capacityPerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private MachineStatus status = MachineStatus.ACTIVE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobCardAssignment> jobCardAssignments = new ArrayList<>();

    // Helper methods
    public boolean isAvailable() {
        return status == MachineStatus.ACTIVE && Boolean.TRUE.equals(isActive);
    }

    public boolean isUnderMaintenance() {
        return status == MachineStatus.MAINTENANCE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}