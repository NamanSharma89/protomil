// src/main/java/com/protomil/core/shared/security/CookieManager.java
package com.protomil.core.shared.security;

import com.protomil.core.shared.logging.LogExecutionTime;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class CookieManager {

    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    public static final String USER_INFO_COOKIE = "USER_INFO";

    @Value("${protomil.security.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${protomil.security.cookies.same-site:Strict}")
    private String sameSite;

    @Value("${protomil.security.cookies.domain:}")
    private String cookieDomain;

    @Value("${protomil.security.cookies.path:/}")
    private String cookiePath;

    @LogExecutionTime
    public void setAuthenticationCookies(HttpServletResponse response, TokenPair tokenPair,
                                         UserTokenClaims userClaims, boolean rememberMe) {
        log.debug("Setting authentication cookies for user: {}", userClaims.getUserId());

        // Access token cookie (HttpOnly for security)
        Cookie accessTokenCookie = createSecureCookie(ACCESS_TOKEN_COOKIE,
                tokenPair.getAccessToken(), tokenPair.getAccessTokenExpiresIn(), true);
        response.addCookie(accessTokenCookie);

        // Refresh token cookie (HttpOnly for security)
        Cookie refreshTokenCookie = createSecureCookie(REFRESH_TOKEN_COOKIE,
                tokenPair.getRefreshToken(), tokenPair.getRefreshTokenExpiresIn(), true);
        response.addCookie(refreshTokenCookie);

        // User info cookie (accessible to JavaScript for UI)
        String userInfoJson = createUserInfoJson(userClaims);
        int userInfoMaxAge = rememberMe ? tokenPair.getRefreshTokenExpiresIn() : -1; // Session or remember me
        Cookie userInfoCookie = createSecureCookie(USER_INFO_COOKIE, userInfoJson, userInfoMaxAge, false);
        response.addCookie(userInfoCookie);

        log.debug("Authentication cookies set successfully for user: {}", userClaims.getUserId());
    }

    @LogExecutionTime
    public void clearAuthenticationCookies(HttpServletResponse response) {
        log.debug("Clearing authentication cookies");

        String[] cookieNames = {ACCESS_TOKEN_COOKIE, REFRESH_TOKEN_COOKIE, USER_INFO_COOKIE};

        for (String cookieName : cookieNames) {
            Cookie cookie = createSecureCookie(cookieName, "", 0, true);
            response.addCookie(cookie);
        }

        log.debug("Authentication cookies cleared successfully");
    }

    @LogExecutionTime
    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    @LogExecutionTime
    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    @LogExecutionTime
    public Optional<String> getUserInfo(HttpServletRequest request) {
        return getCookieValue(request, USER_INFO_COOKIE);
    }

    @LogExecutionTime
    public boolean hasValidAuthCookies(HttpServletRequest request) {
        Optional<String> accessToken = getAccessToken(request);
        Optional<String> refreshToken = getRefreshToken(request);

        boolean hasValidCookies = accessToken.isPresent() && refreshToken.isPresent();

        log.debug("Auth cookies validation - Access Token Present: {}, Refresh Token Present: {}",
                accessToken.isPresent(), refreshToken.isPresent());

        return hasValidCookies;
    }

    @LogExecutionTime
    public void updateAccessTokenCookie(HttpServletResponse response, String newAccessToken, int expiresIn) {
        log.debug("Updating access token cookie");

        Cookie accessTokenCookie = createSecureCookie(ACCESS_TOKEN_COOKIE, newAccessToken, expiresIn, true);
        response.addCookie(accessTokenCookie);

        log.debug("Access token cookie updated successfully");
    }

    private Cookie createSecureCookie(String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secureCookies);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(maxAge);

        if (StringUtils.hasText(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        // Note: SameSite attribute is not directly supported in Cookie class
        // It would need to be set via response headers in a production environment
        // For now, we'll log it and handle it in a filter if needed
        log.debug("Created cookie: {} with maxAge: {}, httpOnly: {}, secure: {}, sameSite: {}",
                name, maxAge, httpOnly, secureCookies, sameSite);

        return cookie;
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private String createUserInfoJson(UserTokenClaims userClaims) {
        // Simple JSON creation - in production, consider using ObjectMapper
        return String.format(
                "{\"userId\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"department\":\"%s\",\"roles\":%s}",
                userClaims.getUserId(),
                escapeJson(userClaims.getEmail()),
                escapeJson(userClaims.getFirstName()),
                escapeJson(userClaims.getLastName()),
                escapeJson(userClaims.getDepartment()),
                formatRolesJson(userClaims.getRoles())
        );
    }

    private String escapeJson(String value) {
        if (value == null) return "null";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String formatRolesJson(java.util.List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "[]";
        }

        return "[" + roles.stream()
                .map(role -> "\"" + escapeJson(role) + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("") + "]";
    }
}