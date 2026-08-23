package com.clean.it.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final Environment environment;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${security.jwt.audience}")
    private String jwtAudience;

    @Value("${security.jwt.jwk-set-uri:}")
    private String jwtJwkSetUri;

    @Value("${security.basic.enabled:false}")
    private boolean allowBasicAuth;

    @Value("${security.basic.username:}")
    private String basicAuthUser;

    @Value("${security.basic.password:}")
    private String basicAuthPassword;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean localProfile = environment.matchesProfiles("local");
        if (allowBasicAuth && !localProfile) {
            throw new IllegalStateException("Basic Auth can only be enabled with the local profile");
        }

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline' https://unpkg.com https://js.stripe.com https://*.js.stripe.com; "
                                        + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                        + "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://*.stripe.com https://*.link.com; "
                                        + "frame-src https://js.stripe.com https://*.js.stripe.com https://hooks.stripe.com https://link.com https://*.link.com; "
                                        + "connect-src 'self' https://api.stripe.com https://link.com https://*.link.com; "
                                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'self'")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/info", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cleaners", "/api/cleaners/**", "/api/reviews/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        if (allowBasicAuth) {
            log.warn("Basic Auth is enabled for local development only");
            http.httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwtIssuer.isBlank() || jwtAudience.isBlank()) {
            throw new IllegalStateException("JWT issuer and audience must be configured");
        }

        NimbusJwtDecoder decoder;
        if (!jwtJwkSetUri.isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwtJwkSetUri).build();
        } else {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "Configure JWT_JWK_SET_URI or a JWT_SECRET containing at least 32 UTF-8 bytes");
            }
            SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(key).build();
        }

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(jwtIssuer);
        OAuth2TokenValidator<Jwt> audienceValidator = token -> token.getAudience() != null
                && token.getAudience().contains(jwtAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            addRoles(authorities, jwt.getClaim("role"));
            addRoles(authorities, jwt.getClaim("roles"));
            return authorities;
        });
        return converter;
    }

    private void addRoles(List<GrantedAuthority> authorities, Object claim) {
        if (claim instanceof String value) {
            for (String role : value.split("[,\\s]+")) {
                addRole(authorities, role);
            }
        } else if (claim instanceof Collection<?> values) {
            values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .forEach(value -> addRole(authorities, value));
        }
    }

    private void addRole(List<GrantedAuthority> authorities, String rawRole) {
        String role = rawRole == null ? "" : rawRole.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        if (!role.isBlank()) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            if (!authorities.contains(authority)) {
                authorities.add(authority);
            }
        }
    }

    @Bean
    public UserDetailsService userDetailsService() {
        if (!allowBasicAuth) {
            return new InMemoryUserDetailsManager();
        }
        if (basicAuthUser.isBlank() || basicAuthPassword.isBlank()) {
            throw new IllegalStateException("Basic Auth credentials are required when Basic Auth is enabled");
        }
        UserDetails user = User.withUsername(basicAuthUser)
                .password("{noop}" + basicAuthPassword)
                .roles("CLIENT", "CLEANER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
