// com/protomil/core/shared/exception/ExternalServiceException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR");
    }

    public ExternalServiceException(String message, String serviceName) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR");
        addDetail("serviceName", serviceName);
    }

    public ExternalServiceException(String message, String serviceName, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR");
        addDetail("serviceName", serviceName);
    }

    public ExternalServiceException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}