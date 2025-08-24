package com.protomil.core.shared.repository;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        // Extract user ID from authentication
        // This assumes your authentication principal contains user ID
        try {
            if (authentication.getPrincipal() instanceof Long) {
                return Optional.of((Long) authentication.getPrincipal());
            } else if (authentication.getPrincipal() instanceof String) {
                return Optional.of(Long.valueOf((String) authentication.getPrincipal()));
            }
            // Add more cases based on your authentication implementation
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}