package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.MeResponse;
import com.clean.it.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Identity", description = "Identidad autenticada")
public class MeController {
    private final AuthenticatedUser authenticatedUser;

    public MeController(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping
    @Operation(summary = "Obtener la identidad autenticada persistida")
    public MeResponse me(Authentication authentication) {
        UserAccount account = authenticatedUser.account(authentication);
        MeResponse response = new MeResponse();
        response.setId(account.getId());
        response.setIssuer(account.getIssuer());
        response.setSubject(account.getSubject());
        response.setEmail(account.getEmail());
        response.setRoles(authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .distinct()
                .sorted()
                .toList());
        return response;
    }
}
