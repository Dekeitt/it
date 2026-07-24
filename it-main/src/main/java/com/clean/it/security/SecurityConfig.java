package com.clean.it.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${security.jwt.secret:secret-key-should-be-changed-in-prod}")
    private String jwtSecret;

    @Value("${ALLOW_BASIC_AUTH:false}")
    private boolean allowBasicAuth;

    @Value("${BASIC_AUTH_USERNAME:dev}")
    private String basicAuthUser;

    @Value("${BASIC_AUTH_PASSWORD:dev}")
    private String basicAuthPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        log.info("SecurityConfig starting: ALLOW_BASIC_AUTH={} BASIC_AUTH_USERNAME={}", allowBasicAuth, basicAuthUser);
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/api/info", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/postman_collection.json").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cleaners", "/api/cleaners/**", "/api/reviews/**", "/api/jobs/open").permitAll()
                        .requestMatchers("/ws/**", "/sockjs/**", "/topic/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        // Optionally enable HTTP Basic for quick dev testing (controlled via ALLOW_BASIC_AUTH env var)
        if (allowBasicAuth) {
            // Spring Security 6+: httpBasic expects a Customizer argument
            http.httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Using symmetric key for simplicity. In production use RSA or managed keys.
        // Nimbus requires at least a 256-bit (32 byte) secret for HS256. Allow short dev secrets by
        // deriving a 256-bit key via SHA-256 when the provided secret is too short.
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes);
            } catch (Exception e) {
                // fallback to original bytes if digest unexpectedly fails
            }
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // don't expect 'scope' claim, map from custom 'role' claim instead
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // first, map the 'role' claim (string) to a GrantedAuthority
            var authorities = authoritiesConverter.convert(jwt);
            Object role = jwt.getClaim("role");
            if (role instanceof String) {
                // Spring expects ROLE_* prefix; JwtGrantedAuthoritiesConverter already handled prefix, but we add explicitly
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + ((String) role).toUpperCase()));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Provide an in-memory user only when basic auth is enabled. Password is stored {noop} for dev convenience.
        if (!allowBasicAuth) {
            return new InMemoryUserDetailsManager();
        }
        UserDetails user = User.withUsername(basicAuthUser)
                .password("{noop}" + basicAuthPassword)
                .roles("CLIENT", "CLEANER")
                .build();
        log.info("Registering in-memory basic auth user='{}' (roles CLIENT,CLEANER)", basicAuthUser);
        return new InMemoryUserDetailsManager(user);
    }
}
