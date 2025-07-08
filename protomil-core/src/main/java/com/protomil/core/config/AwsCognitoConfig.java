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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@ConditionalOnProperty(name = "aws.cognito.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(CognitoProperties.class)
@Slf4j
public class AwsCognitoConfig {

    private final Environment environment;

    public AwsCognitoConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public CognitoIdentityProviderClient cognitoClient(CognitoProperties cognitoProperties) {
        AwsCredentialsProvider credentialsProvider = createCredentialsProvider();

        log.info("Initializing Cognito client for region: {} with credentials provider: {}",
                cognitoProperties.getRegion(), credentialsProvider.getClass().getSimpleName());

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoProperties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private AwsCredentialsProvider createCredentialsProvider() {
        boolean isDevelopment = isDevelopmentEnvironment();
        String awsProfile = getAwsProfile();

        if (isDevelopment && awsProfile != null && !awsProfile.isEmpty()) {
            log.info("Development environment detected. Using AWS profile: {}", awsProfile);
            try {
                return AwsCredentialsProviderChain.of(
                        ProfileCredentialsProvider.create(awsProfile),
                        EnvironmentVariableCredentialsProvider.create(),
                        DefaultCredentialsProvider.create()
                );
            } catch (Exception e) {
                log.warn("Failed to create profile credentials provider for profile: {}. Falling back to default.",
                        awsProfile, e);
                return createFallbackCredentialsProvider();
            }
        } else {
            if (!isDevelopment) {
                log.info("Production environment detected. Using instance profile or environment credentials");
                return AwsCredentialsProviderChain.of(
                        EnvironmentVariableCredentialsProvider.create(),
                        InstanceProfileCredentialsProvider.create(),
                        DefaultCredentialsProvider.create()
                );
            } else {
                log.info("Development environment but no AWS_PROFILE set. Using default credentials chain");
                return createFallbackCredentialsProvider();
            }
        }
    }

    private AwsCredentialsProvider createFallbackCredentialsProvider() {
        return AwsCredentialsProviderChain.of(
                EnvironmentVariableCredentialsProvider.create(),
                DefaultCredentialsProvider.create()
        );
    }

    private String getAwsProfile() {
        String profile = System.getenv("AWS_PROFILE");
        if (profile == null || profile.isEmpty()) {
            profile = System.getProperty("aws.profile");
        }
        if (profile == null || profile.isEmpty()) {
            profile = environment.getProperty("aws.profile");
        }
        return profile;
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