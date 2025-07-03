// com/protomil/core/shared/dto/ApiResponse.java
package com.protomil.core.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Response timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Trace ID for request tracking", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    @Schema(description = "Error details (only present when success = false)")
    private ErrorResponse error;

    // Static factory methods for success responses
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Static factory methods for error responses
    public static <T> ApiResponse<T> error(String message, ErrorResponse error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(error.getMessage())
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
}