// com/protomil/core/shared/exception/ExceptionUtils.java
package com.protomil.core.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public final class ExceptionUtils {

    private ExceptionUtils() {
        // Utility class - prevent instantiation
    }

    // Updated resource not found helpers using factory methods
    public static ResourceNotFoundException userNotFound(String identifier) {
        return ResourceNotFoundException.user(identifier)
                .addDetail("searchCriteria", identifier)
                .addDetail("searchType", "identifier");
    }

    public static ResourceNotFoundException userNotFoundByEmail(String email) {
        return ResourceNotFoundException.user(email)
                .addDetail("searchCriteria", email)
                .addDetail("searchType", "email");
    }

    public static ResourceNotFoundException userNotFoundByCognitoSub(String cognitoSub) {
        return ResourceNotFoundException.user(cognitoSub)
                .addDetail("searchCriteria", cognitoSub)
                .addDetail("searchType", "cognitoSub");
    }

    public static ResourceNotFoundException jobCardNotFound(String identifier) {
        return ResourceNotFoundException.jobCard(identifier)
                .addDetail("searchCriteria", identifier);
    }

    public static ResourceNotFoundException personnelNotFound(String identifier) {
        return ResourceNotFoundException.personnel(identifier)
                .addDetail("searchCriteria", identifier);
    }

    public static ResourceNotFoundException equipmentNotFound(String identifier) {
        return ResourceNotFoundException.equipment(identifier)
                .addDetail("searchCriteria", identifier);
    }

    public static ResourceNotFoundException roleNotFound(String identifier) {
        return ResourceNotFoundException.role(identifier)
                .addDetail("searchCriteria", identifier);
    }

    public static ResourceNotFoundException customResourceNotFound(String resourceType, String identifier, String errorCode) {
        return new ResourceNotFoundException(
                String.format("%s not found with identifier: %s", resourceType, identifier),
                errorCode
        )
                .addDetail("resourceType", resourceType)
                .addDetail("identifier", identifier);
    }

    // Business rule violation helpers
    public static BusinessException invalidUserStatus(String currentStatus, String requiredStatus) {
        return new BusinessException(
                String.format("User status is '%s' but operation requires '%s'", currentStatus, requiredStatus),
                "INVALID_USER_STATUS"
        )
                .addDetail("currentStatus", currentStatus)
                .addDetail("requiredStatus", requiredStatus);
    }

    public static BusinessException jobCardAlreadyAssigned(String jobCardId, String assigneeId) {
        return new BusinessException(
                String.format("Job card %s is already assigned to %s", jobCardId, assigneeId),
                "JOB_CARD_ALREADY_ASSIGNED"
        )
                .addDetail("jobCardId", jobCardId)
                .addDetail("currentAssignee", assigneeId);
    }

    public static BusinessException insufficientPermissions(String operation, String resource) {
        return new BusinessException(
                String.format("Insufficient permissions to %s %s", operation, resource),
                "INSUFFICIENT_PERMISSIONS"
        )
                .addDetail("operation", operation)
                .addDetail("resource", resource);
    }

    // External service error helpers
    public static ExternalServiceException awsServiceError(String serviceName, String operation, Throwable cause) {
        return new ExternalServiceException(
                String.format("AWS %s service error during %s", serviceName, operation),
                serviceName,
                cause
        )
                .addDetail("operation", operation)
                .addDetail("provider", "AWS");
    }

    public static ExternalServiceException cognitoError(String operation, Throwable cause) {
        return new ExternalServiceException(
                String.format("Cognito error during %s", operation),
                "AWS Cognito",
                cause
        )
                .addDetail("operation", operation)
                .addDetail("service", "cognito");
    }

    // Validation error helpers
    public static ValidationException requiredFieldMissing(String fieldName) {
        return new ValidationException(
                String.format("Required field '%s' is missing", fieldName),
                fieldName,
                "Field is required"
        );
    }

    public static ValidationException invalidFieldValue(String fieldName, String value, String expectedFormat) {
        return new ValidationException(
                String.format("Invalid value '%s' for field '%s'", value, fieldName),
                fieldName,
                String.format("Expected format: %s", expectedFormat)
        )
                .addDetail("providedValue", value)
                .addDetail("expectedFormat", expectedFormat);
    }

    // Concurrency error helpers
    public static ConcurrencyException optimisticLockFailure(String resourceType, String resourceId) {
        return new ConcurrencyException(resourceType, resourceId)
                .addDetail("conflictType", "optimistic_lock")
                .addDetail("suggestion", "Refresh the resource and try again");
    }

    // Safe execution helpers
    public static <T> T executeWithExceptionHandling(Supplier<T> operation, String operationName) {
        try {
            return operation.get();
        } catch (Exception e) {
            log.error("Error executing operation: {}", operationName, e);
            throw new BusinessException(
                    String.format("Failed to execute operation: %s", operationName),
                    "OPERATION_FAILED"
            ).addDetail("operation", operationName);
        }
    }

    public static void executeWithExceptionHandling(Runnable operation, String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("Error executing operation: {}", operationName, e);
            throw new BusinessException(
                    String.format("Failed to execute operation: %s", operationName),
                    "OPERATION_FAILED"
            ).addDetail("operation", operationName);
        }
    }

    // Error context builders
    public static Map<String, Object> buildErrorContext(String... keyValuePairs) {
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            context.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return context;
    }

    // HTTP Status to Error Code mapping
    public static String getErrorCodeForHttpStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "HTTP_400";
            case UNAUTHORIZED -> "HTTP_401";
            case FORBIDDEN -> "HTTP_403";
            case NOT_FOUND -> "HTTP_404";
            case METHOD_NOT_ALLOWED -> "HTTP_405";
            case CONFLICT -> "HTTP_409";
            case UNSUPPORTED_MEDIA_TYPE -> "HTTP_415";
            case INTERNAL_SERVER_ERROR -> "HTTP_500";
            case SERVICE_UNAVAILABLE -> "HTTP_503";
            default -> "HTTP_" + status.value();
        };
    }
}