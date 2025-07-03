// com/protomil/core/config/JpaAuditingConfig.java
package com.protomil.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    public static class SpringSecurityAuditorAware implements AuditorAware<UUID> {

        @Override
        public Optional<UUID> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }

            // Assuming your UserPrincipal has a getUserId() method
            try {
                if (authentication.getPrincipal() instanceof String) {
                    // For system operations or when user ID is directly in principal
                    return Optional.of(UUID.fromString((String) authentication.getPrincipal()));
                }
                // Add your custom UserPrincipal logic here
                return Optional.empty();
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }
}