package com.protomil.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoProperties {

    private boolean enabled = false;
    private String userPoolId;
    private String clientId;
    //TODO : Make aws region below as dynamic using property file
    private String region = "ap-south-1";
    private String userPoolDomain;

    // Email verification settings
    private EmailSettings email = new EmailSettings();

    // Auth flow settings
    private List<String> authFlows = List.of(
            "ALLOW_ADMIN_USER_PASSWORD_AUTH",
            "ALLOW_USER_PASSWORD_AUTH",
            "ALLOW_REFRESH_TOKEN_AUTH"
    );

    @Data
    public static class EmailSettings {
        private boolean verificationRequired = true;
        private String fromEmail;
        private String replyToEmail;
        private String verificationSubject = "Verify your Protomil account";
        private String verificationMessage = "Your verification code is {####}";
        private String inviteSubject = "Welcome to Protomil";
        private String inviteMessage = "Your username is {username} and temporary password is {####}";
    }
}