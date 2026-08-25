package com.clean.it.service;

import com.clean.it.domain.UserAccount;
import com.clean.it.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {

    @Test
    void claimsLegacyEmailAccountAndRekeysItToIssuerAndSubjectWithoutChangingId() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount legacy = new UserAccount();
        legacy.setId(7L);
        legacy.setIssuer(UserAccount.LEGACY_EMAIL_ISSUER);
        legacy.setSubject("client@example.com");
        legacy.setEmail("client@example.com");

        when(repository.findByIssuerAndSubject("https://issuer.example/", "oidc-sub-123"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByEmailIgnoreCaseAndIssuer("client@example.com", UserAccount.LEGACY_EMAIL_ISSUER))
                .thenReturn(Optional.of(legacy));
        when(repository.save(legacy)).thenReturn(legacy);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://issuer.example/")
                .subject("oidc-sub-123")
                .claim("email", "client@example.com")
                .claim("name", "Client CleanIT")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

        UserAccount account = new UserAccountService(repository).synchronize(authentication);

        assertThat(account.getId()).isEqualTo(7L);
        assertThat(account.getIssuer()).isEqualTo("https://issuer.example/");
        assertThat(account.getSubject()).isEqualTo("oidc-sub-123");
        assertThat(account.getEmail()).isEqualTo("client@example.com");
        assertThat(account.getRoles()).isEqualTo("CLIENT");
        verify(repository).save(legacy);
    }

    @Test
    void existingIssuerSubjectAccountSurvivesAnEmailChange() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount existing = new UserAccount();
        existing.setId(9L);
        existing.setIssuer("https://issuer.example/");
        existing.setSubject("stable-sub");
        existing.setEmail("old@example.com");
        when(repository.findByIssuerAndSubject("https://issuer.example/", "stable-sub"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer("https://issuer.example/")
                .subject("stable-sub")
                .claim("email", "new@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));

        UserAccount account = new UserAccountService(repository).synchronize(authentication);

        assertThat(account.getId()).isEqualTo(9L);
        assertThat(account.getEmail()).isEqualTo("new@example.com");
    }
}
