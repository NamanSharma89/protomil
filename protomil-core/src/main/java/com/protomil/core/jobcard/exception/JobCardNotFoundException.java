package com.protomil.core.jobcard.exception;

import com.protomil.core.shared.exception.BusinessException;

public class JobCardNotFoundException extends BusinessException {

    public JobCardNotFoundException(String message) {
        super(message);
    }

    public JobCardNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}