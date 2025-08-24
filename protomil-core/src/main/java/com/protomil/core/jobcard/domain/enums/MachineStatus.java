package com.protomil.core.jobcard.domain.enums;

public enum MachineStatus {
    ACTIVE("Active", "Machine is operational and available", "#28A745"),
    MAINTENANCE("Maintenance", "Machine is under maintenance", "#FFC107"),
    BREAKDOWN("Breakdown", "Machine is not working due to breakdown", "#DC3545"),
    INACTIVE("Inactive", "Machine is temporarily inactive", "#6C757D");

    private final String displayName;
    private final String description;
    private final String colorCode;

    MachineStatus(String displayName, String description, String colorCode) {
        this.displayName = displayName;
        this.description = description;
        this.colorCode = colorCode;
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

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public boolean requiresAttention() {
        return this == BREAKDOWN || this == MAINTENANCE;
    }
}