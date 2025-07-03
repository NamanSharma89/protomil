// com/protomil/core/user/repository/UserRepository.java
package com.protomil.core.user.repository;

import com.protomil.core.user.domain.User;
import com.protomil.core.shared.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCognitoUserSub(String cognitoUserSub);

    Optional<User> findByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByCognitoUserSub(String cognitoUserSub);

    List<User> findByStatus(UserStatus status);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.department = :department AND u.status = :status")
    List<User> findByDepartmentAndStatus(@Param("department") String department,
                                         @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate AND u.status = :status")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate,
                                 @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:searchTerm% OR u.lastName LIKE %:searchTerm% OR u.email LIKE %:searchTerm%")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}