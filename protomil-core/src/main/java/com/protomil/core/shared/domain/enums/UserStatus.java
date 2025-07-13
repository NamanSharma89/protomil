// src/main/java/com/protomil/core/shared/domain/enums/UserStatus.java (Updated)
package com.protomil.core.shared.domain.enums;

public enum UserStatus {
    PENDING_VERIFICATION("Pending Email Verification"),
    PENDING_APPROVAL("Pending Administrator Approval"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    DELETED("Deleted"),
    REJECTED("Rejected by Administrator"),
    COGNITO_SYNC_FAILURE("Cognito Synchronization Failed");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isLoginAllowed() {
        return this == ACTIVE;
    }

    public boolean requiresAdminIntervention() {
        return this == COGNITO_SYNC_FAILURE || this == SUSPENDED;
    }

    public boolean isPending() {
        return this == PENDING_VERIFICATION || this == PENDING_APPROVAL;
    }
}