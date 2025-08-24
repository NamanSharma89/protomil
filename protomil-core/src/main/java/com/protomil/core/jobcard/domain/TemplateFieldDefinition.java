package com.protomil.core.jobcard.domain;

import com.protomil.core.jobcard.domain.enums.FieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "template_field_definitions",
        uniqueConstraints = @UniqueConstraint(name = "unique_field_per_template",
                columnNames = {"template_id", "field_name"}),
        indexes = {
                @Index(name = "idx_template_field_definitions_template", columnList = "template_id"),
                @Index(name = "idx_template_field_definitions_order", columnList = "template_id, display_order")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private JobCardTemplate template;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(name = "field_group", length = 100)
    private String fieldGroup;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_config", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> fieldConfig = Map.of();

    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "fieldDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FieldOption> fieldOptions = new ArrayList<>();
}