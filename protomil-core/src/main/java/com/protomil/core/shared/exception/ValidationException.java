// com/protomil/core/shared/exception/ValidationException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        addDetail("fieldErrors", fieldErrors);
    }

    public ValidationException(String message, String field, String fieldError) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        addDetail("field", field);
        addDetail("fieldError", fieldError);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
}