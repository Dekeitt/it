package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.dto.AppDtos.ReservationReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.ReservationService;
import com.clean.it.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Ciclo completo de reservas")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final AuthenticatedUser authenticatedUser;

    public ReservationController(ReservationService reservationService,
                                 ReviewService reviewService,
                                 AuthenticatedUser authenticatedUser) {
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear una reserva")
    public ResponseEntity<ReservationResponse> reserve(Authentication authentication,
                                                       @Valid @RequestBody ReservationRequest request) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(reservationService.reserve(account.getId(), account.getEmail(), request));
    }

    @GetMapping
    @Operation(summary = "Listar reservas del usuario autenticado")
    public ResponseEntity<List<ReservationResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(reservationService.listForUser(authenticatedUser.id(authentication)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una reserva visible para el usuario")
    public ResponseEntity<ReservationResponse> get(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getForUser(authenticatedUser.id(authentication), id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Cancelar una reserva y cancelar/reembolsar su pago si existe")
    public ResponseEntity<ReservationResponse> cancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(authenticatedUser.id(authentication), id));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Reprogramar una reserva")
    public ResponseEntity<ReservationResponse> reschedule(Authentication authentication,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody ReservationRescheduleRequest request) {
        return ResponseEntity.ok(reservationService.reschedule(authenticatedUser.id(authentication), id, request));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Marcar una reserva como iniciada")
    public ResponseEntity<ReservationResponse> start(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.start(authenticatedUser.id(authentication), id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Marcar una reserva como completada")
    public ResponseEntity<ReservationResponse> complete(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.complete(authenticatedUser.id(authentication), id));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear la reseña de una reserva completada")
    public ResponseEntity<ReviewResponse> review(Authentication authentication,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody ReservationReviewRequest request) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(reviewService.addReviewForReservation(account.getId(), account.getEmail(), id, request));
    }
}
