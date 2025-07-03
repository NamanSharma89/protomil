// com/protomil/core/user/repository/RoleRepository.java
package com.protomil.core.user.repository;

import com.protomil.core.user.domain.Role;
import com.protomil.core.shared.domain.enums.RoleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findByStatus(RoleStatus status);

    Page<Role> findByStatus(RoleStatus status, Pageable pageable);

    @Query("SELECT r FROM Role r WHERE r.name LIKE %:name% AND r.status = :status")
    Page<Role> findByNameContainingAndStatus(@Param("name") String name,
                                             @Param("status") RoleStatus status,
                                             Pageable pageable);
}