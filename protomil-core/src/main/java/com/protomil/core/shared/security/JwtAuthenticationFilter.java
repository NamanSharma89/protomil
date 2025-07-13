// src/main/java/com/protomil/core/shared/security/JwtAuthenticationFilter.java
package com.protomil.core.shared.security;

import com.protomil.core.shared.exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;
    private final CookieManager cookieManager;
    private final SessionManager sessionManager;

    public JwtAuthenticationFilter(JwtTokenManager jwtTokenManager,
                                   CookieManager cookieManager,
                                   SessionManager sessionManager) {
        this.jwtTokenManager = jwtTokenManager;
        this.cookieManager = cookieManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("Processing authentication for request: {}", requestURI);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI)) {
            log.debug("Skipping authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Attempt to authenticate user from cookies
            Optional<UserTokenClaims> userClaims = authenticateFromCookies(request, response);

            if (userClaims.isPresent()) {
                // Set user context for the request
                setUserContext(request, userClaims.get());
                log.debug("User authenticated successfully: {}", userClaims.get().getUserId());
            } else if (isProtectedEndpoint(requestURI)) {
                // Redirect to login for protected endpoints
                log.debug("Authentication required for protected endpoint: {}", requestURI);
                redirectToLogin(request, response);
                return;
            }

            filterChain.doFilter(request, response);

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for request: {} - {}", requestURI, e.getMessage());
            handleAuthenticationFailure(request, response, e);

        } catch (Exception e) {
            log.error("Unexpected error during authentication for request: {}", requestURI, e);
            handleAuthenticationFailure(request, response,
                    new AuthenticationException("Authentication system error"));
        } finally {
            // Clear user context
            clearUserContext();
        }
    }

    private Optional<UserTokenClaims> authenticateFromCookies(HttpServletRequest request,
                                                              HttpServletResponse response) {
        // Get access token from cookie
        Optional<String> accessToken = cookieManager.getAccessToken(request);

        if (accessToken.isEmpty()) {
            log.debug("No access token found in cookies");
            return Optional.empty();
        }

        try {
            // Validate access token
            if (!jwtTokenManager.isTokenExpired(accessToken.get())) {
                UserTokenClaims userClaims = jwtTokenManager.extractUserClaims(accessToken.get());
                log.debug("Access token is valid for user: {}", userClaims.getUserId());
                return Optional.of(userClaims);
            }

            log.debug("Access token is expired, attempting refresh");

            // Access token expired, try to refresh
            return refreshTokenIfPossible(request, response);

        } catch (AuthenticationException e) {
            log.debug("Access token validation failed: {}", e.getMessage());
            return refreshTokenIfPossible(request, response);
        }
    }

    private Optional<UserTokenClaims> refreshTokenIfPossible(HttpServletRequest request,
                                                             HttpServletResponse response) {
        Optional<String> refreshToken = cookieManager.getRefreshToken(request);

        if (refreshToken.isEmpty()) {
            log.debug("No refresh token available");
            return Optional.empty();
        }

        try {
            // Validate refresh token
            if (jwtTokenManager.isTokenExpired(refreshToken.get()) ||
                    !jwtTokenManager.isRefreshToken(refreshToken.get())) {
                log.debug("Refresh token is expired or invalid");
                cookieManager.clearAuthenticationCookies(response);
                return Optional.empty();
            }

            // Extract user info from refresh token
            UserTokenClaims refreshClaims = jwtTokenManager.extractUserClaims(refreshToken.get());
            log.debug("Refresh token is valid, generating new access token for user: {}",
                    refreshClaims.getUserId());

            // Generate new access token with current user data
            UserTokenClaims currentUserClaims = sessionManager.getCurrentUserClaims(refreshClaims.getUserId());
            String newAccessToken = jwtTokenManager.generateAccessToken(currentUserClaims);

            // Update access token cookie
            cookieManager.updateAccessTokenCookie(response, newAccessToken,
                    jwtTokenManager.accessTokenExpiration);

            log.debug("Access token refreshed successfully for user: {}", currentUserClaims.getUserId());

            return Optional.of(currentUserClaims);

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            cookieManager.clearAuthenticationCookies(response);
            return Optional.empty();
        }
    }

    private void setUserContext(HttpServletRequest request, UserTokenClaims userClaims) {
        // Store user context in request attributes for use in controllers
        request.setAttribute("authenticatedUser", userClaims);
        request.setAttribute("userId", userClaims.getUserId());
        request.setAttribute("userRoles", userClaims.getRoles());

        // Set MDC for logging
        org.slf4j.MDC.put("userId", userClaims.getUserId().toString());
        org.slf4j.MDC.put("userEmail", userClaims.getEmail());
    }

    private void clearUserContext() {
        org.slf4j.MDC.remove("userId");
        org.slf4j.MDC.remove("userEmail");
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/wireframes/login") ||
                requestURI.startsWith("/wireframes/register") ||
                requestURI.startsWith("/wireframes/verify-email") ||
                requestURI.startsWith("/wireframes/forgot-password") ||
                requestURI.equals("/wireframes/") ||
                requestURI.startsWith("/api/v1/auth/") ||
                requestURI.startsWith("/actuator/health") ||
                requestURI.startsWith("/actuator/info") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/images/") ||
                requestURI.startsWith("/favicon") ||
                requestURI.startsWith("/error");
    }

    private boolean isProtectedEndpoint(String requestURI) {
        return requestURI.startsWith("/wireframes/dashboard") ||
                requestURI.startsWith("/wireframes/profile") ||
                requestURI.startsWith("/api/v1/users/") ||
                requestURI.startsWith("/api/v1/jobcards/") ||
                requestURI.startsWith("/api/v1/equipment/");
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String redirectUrl = requestURI + (queryString != null ? "?" + queryString : "");

        response.sendRedirect("/wireframes/login?redirect=" +
                java.net.URLEncoder.encode(redirectUrl, "UTF-8"));
    }

    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                             AuthenticationException e) throws IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/")) {
            // API endpoints - return JSON error
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication failed\",\"message\":\"" +
                    e.getMessage() + "\"}");
        } else {
            // UI endpoints - redirect to login
            redirectToLogin(request, response);
        }
    }
}