package com.clean.it.service;

import com.clean.it.domain.UserAccount;
import com.clean.it.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserAccount synchronize(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }

        Identity identity = identity(authentication);
        UserAccount account = repository.findByIssuerAndSubject(identity.issuer(), identity.subject())
                .orElseGet(() -> claimLegacyAccount(identity.email()));

        account.setIssuer(identity.issuer());
        account.setSubject(identity.subject());
        account.setEmail(identity.email());
        account.setDisplayName(identity.displayName());
        account.setRoles(identity.roles());
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public UserAccount require(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User account not found"));
    }

    private UserAccount claimLegacyAccount(String email) {
        if (email != null && !email.isBlank()) {
            return repository.findFirstByEmailIgnoreCaseAndIssuer(email, UserAccount.LEGACY_EMAIL_ISSUER)
                    .orElseGet(UserAccount::new);
        }
        return new UserAccount();
    }

    private Identity identity(Authentication authentication) {
        String issuer = "local-basic";
        String subject = authentication.getName();
        String email = authentication.getName();
        String displayName = authentication.getName();

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            issuer = firstNonBlank(jwt.getClaimAsString("iss"), "unknown-issuer");
            subject = firstNonBlank(jwt.getSubject(), authentication.getName());
            email = firstNonBlank(jwt.getClaimAsString("email"), jwt.getClaimAsString("preferred_username"));
            displayName = firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username"), email, subject);
        }

        String roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .distinct()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return new Identity(issuer, subject, email, displayName, roles);
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private record Identity(String issuer, String subject, String email, String displayName, String roles) {}
}
