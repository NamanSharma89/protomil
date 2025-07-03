// com/protomil/core/config/SecurityConfig.java
package com.protomil.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile({"prod", "test"})  // Only for production and test profiles
public class SecurityConfig {

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers("/api/v1/auth/reset-password").permitAll()

                        // Limited public actuator endpoints
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // Protected actuator endpoints (admin only)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Documentation endpoints (consider removing in prod)
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()

                        // Protected admin endpoints
                        .requestMatchers("/api/v1/users/*/approve").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = String.format(
                "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                awsRegion, userPoolId
        );
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}