package com.agileo.AGILEO.security;

import com.agileo.AGILEO.entity.secondary.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }

            if (authentication instanceof CustomJwtAuthenticationToken) {
                CustomJwtAuthenticationToken customToken = (CustomJwtAuthenticationToken) authentication;
                User user = customToken.getUser();
                if (user != null && user.getLogin() != null) {
                    return Optional.of(user.getLogin());
                }
            }

            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                String username = jwtToken.getToken().getClaimAsString("preferred_username");
                if (username != null && !username.isEmpty()) {
                    return Optional.of(username);
                }
                return Optional.of(authentication.getName());
            }

            String name = authentication.getName();
            if (name != null && !name.equals("anonymousUser")) {
                return Optional.of(name);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur récupération auditor: " + e.getMessage());
        }

        return Optional.of("SYSTEM");
    }
}