package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.ProductionMaster;
import com.protomil.core.jobcard.domain.enums.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionMasterRepository extends JpaRepository<ProductionMaster, Long> {

    // Job card based queries
    List<ProductionMaster> findByJobCardId(Long jobCardId);

    Optional<ProductionMaster> findByJobCardIdAndIsValidatedTrue(Long jobCardId);

    // Date-based queries
    List<ProductionMaster> findByEntryDate(LocalDate entryDate);

    List<ProductionMaster> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);

    Page<ProductionMaster> findByEntryDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Machine-based queries
    List<ProductionMaster> findByMachineCodeOrderByEntryDateDesc(String machineCode);

    List<ProductionMaster> findByMachineCodeAndEntryDateBetween(String machineCode, LocalDate startDate, LocalDate endDate);

    // Operator-based queries
    List<ProductionMaster> findByEmployeeNumberOrderByEntryDateDesc(String employeeNumber);

    List<ProductionMaster> findByOperatorNameContainingIgnoreCase(String operatorName);

    // Part-based queries
    List<ProductionMaster> findByPartNumberOrderByEntryDateDesc(String partNumber);

    List<ProductionMaster> findByPartNumberAndEntryDateBetween(String partNumber, LocalDate startDate, LocalDate endDate);

    // Shift-based queries
    List<ProductionMaster> findByShiftAndEntryDate(Shift shift, LocalDate entryDate);

    List<ProductionMaster> findByShiftAndEntryDateBetween(Shift shift, LocalDate startDate, LocalDate endDate);

    // Validation queries
    List<ProductionMaster> findByIsValidatedFalseOrderByEntryDateDesc();

    List<ProductionMaster> findByValidatedByAndValidatedAtBetween(Long validatedBy, LocalDateTime startDate, LocalDateTime endDate);

    // Efficiency queries
    @Query("SELECT pm FROM ProductionMaster pm WHERE pm.operatorEfficiencyPercentage < :threshold AND pm.entryDate = :date")
    List<ProductionMaster> findLowOperatorEfficiency(@Param("threshold") BigDecimal threshold, @Param("date") LocalDate date);

    @Query("SELECT pm FROM ProductionMaster pm WHERE pm.equipmentEfficiencyPercentage < :threshold AND pm.entryDate = :date")
    List<ProductionMaster> findLowEquipmentEfficiency(@Param("threshold") BigDecimal threshold, @Param("date") LocalDate date);

    // Production statistics
    @Query("SELECT SUM(pm.productionQuantity) FROM ProductionMaster pm WHERE pm.entryDate = :date")
    Long sumProductionQuantityByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(pm.productionQuantity) FROM ProductionMaster pm WHERE pm.machineCode = :machineCode AND pm.entryDate BETWEEN :startDate AND :endDate")
    Long sumProductionQuantityByMachineAndDateRange(@Param("machineCode") String machineCode, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Rejection analysis
    @Query("SELECT SUM(pm.rejectionInprocessQuantity + pm.rejectionFinalstageQuantity) FROM ProductionMaster pm WHERE pm.entryDate = :date")
    Long sumRejectionQuantityByDate(@Param("date") LocalDate date);

    @Query("SELECT pm FROM ProductionMaster pm WHERE (pm.rejectionInprocessQuantity + pm.rejectionFinalstageQuantity) > 0 AND pm.entryDate BETWEEN :startDate AND :endDate")
    List<ProductionMaster> findRecordsWithRejections(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Loss time analysis
    @Query("SELECT pm FROM ProductionMaster pm WHERE " +
            "(pm.lossTime1Minutes + pm.lossTime2Minutes + pm.lossTime3Minutes + pm.lossTime4Minutes + " +
            "pm.lossTime5Minutes + pm.lossTime6Minutes + pm.lossTime7Minutes + pm.lossTime8Minutes) > :threshold " +
            "AND pm.entryDate = :date")
    List<ProductionMaster> findHighLossTimeRecords(@Param("threshold") Integer threshold, @Param("date") LocalDate date);

    // Dashboard queries
    @Query("SELECT AVG(pm.operatorEfficiencyPercentage) FROM ProductionMaster pm WHERE pm.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateAverageOperatorEfficiency(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(pm.equipmentEfficiencyPercentage) FROM ProductionMaster pm WHERE pm.entryDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateAverageEquipmentEfficiency(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Complex reporting queries
    @Query("SELECT pm.machineCode, COUNT(pm), SUM(pm.productionQuantity), AVG(pm.operatorEfficiencyPercentage) " +
            "FROM ProductionMaster pm WHERE pm.entryDate BETWEEN :startDate AND :endDate " +
            "GROUP BY pm.machineCode ORDER BY pm.machineCode")
    List<Object[]> getMachineProductionSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT pm.shift, COUNT(pm), SUM(pm.productionQuantity), AVG(pm.equipmentEfficiencyPercentage) " +
            "FROM ProductionMaster pm WHERE pm.entryDate BETWEEN :startDate AND :endDate " +
            "GROUP BY pm.shift ORDER BY pm.shift")
    List<Object[]> getShiftProductionSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Search functionality
    @Query("SELECT pm FROM ProductionMaster pm WHERE " +
            "LOWER(pm.machineCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.partNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.operatorName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pm.employeeNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ProductionMaster> searchProductionRecords(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Data integrity checks
    @Query("SELECT pm FROM ProductionMaster pm WHERE pm.startDateTime IS NOT NULL AND pm.stopDateTime IS NOT NULL AND pm.startDateTime > pm.stopDateTime")
    List<ProductionMaster> findInvalidTimeRecords();

    @Query("SELECT pm FROM ProductionMaster pm WHERE pm.productionQuantity < 0 OR pm.rejectionInprocessQuantity < 0 OR pm.rejectionFinalstageQuantity < 0")
    List<ProductionMaster> findInvalidQuantityRecords();
}