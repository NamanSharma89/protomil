// com/protomil/core/shared/domain/enums/PermissionStatus.java
package com.protomil.core.shared.domain.enums;

public enum PermissionStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    DEPRECATED("Deprecated");

    private final String description;

    PermissionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}