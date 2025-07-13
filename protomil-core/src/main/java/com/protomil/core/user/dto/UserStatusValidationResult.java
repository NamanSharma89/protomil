// src/main/java/com/protomil/core/user/dto/UserStatusValidationResult.java
package com.protomil.core.user.dto;

import com.protomil.core.shared.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusValidationResult {

    private String email;
    private boolean consistent;
    private boolean localUserExists;
    private UserStatus localStatus;
    private boolean cognitoUserExists;
    private UserStatusType cognitoStatus;
    private boolean cognitoEnabled;
    private String cognitoApprovalStatus;
    private String issue;

    public boolean hasIssues() {
        return !consistent || issue != null;
    }

    public String getSummary() {
        if (consistent) {
            return "Status is consistent between local DB and Cognito";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Status inconsistency detected for ").append(email).append(": ");

        if (localUserExists && cognitoUserExists) {
            summary.append(String.format("Local: %s, Cognito: %s/%s/%s",
                    localStatus,
                    cognitoStatus,
                    cognitoEnabled ? "enabled" : "disabled",
                    cognitoApprovalStatus));
        } else if (!localUserExists) {
            summary.append("User missing from local database");
        } else if (!cognitoUserExists) {
            summary.append("User missing from Cognito");
        }

        if (issue != null) {
            summary.append(" - ").append(issue);
        }

        return summary.toString();
    }
}