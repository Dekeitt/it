package com.clean.it.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${ALLOW_BASIC_AUTH:false}")
    private boolean allowBasicAuth;

    @Value("${BASIC_AUTH_USERNAME:}")
    private String basicAuthUser;

    @Value("${BASIC_AUTH_PASSWORD:}")
    private String basicAuthPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig starting: ALLOW_BASIC_AUTH={}", allowBasicAuth);
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/ui.css", "/api/info", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/postman_collection.json").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cleaners", "/api/cleaners/**", "/api/reviews/**", "/api/jobs/open").permitAll()
                        .requestMatchers("/ws/**", "/sockjs/**", "/topic/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        if (allowBasicAuth) {
            http.httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
            Object role = jwt.getClaim("role");
            if (role instanceof String roleName && !roleName.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        if (!allowBasicAuth) {
            return new InMemoryUserDetailsManager();
        }
        if (basicAuthUser.isBlank() || basicAuthPassword.isBlank()) {
            throw new IllegalStateException("BASIC_AUTH_USERNAME and BASIC_AUTH_PASSWORD are required when Basic Auth is enabled");
        }
        UserDetails user = User.withUsername(basicAuthUser)
                .password("{noop}" + basicAuthPassword)
                .roles("CLIENT", "CLEANER")
                .build();
        log.info("Registering in-memory Basic Auth user='{}'", basicAuthUser);
        return new InMemoryUserDetailsManager(user);
    }
}
