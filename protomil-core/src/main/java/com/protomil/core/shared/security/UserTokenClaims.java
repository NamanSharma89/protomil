// src/main/java/com/protomil/core/shared/security/UserTokenClaims.java
package com.protomil.core.shared.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenClaims {

    private String cognitoSub;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private List<String> roles;
    private String tokenType;
}