// com/protomil/core/shared/exception/GlobalExceptionHandler.java
package com.protomil.core.shared.exception;

import com.protomil.core.shared.dto.ApiResponse;
import com.protomil.core.shared.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Custom Business Exceptions
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        String traceId = getOrGenerateTraceId();

        log.error("Business exception occurred - TraceId: {}, Error: {}, Path: {}",
                traceId, ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .status(ex.getHttpStatus().value())
                .timestamp(ex.getTimestamp())
                .path(request.getRequestURI())
                .traceId(traceId)
                .context(ex.getDetails())
                .suggestions(getSuggestions(ex))
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        Map<String, List<String>> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });

        log.warn("Validation failed - TraceId: {}, Path: {}, Errors: {}",
                traceId, request.getRequestURI(), fieldErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed for the request")
                .details("Please check the field errors and correct the input")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .suggestions(Arrays.asList(
                        "Check all required fields are provided",
                        "Verify data formats match the expected patterns",
                        "Ensure all constraints are satisfied"
                ))
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        }

        log.warn("Constraint violation - TraceId: {}, Path: {}, Errors: {}",
                traceId, request.getRequestURI(), fieldErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("CONSTRAINT_VIOLATION")
                .message("Constraint validation failed")
                .details("One or more constraints were violated")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse));
    }

    // Security Exceptions
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Authentication failed - TraceId: {}, Path: {}, Error: {}",
                traceId, request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("AUTHENTICATION_FAILED")
                .message("Authentication failed")
                .details("Invalid credentials or authentication token")
                .status(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Check your credentials",
                        "Ensure your token is valid and not expired",
                        "Try logging in again"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Access denied - TraceId: {}, Path: {}, User: {}",
                traceId, request.getRequestURI(), getCurrentUser());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ACCESS_DENIED")
                .message("Access denied")
                .details("You don't have permission to access this resource")
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Contact your administrator for access",
                        "Verify your role permissions"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse));
    }

    // Database Exceptions
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.error("Data integrity violation - TraceId: {}, Path: {}",
                traceId, request.getRequestURI(), ex);

        String message = "Data integrity constraint violation";
        String details = "The operation violates database constraints";

        // Try to provide more specific error messages
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique")) {
                message = "Duplicate data found";
                details = "A record with the same value already exists";
            } else if (ex.getMessage().contains("foreign key")) {
                message = "Referenced data not found";
                details = "The operation references data that doesn't exist";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .message(message)
                .details(details)
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Check for duplicate data",
                        "Verify all referenced entities exist",
                        "Review the data constraints"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingException(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Optimistic locking failure - TraceId: {}, Path: {}",
                traceId, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("CONCURRENT_MODIFICATION")
                .message("Resource was modified by another user")
                .details("Please refresh and try again")
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Refresh the page and try again",
                        "Check if someone else is modifying the same data"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse));
    }

    // HTTP Method/Media Type Exceptions
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Method not supported - TraceId: {}, Method: {}, Path: {}",
                traceId, ex.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("METHOD_NOT_SUPPORTED")
                .message("HTTP method not supported")
                .details(String.format("Method '%s' is not supported for this endpoint", ex.getMethod()))
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .context(Map.of("supportedMethods", ex.getSupportedHttpMethods()))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Media type not supported - TraceId: {}, ContentType: {}, Path: {}",
                traceId, ex.getContentType(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("MEDIA_TYPE_NOT_SUPPORTED")
                .message("Media type not supported")
                .details(String.format("Content type '%s' is not supported", ex.getContentType()))
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .context(Map.of("supportedMediaTypes", ex.getSupportedMediaTypes()))
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(errorResponse));
    }

    // Request Parameter Exceptions
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Missing request parameter - TraceId: {}, Parameter: {}, Path: {}",
                traceId, ex.getParameterName(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("MISSING_PARAMETER")
                .message("Required parameter is missing")
                .details(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .context(Map.of("missingParameter", ex.getParameterName()))
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Type mismatch - TraceId: {}, Parameter: {}, Value: {}, Path: {}",
                traceId, ex.getName(), ex.getValue(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("TYPE_MISMATCH")
                .message("Parameter type mismatch")
                .details(String.format("Parameter '%s' should be of type %s",
                        ex.getName(), ex.getRequiredType().getSimpleName()))
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .context(Map.of(
                        "parameter", ex.getName(),
                        "providedValue", ex.getValue(),
                        "expectedType", ex.getRequiredType().getSimpleName()
                ))
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("Message not readable - TraceId: {}, Path: {}",
                traceId, request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("MALFORMED_REQUEST")
                .message("Request body is not readable")
                .details("The request body contains invalid JSON or is malformed")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Check JSON syntax",
                        "Verify Content-Type header",
                        "Ensure request body is properly formatted"
                ))
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.warn("No handler found - TraceId: {}, Method: {}, Path: {}",
                traceId, ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ENDPOINT_NOT_FOUND")
                .message("Endpoint not found")
                .details(String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Check the URL spelling",
                        "Verify the HTTP method",
                        "Refer to API documentation for correct endpoints"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse));
    }

    // Generic Exception Handler (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = getOrGenerateTraceId();

        log.error("Unexpected error occurred - TraceId: {}, Path: {}",
                traceId, request.getRequestURI(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .details("Please try again later or contact support if the problem persists")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .traceId(traceId)
                .suggestions(Arrays.asList(
                        "Try the request again",
                        "Contact support with the trace ID if the error persists"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse));
    }

    // Helper Methods
    private String getOrGenerateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }

    private String getCurrentUser() {
        // Implementation depends on your security context
        return MDC.get("userId") != null ? MDC.get("userId") : "anonymous";
    }

    private List<String> getSuggestions(BaseException ex) {
        // Return context-specific suggestions based on exception type
        if (ex instanceof ResourceNotFoundException) {
            return Arrays.asList(
                    "Verify the resource ID is correct",
                    "Check if the resource exists",
                    "Ensure you have access to this resource"
            );
        } else if (ex instanceof BusinessException) {
            return Arrays.asList(
                    "Review the business rules",
                    "Check the request parameters",
                    "Ensure all prerequisites are met"
            );
        } else if (ex instanceof AuthorizationException) {
            return Arrays.asList(
                    "Contact your administrator for access",
                    "Verify your role permissions"
            );
        }
        return Arrays.asList("Contact support for assistance");
    }
}