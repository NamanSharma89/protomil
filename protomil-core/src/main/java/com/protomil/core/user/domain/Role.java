package com.protomil.core.user.domain;

import com.protomil.core.shared.domain.AuditableEntity;
import com.protomil.core.shared.domain.enums.RoleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// com/protomil/core/user/domain/Role.java
@Entity
@Table(name = "roles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "aws_iam_role_arn")
    private String awsIamRoleArn;

    @Column(name = "aws_policy_arn")
    private String awsPolicyArn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus status;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RolePermission> rolePermissions = new ArrayList<>();
}