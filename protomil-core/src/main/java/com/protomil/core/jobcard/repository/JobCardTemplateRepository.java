package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.JobCardTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobCardTemplateRepository extends JpaRepository<JobCardTemplate, Long> {

    // Basic finders
    Optional<JobCardTemplate> findByTemplateCode(String templateCode);

    boolean existsByTemplateCode(String templateCode);

    // Active templates
    List<JobCardTemplate> findByIsActiveTrue();

    Page<JobCardTemplate> findByIsActiveTrue(Pageable pageable);

    // Category-based queries
    List<JobCardTemplate> findByCategory(String category);

    List<JobCardTemplate> findByCategoryAndIsActiveTrue(String category);

    // Version queries
    List<JobCardTemplate> findByTemplateNameOrderByVersionDesc(String templateName);

    @Query("SELECT jct FROM JobCardTemplate jct WHERE jct.templateName = :templateName AND jct.isActive = true ORDER BY jct.version DESC")
    Optional<JobCardTemplate> findLatestActiveVersionByTemplateName(@Param("templateName") String templateName);

    // Search functionality
    @Query("SELECT jct FROM JobCardTemplate jct WHERE " +
            "LOWER(jct.templateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(jct.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(jct.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<JobCardTemplate> searchTemplates(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Statistics
    @Query("SELECT COUNT(jct) FROM JobCardTemplate jct WHERE jct.category = :category AND jct.isActive = true")
    Long countActiveByCategoryName(@Param("category") String category);

    // Complex queries with field definitions
    @Query("SELECT jct FROM JobCardTemplate jct " +
            "LEFT JOIN FETCH jct.fieldDefinitions fd " +
            "WHERE jct.id = :id AND jct.isActive = true " +
            "ORDER BY fd.displayOrder")
    Optional<JobCardTemplate> findByIdWithFieldDefinitions(@Param("id") Long id);

    @Query("SELECT DISTINCT jct.category FROM JobCardTemplate jct WHERE jct.isActive = true ORDER BY jct.category")
    List<String> findDistinctActiveCategories();
}