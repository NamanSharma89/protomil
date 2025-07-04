// src/main/java/com/protomil/core/config/AwsCognitoConfig.java
package com.protomil.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@EnableConfigurationProperties(CognitoProperties.class)
@Slf4j
public class AwsCognitoConfig {

    private final Environment environment;

    public AwsCognitoConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnProperty(name = "aws.cognito.enabled", havingValue = "true")
    public CognitoIdentityProviderClient cognitoClient(CognitoProperties properties) {

        AwsCredentialsProvider credentialsProvider = createCredentialsProvider();

        log.info("Initializing Cognito client for region: {} with credentials provider: {}",
                properties.getRegion(), credentialsProvider.getClass().getSimpleName());

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private AwsCredentialsProvider createCredentialsProvider() {
        boolean isDevelopment = isDevelopmentEnvironment();
        String awsProfile = System.getenv("AWS_PROFILE");

        if (isDevelopment && awsProfile != null && !awsProfile.isEmpty()) {
            log.info("Development environment detected. Using AWS profile: {}", awsProfile);
            try {
                return ProfileCredentialsProvider.create(awsProfile);
            } catch (Exception e) {
                log.warn("Failed to create profile credentials provider for profile: {}. Falling back to default.", awsProfile, e);
                return DefaultCredentialsProvider.create();
            }
        } else {
            if (!isDevelopment) {
                log.info("Production environment detected. Using DefaultCredentialsProvider (IAM roles)");
            } else {
                log.info("Development environment but no AWS_PROFILE set. Using DefaultCredentialsProvider");
            }
            return DefaultCredentialsProvider.create();
        }
    }

    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "local".equals(profile) || "development".equals(profile)) {
                return true;
            }
        }

        String springEnv = System.getenv("SPRING_PROFILES_ACTIVE");
        return springEnv != null && (springEnv.contains("dev") || springEnv.contains("local"));
    }
}