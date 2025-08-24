package com.protomil.core.jobcard.domain.enums;

public enum JobStatus {
    DRAFT("Draft", "Job card is being created", "#FFC107", 0),
    READY("Ready", "Job card is ready for assignment", "#17A2B8", 1),
    ASSIGNED("Assigned", "Job card has been assigned to personnel", "#007BFF", 2),
    IN_PROGRESS("In Progress", "Work is being performed", "#FD7E14", 3),
    PENDING_REVIEW("Pending Review", "Work completed, awaiting approval", "#6610F2", 4),
    COMPLETED("Completed", "Job card successfully finished", "#28A745", 5),
    CANCELLED("Cancelled", "Job card was cancelled", "#6C757D", 6),
    REWORK_REQUIRED("Rework Required", "Quality issues require rework", "#DC3545", 7);

    private final String displayName;
    private final String description;
    private final String colorCode;
    private final int order;

    JobStatus(String displayName, String description, String colorCode, int order) {
        this.displayName = displayName;
        this.description = description;
        this.colorCode = colorCode;
        this.order = order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getColorCode() {
        return colorCode;
    }

    public int getOrder() {
        return order;
    }

    public boolean isActiveStatus() {
        return this == ASSIGNED || this == IN_PROGRESS || this == PENDING_REVIEW;
    }

    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isWorkInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean canTransitionTo(JobStatus targetStatus) {
        return switch (this) {
            case DRAFT -> targetStatus == READY || targetStatus == CANCELLED;
            case READY -> targetStatus == ASSIGNED || targetStatus == CANCELLED;
            case ASSIGNED -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == PENDING_REVIEW || targetStatus == CANCELLED;
            case PENDING_REVIEW -> targetStatus == COMPLETED || targetStatus == REWORK_REQUIRED;
            case REWORK_REQUIRED -> targetStatus == IN_PROGRESS || targetStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false; // Final states
        };
    }

    public static JobStatus[] getActiveStatuses() {
        return new JobStatus[]{ASSIGNED, IN_PROGRESS, PENDING_REVIEW};
    }

    public static JobStatus[] getFinalStatuses() {
        return new JobStatus[]{COMPLETED, CANCELLED};
    }
}