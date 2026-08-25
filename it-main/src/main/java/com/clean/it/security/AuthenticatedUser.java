package com.clean.it.security;

import com.clean.it.domain.UserAccount;
import com.clean.it.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {
    private final UserAccountService userAccountService;

    public AuthenticatedUser(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public UserAccount account(Authentication authentication) {
        return userAccountService.synchronize(authentication);
    }

    public Long id(Authentication authentication) {
        return account(authentication).getId();
    }

    public String email(Authentication authentication) {
        UserAccount account = account(authentication);
        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new IllegalStateException("Authenticated identity must provide email or preferred_username");
        }
        return account.getEmail();
    }
}
