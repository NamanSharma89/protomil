package com.protomil.core.shared.error.service;

import com.protomil.core.shared.error.dto.ErrorDetails;
import com.protomil.core.shared.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ErrorLoggingService {

    public void logError(ErrorDetails errorDetails, HttpServletRequest request) {
        setMDCContext(errorDetails, request);

        if (errorDetails.getStatusCode() >= 500) {
            log.error("Server error occurred - Status: {}, Path: {}, Message: {}",
                    errorDetails.getStatusCode(), errorDetails.getPath(), errorDetails.getMessage());
        } else if (errorDetails.getStatusCode() >= 400) {
            log.warn("Client error occurred - Status: {}, Path: {}, Message: {}",
                    errorDetails.getStatusCode(), errorDetails.getPath(), errorDetails.getMessage());
        }

        clearMDC();
    }

    public void logValidationError(Exception ex, HttpServletRequest request) {
        setMDCContext(request);
        log.warn("Validation error in HTMX request - Path: {}, Error: {}",
                request.getRequestURI(), ex.getMessage());
        clearMDC();
    }

    public void logBindingError(Exception ex, HttpServletRequest request) {
        setMDCContext(request);
        log.warn("Binding error in HTMX request - Path: {}, Error: {}",
                request.getRequestURI(), ex.getMessage());
        clearMDC();
    }

    public void logBusinessError(BaseException ex, HttpServletRequest request) {
        setMDCContext(request);
        log.warn("Business error in HTMX request - Path: {}, Error: {}, Code: {}",
                request.getRequestURI(), ex.getMessage(), ex.getErrorCode());
        clearMDC();
    }

    public void logGenericError(Exception ex, HttpServletRequest request) {
        setMDCContext(request);
        log.error("Unexpected error in HTMX request - Path: {}",
                request.getRequestURI(), ex);
        clearMDC();
    }

    private void setMDCContext(ErrorDetails errorDetails, HttpServletRequest request) {
        MDC.put("traceId", errorDetails.getTraceId());
        MDC.put("statusCode", String.valueOf(errorDetails.getStatusCode()));
        MDC.put("path", errorDetails.getPath());
        setMDCContext(request);
    }

    private void setMDCContext(HttpServletRequest request) {
        MDC.put("method", request.getMethod());
        MDC.put("userAgent", request.getHeader("User-Agent"));
        MDC.put("clientIP", getClientIP(request));

        // Check if it's an HTMX request
        String hxRequest = request.getHeader("HX-Request");
        if ("true".equals(hxRequest)) {
            MDC.put("requestType", "HTMX");
            MDC.put("hxTarget", request.getHeader("HX-Target"));
            MDC.put("hxTrigger", request.getHeader("HX-Trigger"));
        } else {
            MDC.put("requestType", "STANDARD");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void clearMDC() {
        MDC.clear();
    }
}