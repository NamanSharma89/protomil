// com/protomil/core/user/domain/RolePermission.java
package com.protomil.core.user.domain;

import com.protomil.core.shared.domain.AuditableEntity;
import com.protomil.core.shared.domain.enums.RolePermissionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RolePermissionStatus status = RolePermissionStatus.ACTIVE;
}