// com/protomil/core/shared/exception/AuthenticationException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }

    public AuthenticationException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }
}