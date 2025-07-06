package com.protomil.core.shared.error.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetails {
    private int statusCode;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private String traceId;
    private String exception;
    private String userFriendlyMessage;

    public String getUserFriendlyMessage() {
        if (userFriendlyMessage != null) {
            return userFriendlyMessage;
        }

        return switch (statusCode) {
            case 404 -> "The page you're looking for doesn't exist.";
            case 403 -> "You don't have permission to access this resource.";
            case 500 -> "Something went wrong on our end. Please try again later.";
            default -> "An unexpected error occurred.";
        };
    }
}