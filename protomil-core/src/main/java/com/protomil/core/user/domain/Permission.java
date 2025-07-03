// com/protomil/core/user/domain/Permission.java
package com.protomil.core.user.domain;

import com.protomil.core.shared.domain.AuditableEntity;
import com.protomil.core.shared.domain.enums.PermissionStatus;
import com.protomil.core.shared.domain.enums.ResourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_name", columnList = "name"),
        @Index(name = "idx_permission_resource", columnList = "resource_type")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name; // e.g., "JOB_CARD_CREATE", "PERSONNEL_READ"

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType; // JOB_CARD, PERSONNEL, EQUIPMENT, etc.

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, READ, UPDATE, DELETE, APPROVE, ASSIGN

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PermissionStatus status = PermissionStatus.ACTIVE;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
