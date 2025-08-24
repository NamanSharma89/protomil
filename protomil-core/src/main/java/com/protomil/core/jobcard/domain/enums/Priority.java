package com.protomil.core.jobcard.domain.enums;

import lombok.Getter;

@Getter
public enum Priority {
    LOW("Low Priority"),
    MEDIUM("Medium Priority"),
    HIGH("High Priority"),
    CRITICAL("Critical Priority");

    private final String description;

    Priority(String description) {
        this.description = description;
    }

    public int getWeight() {
        return switch (this) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }
}