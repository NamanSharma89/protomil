package com.protomil.core.shared.domain.enums;

// com/protomil/core/shared/domain/enums/UserStatus.java
public enum UserStatus {
    PENDING_VERIFICATION,
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED,
    REJECTED,           // NEW - Admin rejected
    COGNITO_SYNC_FAILURE // NEW - Sync failed after retries
}

