package com.protomil.core.shared.error.dto;

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
public class HtmxErrorResponse {
    private String type;
    private String message;
    private String details;
    private Map<String, List<String>> fieldErrors;
    private String path;
    private LocalDateTime timestamp;
    private String traceId;
    private boolean canRetry;
    private Map<String, Object> context;
    private List<String> suggestions;
}