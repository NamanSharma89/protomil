// com/protomil/core/shared/exception/AuthorizationException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends BaseException {

    public AuthorizationException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }

    public AuthorizationException(String message, String resource) {
        super(message, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        addDetail("resource", resource);
    }

    public AuthorizationException(String message, String errorCode, String resource) {
        super(message, HttpStatus.FORBIDDEN, errorCode);
        addDetail("resource", resource);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }
}