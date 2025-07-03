// com/protomil/core/shared/dto/ErrorResponse.java
package com.protomil.core.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error response structure")
public class ErrorResponse {

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Error message", example = "Validation failed for the request")
    private String message;

    @Schema(description = "Detailed error description", example = "The email field is required and must be a valid email address")
    private String details;

    @Schema(description = "HTTP status code", example = "400")
    private Integer status;

    @Schema(description = "Error timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Request path that caused the error", example = "/api/v1/users/register")
    private String path;

    @Schema(description = "Trace ID for debugging", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    @Schema(description = "Field-specific validation errors")
    private Map<String, List<String>> fieldErrors;

    @Schema(description = "Additional error context")
    private Map<String, Object> context;

    @Schema(description = "Suggested actions to resolve the error")
    private List<String> suggestions;
}