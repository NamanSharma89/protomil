// src/main/java/com/protomil/core/config/CognitoProperties.java
package com.protomil.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoProperties {
    private boolean enabled = false;
    private String userPoolId;
    private String clientId;
    private String region = "ap-south-1";
    private String userPoolDomain; // For hosted UI if needed later
}