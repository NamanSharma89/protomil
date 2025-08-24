package com.protomil.core.jobcard.domain.enums;

public enum Shift {
    MORNING("Morning Shift - 6 AM to 2 PM"),
    AFTERNOON("Afternoon Shift - 2 PM to 10 PM"),
    NIGHT("Night Shift - 10 PM to 6 AM");

    private final String description;

    Shift(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}