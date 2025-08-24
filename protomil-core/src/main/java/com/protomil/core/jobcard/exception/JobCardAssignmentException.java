package com.protomil.core.jobcard.exception;

import com.protomil.core.shared.exception.BusinessException;

public class JobCardAssignmentException extends BusinessException {

    public JobCardAssignmentException(String message) {
        super(message);
    }

    public JobCardAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}