package com.clean.it.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthenticatedUser {
    public String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            for (String claim : List.of("email", "preferred_username")) {
                String value = jwt.getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return authentication.getName();
    }
}
