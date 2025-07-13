// src/main/java/com/protomil/core/shared/security/JwtTokenManager.java
package com.protomil.core.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.protomil.core.shared.exception.AuthenticationException;
import com.protomil.core.shared.logging.LogExecutionTime;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class JwtTokenManager {

    private final SecretKey secretKey;
    private final ObjectMapper objectMapper;

    @Value("${protomil.security.jwt.access-token-expiration:1800}") // 30 minutes
    protected
    int accessTokenExpiration;

    @Value("${protomil.security.jwt.refresh-token-expiration:7200}") // 2 hours
    private int refreshTokenExpiration;

    @Value("${protomil.security.jwt.issuer:protomil-core}")
    private String issuer;

    public JwtTokenManager(@Value("${protomil.security.jwt.secret:}") String jwtSecret,
                           ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.secretKey = generateSecretKey(jwtSecret);
        log.info("JWT Token Manager initialized with access token expiration: {} seconds, refresh token expiration: {} seconds",
                accessTokenExpiration, refreshTokenExpiration);
    }

    @LogExecutionTime
    public TokenPair generateTokenPair(UserTokenClaims userClaims) {
        log.debug("Generating token pair for user: {}", userClaims.getUserId());

        String accessToken = generateAccessToken(userClaims);
        String refreshToken = generateRefreshToken(userClaims);

        log.debug("Token pair generated successfully for user: {}", userClaims.getUserId());

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiration)
                .refreshTokenExpiresIn(refreshTokenExpiration)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .build();
    }

    @LogExecutionTime
    public String generateAccessToken(UserTokenClaims userClaims) {
        log.debug("Generating access token for user: {}", userClaims.getUserId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userClaims.getCognitoSub());
        claims.put("userId", userClaims.getUserId().toString());
        claims.put("email", userClaims.getEmail());
        claims.put("firstName", userClaims.getFirstName());
        claims.put("lastName", userClaims.getLastName());
        claims.put("department", userClaims.getDepartment());
        claims.put("roles", userClaims.getRoles());
        claims.put("tokenType", "access");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpiration);

        String token = Jwts.builder()
                .claims(claims)  // Changed from setClaims to claims
                .issuer(issuer)  // Changed from setIssuer to issuer
                .subject(userClaims.getCognitoSub())  // Changed from setSubject to subject
                .audience().add("protomil-app").and()  // Changed from setAudience to audience
                .issuedAt(Date.from(now))  // Changed from setIssuedAt to issuedAt
                .expiration(Date.from(expiration))  // Changed from setExpiration to expiration
                .id(UUID.randomUUID().toString())  // Changed from setId to id
                .signWith(secretKey)  // Simplified - algorithm auto-detected
                .compact();

        log.debug("Access token generated for user: {} with expiration: {}",
                userClaims.getUserId(), expiration);

        return token;
    }

    @LogExecutionTime
    public String generateRefreshToken(UserTokenClaims userClaims) {
        log.debug("Generating refresh token for user: {}", userClaims.getUserId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userClaims.getCognitoSub());
        claims.put("userId", userClaims.getUserId().toString());
        claims.put("tokenType", "refresh");

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenExpiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(issuer)
                .setSubject(userClaims.getCognitoSub())
                .setAudience("protomil-app")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setId(UUID.randomUUID().toString())
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.debug("Refresh token generated for user: {} with expiration: {}",
                userClaims.getUserId(), expiration);

        return token;
    }

    @LogExecutionTime
    public Claims validateAndParseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Token is null or empty");
        }

        try {
            log.debug("Validating and parsing JWT token");

            Claims claims = Jwts.parser()  // Changed from parserBuilder
                    .verifyWith(secretKey)  // Changed from setSigningKey
                    .requireIssuer(issuer)
                    .requireAudience("protomil-app")
                    .build()
                    .parseSignedClaims(token)  // Changed from parseClaimsJws
                    .getPayload();  // Changed from getBody

            log.debug("Token validated successfully for subject: {}", claims.getSubject());
            return claims;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new AuthenticationException("Token has expired");

        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new AuthenticationException("Invalid token format");

        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw new AuthenticationException("Invalid token signature");

        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new AuthenticationException("Unsupported token type");

        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new AuthenticationException("Token validation failed");

        } catch (IllegalArgumentException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            throw new AuthenticationException("Token validation failed");
        }
    }

    @LogExecutionTime
    public UserTokenClaims extractUserClaims(String token) {
        Claims claims = validateAndParseToken(token);

        log.debug("Extracting user claims from token for subject: {}", claims.getSubject());

        try {
            return UserTokenClaims.builder()
                    .cognitoSub(claims.getSubject())
                    .userId(UUID.fromString(claims.get("userId", String.class)))
                    .email(claims.get("email", String.class))
                    .firstName(claims.get("firstName", String.class))
                    .lastName(claims.get("lastName", String.class))
                    .department(claims.get("department", String.class))
                    .roles(extractRoles(claims))
                    .tokenType(claims.get("tokenType", String.class))
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract user claims from token: {}", e.getMessage());
            throw new AuthenticationException("Invalid token claims");
        }
    }

    @LogExecutionTime
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());

            log.debug("Token expiration check - Expires: {}, Is Expired: {}", expiration, expired);
            return expired;

        } catch (ExpiredJwtException e) {
            log.debug("Token is expired");
            return true;

        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true; // Treat invalid tokens as expired
        }
    }

    @LogExecutionTime
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);

        } catch (Exception e) {
            log.warn("Error checking token type: {}", e.getMessage());
            return false;
        }
    }

    @LogExecutionTime
    public LocalDateTime getTokenExpiration(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            Date expiration = claims.getExpiration();
            return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());

        } catch (Exception e) {
            log.warn("Error getting token expiration: {}", e.getMessage());
            return LocalDateTime.now().minusMinutes(1); // Return past time for invalid tokens
        }
    }

    // Updated secret key generation for JJWT 0.12.3
    private SecretKey generateSecretKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            log.warn("No JWT secret provided, generating random secret for development");
            return Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);
        }

        try {
            // Ensure secret is base64 encoded and of sufficient length
            byte[] secretBytes;
            if (jwtSecret.length() < 64) {
                // Pad short secrets for development
                String paddedSecret = (jwtSecret + "0".repeat(64)).substring(0, 64);
                secretBytes = Base64.getEncoder().encode(paddedSecret.getBytes());
            } else {
                secretBytes = Base64.getDecoder().decode(jwtSecret);
            }

            log.info("JWT secret key initialized successfully");
            return Keys.hmacShaKeyFor(secretBytes);

        } catch (Exception e) {
            log.warn("Failed to decode provided JWT secret, generating random secret: {}", e.getMessage());
            return Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        try {
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            } else if (rolesObj instanceof String) {
                // Handle single role as string
                return List.of((String) rolesObj);
            }
            return List.of();

        } catch (Exception e) {
            log.warn("Failed to extract roles from token claims: {}", e.getMessage());
            return List.of();
        }
    }
}