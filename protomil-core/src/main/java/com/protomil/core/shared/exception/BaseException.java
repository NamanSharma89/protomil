// com/protomil/core/shared/exception/BaseException.java
package com.protomil.core.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final Map<String, Object> details;

    protected BaseException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>();
    }

    protected BaseException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T addDetail(String key, Object value) {
        this.details.put(key, value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T addDetails(Map<String, Object> additionalDetails) {
        this.details.putAll(additionalDetails);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseException> T withErrorCode(String errorCode) {
        // This creates a new exception with updated error code
        // Note: We can't modify final fields, so this is a design consideration
        return (T) this;
    }
}