package com.clean.it.controller;

import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Creación y consulta de pagos")
public class PaymentController {

    private final PaymentService paymentService;
    private final com.clean.it.service.PaymentStore paymentStore;

    public PaymentController(PaymentService paymentService, com.clean.it.service.PaymentStore paymentStore) {
        this.paymentService = paymentService;
        this.paymentStore = paymentStore;
    }

    @PostMapping("/create-intent")
    @Operation(summary = "Crear un intent de pago")
    public ResponseEntity<PaymentResponse> createIntent(Authentication authentication, @Valid @RequestBody PaymentRequest req) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        PaymentResponse resp = paymentService.createPaymentIntent(req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago por ID")
    public ResponseEntity<?> getPayment(@PathVariable("id") Long id) {
        var opt = paymentStore.findById(id);
        if (opt.isPresent()) return ResponseEntity.ok(opt.get());
        return ResponseEntity.status(404).body(java.util.Map.of("error","not found"));
    }

    @GetMapping
    @Operation(summary = "Listar pagos")
    public ResponseEntity<java.util.List<com.clean.it.domain.Payment>> findByReservation(@RequestParam(value = "reservationId", required = false) Long reservationId) {
        if (reservationId != null) {
            return ResponseEntity.ok(paymentStore.findByReservationId(reservationId));
        }
        return ResponseEntity.ok(paymentStore.findAll());
    }
}
