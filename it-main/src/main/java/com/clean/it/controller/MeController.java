package com.clean.it.controller;

import com.clean.it.dto.AppDtos.MeResponse;
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
    @Operation(summary = "Obtener la identidad autenticada")
    public MeResponse me(Authentication authentication) {
        MeResponse response = new MeResponse();
        response.setSubject(authentication.getName());
        response.setEmail(authenticatedUser.email(authentication));
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
