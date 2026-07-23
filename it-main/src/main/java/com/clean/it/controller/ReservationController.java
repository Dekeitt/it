package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Operaciones de reservas de jobs")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Crear una reserva")
    public ResponseEntity<ReservationResponse> reserve(Authentication authentication, @Valid @RequestBody ReservationRequest req) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String client = authentication.getName();
        ReservationResponse resp = reservationService.reserve(client, req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    @Operation(summary = "Listar reservas del usuario autenticado")
    public ResponseEntity<java.util.List<ReservationResponse>> list(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String user = authentication.getName();
        java.util.List<ReservationResponse> list = reservationService.listForUser(user);
        return ResponseEntity.ok(list);
    }
}
