package com.protomil.core.jobcard.domain.enums;

public enum CommentType {
    GENERAL("General Comment", "#6C757D", "General purpose comment"),
    QUALITY("Quality Related", "#DC3545", "Quality control or inspection comment"),
    ISSUE("Issue/Problem", "#FFC107", "Problem or issue reported"),
    INSTRUCTION("Instruction", "#17A2B8", "Additional work instruction or clarification"),
    APPROVAL("Approval", "#28A745", "Approval or rejection comment"),
    PROGRESS("Progress Update", "#007BFF", "Progress or status update");

    private final String displayName;
    private final String colorCode;
    private final String description;

    CommentType(String displayName, String colorCode, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHighPriority() {
        return this == QUALITY || this == ISSUE;
    }

    public boolean requiresAttention() {
        return this == ISSUE || this == QUALITY;
    }
}