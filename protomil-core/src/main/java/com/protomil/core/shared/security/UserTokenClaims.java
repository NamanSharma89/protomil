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

    private UUID userId; // Can be String, Long, or Integer depending on JWT implementation
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private String employeeId;
    private List<String> roles;
    private String cognitoUserSub;

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
}