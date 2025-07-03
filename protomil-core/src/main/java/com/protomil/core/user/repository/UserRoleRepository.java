// com/protomil/core/user/repository/UserRoleRepository.java
package com.protomil.core.user.repository;

import com.protomil.core.user.domain.Role;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.domain.UserRole;
import com.protomil.core.shared.domain.enums.UserRoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserAndStatus(User user, UserRoleStatus status);

    List<UserRole> findByRoleAndStatus(Role role, UserRoleStatus status);

    boolean existsByUserAndRoleAndStatus(User user, Role role, UserRoleStatus status);

    Optional<UserRole> findByUserAndRoleAndStatus(User user, Role role, UserRoleStatus status);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.status = :status")
    List<UserRole> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") UserRoleStatus status);

    @Query("SELECT ur FROM UserRole ur WHERE ur.role.id = :roleId AND ur.status = :status")
    List<UserRole> findByRoleIdAndStatus(@Param("roleId") UUID roleId, @Param("status") UserRoleStatus status);
}