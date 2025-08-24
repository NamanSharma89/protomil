package com.protomil.core.jobcard.repository;

import com.protomil.core.jobcard.domain.TemplateFieldDefinition;
import com.protomil.core.jobcard.domain.enums.FieldType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateFieldDefinitionRepository extends JpaRepository<TemplateFieldDefinition, Long> {

    // Template-based queries
    List<TemplateFieldDefinition> findByTemplateIdOrderByDisplayOrder(Long templateId);

    List<TemplateFieldDefinition> findByTemplateIdAndIsRequiredTrueOrderByDisplayOrder(Long templateId);

    // Field name queries
    Optional<TemplateFieldDefinition> findByTemplateIdAndFieldName(Long templateId, String fieldName);

    boolean existsByTemplateIdAndFieldName(Long templateId, String fieldName);

    // Field type queries
    List<TemplateFieldDefinition> findByTemplateIdAndFieldType(Long templateId, FieldType fieldType);

    // Group-based queries
    List<TemplateFieldDefinition> findByTemplateIdAndFieldGroupOrderByDisplayOrder(Long templateId, String fieldGroup);

    @Query("SELECT DISTINCT tfd.fieldGroup FROM TemplateFieldDefinition tfd WHERE tfd.template.id = :templateId AND tfd.fieldGroup IS NOT NULL ORDER BY tfd.fieldGroup")
    List<String> findDistinctFieldGroupsByTemplateId(@Param("templateId") Long templateId);

    // Complex queries with options
    @Query("SELECT tfd FROM TemplateFieldDefinition tfd " +
            "LEFT JOIN FETCH tfd.fieldOptions fo " +
            "WHERE tfd.template.id = :templateId " +
            "ORDER BY tfd.displayOrder, fo.displayOrder")
    List<TemplateFieldDefinition> findByTemplateIdWithOptions(@Param("templateId") Long templateId);

    // Statistics
    @Query("SELECT COUNT(tfd) FROM TemplateFieldDefinition tfd WHERE tfd.template.id = :templateId AND tfd.isRequired = true")
    Long countRequiredFieldsByTemplateId(@Param("templateId") Long templateId);
}