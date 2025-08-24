package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.LossTimeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LossTimeCategoryRepository extends JpaRepository<LossTimeCategory, Long> {

    Optional<LossTimeCategory> findByCategoryCode(String categoryCode);

    List<LossTimeCategory> findByIsActiveTrueOrderByCategoryName();

    boolean existsByCategoryCode(String categoryCode);
}