package com.protomil.core.jobcard.domain.enums;

public enum FieldType {
    TEXT("Text Input", "text", "Single line text input", false),
    NUMBER("Number Input", "number", "Numeric input with validation", false),
    DATE("Date Picker", "date", "Date selection widget", false),
    DATETIME("Date Time Picker", "datetime-local", "Date and time selection", false),
    DROPDOWN("Dropdown Selection", "select", "Single selection from predefined options", true),
    CHECKBOX("Checkbox", "checkbox", "Multiple selection from predefined options", true),
    TEXTAREA("Text Area", "textarea", "Multi-line text input", false),
    FILE("File Upload", "file", "File upload widget", false),
    EMAIL("Email Input", "email", "Email address with validation", false),
    PHONE("Phone Input", "tel", "Phone number input", false),
    URL("URL Input", "url", "URL input with validation", false),
    CURRENCY("Currency Input", "number", "Currency input with formatting", false);

    private final String displayName;
    private final String htmlType;
    private final String description;
    private final boolean requiresOptions;

    FieldType(String displayName, String htmlType, String description, boolean requiresOptions) {
        this.displayName = displayName;
        this.htmlType = htmlType;
        this.description = description;
        this.requiresOptions = requiresOptions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHtmlType() {
        return htmlType;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresOptions() {
        return requiresOptions;
    }

    public boolean supportsValidation() {
        return this != FILE && this != CHECKBOX;
    }

    public boolean isNumericType() {
        return this == NUMBER || this == CURRENCY;
    }

    public boolean isDateType() {
        return this == DATE || this == DATETIME;
    }

    public boolean isTextType() {
        return this == TEXT || this == TEXTAREA || this == EMAIL || this == PHONE || this == URL;
    }

    public String getDefaultValidationPattern() {
        return switch (this) {
            case EMAIL -> "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
            case PHONE -> "^\\+?[1-9]\\d{1,14}$";
            case URL -> "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
            default -> null;
        };
    }
}