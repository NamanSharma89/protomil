package com.protomil.core.jobcard.exception;

import com.protomil.core.shared.exception.BusinessException;

public class InvalidJobStatusTransitionException extends BusinessException {

    public InvalidJobStatusTransitionException(String message) {
        super(message);
    }

    public InvalidJobStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}