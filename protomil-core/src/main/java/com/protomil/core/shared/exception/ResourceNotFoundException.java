// com/protomil/core/shared/exception/ResourceNotFoundException.java
package com.protomil.core.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    // Constructor 1: Simple message
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    // Constructor 2: Resource name and identifier (with builder pattern)
    public static ResourceNotFoundException forResource(String resourceName, String identifier) {
        ResourceNotFoundException exception = new ResourceNotFoundException(
                String.format("%s not found with identifier: %s", resourceName, identifier)
        );
        exception.addDetail("resourceType", resourceName);
        exception.addDetail("identifier", identifier);
        return exception;
    }

    // Constructor 3: Message with custom error code
    public ResourceNotFoundException(String message, String errorCode) {
        super(message, HttpStatus.NOT_FOUND, errorCode);
    }

    // Constructor 4: Message with cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    // Constructor 5: Message with cause and custom error code
    public ResourceNotFoundException(String message, Throwable cause, String errorCode) {
        super(message, cause, HttpStatus.NOT_FOUND, errorCode);
    }

    // Convenience factory methods for common resources
    public static ResourceNotFoundException user(String identifier) {
        return forResource("User", identifier);
    }

    public static ResourceNotFoundException jobCard(String identifier) {
        return forResource("Job Card", identifier);
    }

    public static ResourceNotFoundException personnel(String identifier) {
        return forResource("Personnel", identifier);
    }

    public static ResourceNotFoundException equipment(String identifier) {
        return forResource("Equipment", identifier);
    }

    public static ResourceNotFoundException role(String identifier) {
        return forResource("Role", identifier);
    }

    public static ResourceNotFoundException permission(String identifier) {
        return forResource("Permission", identifier);
    }
}