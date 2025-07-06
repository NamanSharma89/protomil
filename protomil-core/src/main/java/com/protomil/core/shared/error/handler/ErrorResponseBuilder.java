package com.protomil.core.shared.error.handler;

import com.protomil.core.shared.error.dto.ErrorDetails;
import com.protomil.core.shared.error.dto.HtmxErrorResponse;
import com.protomil.core.shared.exception.BaseException;
import com.protomil.core.shared.exception.BusinessException;
import com.protomil.core.shared.exception.ValidationException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ErrorResponseBuilder {

    public ErrorDetails buildFromRequest(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        return ErrorDetails.builder()
                .statusCode(statusCode != null ? statusCode : 500)
                .message(getMessageForStatusCode(statusCode, errorMessage))
                .path(requestUri != null ? requestUri : request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .exception(exception != null ? exception.getClass().getSimpleName() : "Unknown")
                .build();
    }

    public HtmxErrorResponse buildHtmxValidationError(ValidationException ex, HttpServletRequest request) {
        return HtmxErrorResponse.builder()
                .type("VALIDATION_ERROR")
                .message("Please check your input and try again")
                .details(ex.getMessage())
                .fieldErrors(extractFieldErrors(ex))
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .canRetry(true)
                .build();
    }

    public HtmxErrorResponse buildHtmxBindingError(Exception ex, HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new HashMap<>();

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            fieldErrors = extractFieldErrorsFromValidation(validationEx);
        } else if (ex instanceof BindException bindEx) {
            fieldErrors = extractFieldErrorsFromBinding(bindEx);
        }

        return HtmxErrorResponse.builder()
                .type("VALIDATION_ERROR")
                .message("Please correct the highlighted fields")
                .details("Form validation failed")
                .fieldErrors(fieldErrors)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .canRetry(true)
                .build();
    }

    public HtmxErrorResponse buildHtmxBusinessError(BusinessException ex, HttpServletRequest request) {
        return HtmxErrorResponse.builder()
                .type("BUSINESS_ERROR")
                .message(ex.getMessage())
                .details("A business rule was violated")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .canRetry(false)
                .context(ex.getDetails())
                .build();
    }

    public HtmxErrorResponse buildHtmxGenericError(Exception ex, HttpServletRequest request) {
        return HtmxErrorResponse.builder()
                .type("SYSTEM_ERROR")
                .message("An unexpected error occurred. Please try again.")
                .details("System error")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .canRetry(true)
                .build();
    }

    private String getMessageForStatusCode(Integer statusCode, String errorMessage) {
        if (statusCode == null) return "An unexpected error occurred";

        return switch (statusCode) {
            case 400 -> "Bad Request - Please check your input";
            case 401 -> "Unauthorized - Please login";
            case 403 -> "Access Denied - You don't have permission";
            case 404 -> "Page Not Found";
            case 500 -> "Internal Server Error";
            default -> errorMessage != null ? errorMessage : "An error occurred";
        };
    }

    private Map<String, List<String>> extractFieldErrors(ValidationException ex) {
        // Extract field errors from ValidationException details
        return (Map<String, List<String>>) ex.getDetails().getOrDefault("fieldErrors", new HashMap<>());
    }

    private Map<String, List<String>> extractFieldErrorsFromValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return fieldErrors;
    }

    private Map<String, List<String>> extractFieldErrorsFromBinding(BindException ex) {
        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return fieldErrors;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}