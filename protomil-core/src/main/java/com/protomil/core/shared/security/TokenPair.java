// src/main/java/com/protomil/core/shared/security/TokenPair.java
package com.protomil.core.shared.security;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPair {

    private String accessToken;
    private String refreshToken;
    private Integer accessTokenExpiresIn;
    private Integer refreshTokenExpiresIn;
    private String tokenType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;
}