// com/protomil/core/shared/domain/enums/RolePermissionStatus.java
package com.protomil.core.shared.domain.enums;

public enum RolePermissionStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    REVOKED("Revoked");

    private final String description;

    RolePermissionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}