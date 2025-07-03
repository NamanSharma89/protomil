// com/protomil/core/shared/logging/RequestResponseLoggingFilter.java
package com.protomil.core.shared.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class RequestResponseLoggingFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip logging for health checks and static resources
        if (shouldSkipLogging(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // Generate or extract trace ID
        String traceId = getOrGenerateTraceId(httpRequest);

        // Set up MDC
        MDC.put("traceId", traceId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());
        MDC.put("clientIP", getClientIP(httpRequest));

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        // Add trace ID to response header
        wrappedResponse.setHeader(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logRequest(wrappedRequest);

            // Process the request
            chain.doFilter(wrappedRequest, wrappedResponse);

            // Log outgoing response
            logResponse(wrappedResponse, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error processing request - TraceId: {}, URI: {}", traceId, httpRequest.getRequestURI(), e);
            throw e;
        } finally {
            // Copy cached content to actual response
            wrappedResponse.copyBodyToResponse();
            // Clear MDC
            MDC.clear();
        }
    }

    private boolean shouldSkipLogging(String uri) {
        return uri.startsWith("/actuator/health") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".ico");
    }

    private String getOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        int contentLength = request.getContentLength();

        log.info("Incoming request - Method: {}, URI: {}, ContentType: {}, ContentLength: {}, QueryString: {}",
                request.getMethod(),
                request.getRequestURI(),
                contentType,
                contentLength,
                request.getQueryString());

        // Log request body for non-GET requests (excluding sensitive endpoints)
        if (!"GET".equals(request.getMethod()) && shouldLogRequestBody(request.getRequestURI())) {
            String requestBody = getContentAsString(request.getContentAsByteArray(), contentType);
            if (requestBody != null && !requestBody.isEmpty()) {
                log.debug("Request body: {}", sanitizeRequestBody(requestBody));
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        String contentType = response.getContentType();

        log.info("Outgoing response - Status: {}, ContentType: {}, Duration: {}ms",
                status, contentType, duration);

        // Log response body for error responses
        if (status >= 400) {
            String responseBody = getContentAsString(response.getContentAsByteArray(), contentType);
            if (responseBody != null && !responseBody.isEmpty()) {
                log.debug("Response body: {}", responseBody);
            }
        }
    }

    private boolean shouldLogRequestBody(String uri) {
        // Don't log request body for sensitive endpoints
        return !uri.contains("/login") &&
                !uri.contains("/password") &&
                !uri.contains("/reset") &&
                !uri.contains("/token");
    }

    private String sanitizeRequestBody(String requestBody) {
        // Remove sensitive information from request body
        return requestBody
                .replaceAll("(\"password\"\\s*:\\s*\")[^\"]*(\",?)", "$1***$2")
                .replaceAll("(\"token\"\\s*:\\s*\")[^\"]*(\",?)", "$1***$2")
                .replaceAll("(\"secret\"\\s*:\\s*\")[^\"]*(\",?)", "$1***$2");
    }

    private String getContentAsString(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            return null;
        }

        if (contentType != null && contentType.startsWith("application/json")) {
            return new String(content);
        }

        return "[Binary Content]";
    }
}