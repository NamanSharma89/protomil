package com.protomil.core.shared.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/*
*
* Do not modify unless really need to. Main model for JWT format.
*
*
* */


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
    private String employeeId;
    private List<String> roles;
    private String tokenType;

    // Convenience methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public boolean hasRole(String role) {
        return roles != null && (roles.contains(role) || roles.contains("ROLE_" + role));
    }

    public boolean hasAnyRole(String... rolesToCheck) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        for (String role : rolesToCheck) {
            if (roles.contains(role) || roles.contains("ROLE_" + role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAccessToken() {
        return "access".equals(tokenType);
    }

    public boolean isRefreshToken() {
        return "refresh".equals(tokenType);
    }
}