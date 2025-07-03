// com/protomil/core/shared/exception/ConcurrencyException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

public class ConcurrencyException extends BaseException {

    public ConcurrencyException(String message) {
        super(message, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION");
    }

    public ConcurrencyException(String resourceType, String resourceId) {
        super(String.format("Resource %s with ID %s was modified by another user", resourceType, resourceId),
                HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION");
        addDetail("resourceType", resourceType);
        addDetail("resourceId", resourceId);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION");
    }
}