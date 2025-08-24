package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.ProductionLossDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionLossDetailRepository extends JpaRepository<ProductionLossDetail, Long> {

    List<ProductionLossDetail> findByProductionMasterIdOrderByLossSequence(Long productionMasterId);

    List<ProductionLossDetail> findByLossCategoryIdOrderByCreatedAtDesc(Long lossCategoryId);

    @Query("SELECT SUM(pld.lossTimeMinutes) FROM ProductionLossDetail pld WHERE pld.productionMaster.id = :productionMasterId")
    Integer sumLossTimeByProductionMasterId(@Param("productionMasterId") Long productionMasterId);

    @Query("SELECT pld.lossCategory.categoryName, SUM(pld.lossTimeMinutes) " +
            "FROM ProductionLossDetail pld " +
            "WHERE pld.productionMaster.entryDate BETWEEN :startDate AND :endDate " +
            "GROUP BY pld.lossCategory.categoryName " +
            "ORDER BY SUM(pld.lossTimeMinutes) DESC")
    List<Object[]> getLossTimeSummaryByCategory(@Param("startDate") java.time.LocalDate startDate,
                                                @Param("endDate") java.time.LocalDate endDate);
}