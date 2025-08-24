package com.protomil.core.jobcard.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "field_options", indexes = {
        @Index(name = "idx_field_options_field_def", columnList = "field_definition_id"),
        @Index(name = "idx_field_options_order", columnList = "field_definition_id, display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private TemplateFieldDefinition fieldDefinition;

    @Column(name = "option_value", nullable = false, length = 200)
    private String optionValue;

    @Column(name = "option_label", nullable = false, length = 200)
    private String optionLabel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}