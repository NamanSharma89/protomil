// com/protomil/core/user/domain/User.java
package com.protomil.core.user.domain;

import com.protomil.core.shared.domain.AuditableEntity;
import com.protomil.core.shared.domain.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_cognito_sub", columnList = "cognito_user_sub"),
        @Index(name = "idx_user_employee_id", columnList = "employee_id"),
        @Index(name = "idx_user_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "cognito_user_sub", unique = true, nullable = false, length = 255)
    private String cognitoUserSub; // Cognito User Pool Sub (unique identifier)

    @Column(name = "email", unique = true, nullable = false, length = 255)
    @Email
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status;

    // Business-specific fields (not in Cognito)
    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "aws_iam_role_arn", length = 500)
    private String awsIamRoleArn;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Application-specific role assignments
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();
}