package com.protomil.core.jobcard.exception;

import com.protomil.core.shared.exception.BusinessException;

public class TemplateNotFoundException extends BusinessException {

    public TemplateNotFoundException(String message) {
        super(message);
    }

    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}