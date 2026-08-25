package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.ConnectDtos.*;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.ConnectAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connect")
@Tag(name="Stripe Connect",description="Onboarding y estado de cobros del profesional")
public class ConnectAccountController {
 private final ConnectAccountService service; private final AuthenticatedUser authenticatedUser;
 public ConnectAccountController(ConnectAccountService service,AuthenticatedUser authenticatedUser){this.service=service;this.authenticatedUser=authenticatedUser;}
 @GetMapping("/status") @PreAuthorize("hasRole('CLEANER')") @Operation(summary="Consultar estado de onboarding/payouts")
 public ResponseEntity<ConnectStatusResponse> status(Authentication a){return ResponseEntity.ok(service.status(authenticatedUser.id(a)));}
 @PostMapping("/onboarding") @PreAuthorize("hasRole('CLEANER')") @Operation(summary="Crear o reanudar onboarding Stripe-hosted")
 public ResponseEntity<OnboardingResponse> onboarding(Authentication a,@Valid @RequestBody OnboardingRequest request){UserAccount account=authenticatedUser.account(a);return ResponseEntity.ok(service.onboarding(account.getId(),account.getEmail(),request));}
}
