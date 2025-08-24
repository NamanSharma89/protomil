package com.protomil.core.jobcard.domain.enums;

import lombok.Getter;

@Getter
public enum InstructionType {
    TEXT("Text Instruction"),
    IMAGE("Image Instruction"),
    VIDEO("Video Instruction"),
    CHECKLIST("Checklist"),
    FORM("Form Input"),
    ATTACHMENT("File Attachment");

    private final String description;

    InstructionType(String description) {
        this.description = description;
    }

}