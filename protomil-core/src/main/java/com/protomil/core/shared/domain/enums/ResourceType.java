// com/protomil/core/shared/domain/enums/ResourceType.java
package com.protomil.core.shared.domain.enums;

public enum ResourceType {
    JOB_CARD("Job Card"),
    PERSONNEL("Personnel"),
    EQUIPMENT("Equipment"),
    WORKFLOW("Workflow"),
    REPORTING("Reporting"),
    USER_MANAGEMENT("User Management"),
    SYSTEM_ADMIN("System Administration");

    private final String description;

    ResourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}