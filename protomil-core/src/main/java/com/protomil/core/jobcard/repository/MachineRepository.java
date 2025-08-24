package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.Machine;
import com.protomil.core.jobcard.domain.enums.MachineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByMachineCode(String machineCode);

    boolean existsByMachineCode(String machineCode);

    List<Machine> findByIsActiveTrueOrderByMachineCode();

    List<Machine> findByStatusAndIsActiveTrue(MachineStatus status);

    List<Machine> findBySectionCodeAndIsActiveTrue(String sectionCode);

    Page<Machine> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT DISTINCT m.sectionCode FROM Machine m WHERE m.isActive = true ORDER BY m.sectionCode")
    List<String> findDistinctActiveSections();

    @Query("SELECT m FROM Machine m WHERE LOWER(m.machineName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(m.machineCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Machine> searchMachines(@Param("searchTerm") String searchTerm, Pageable pageable);
}