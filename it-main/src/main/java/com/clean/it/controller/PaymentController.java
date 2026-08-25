package com.clean.it.controller;

import com.clean.it.domain.Payment;
import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.dto.AppDtos.PaymentSummary;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.PaymentService;
import com.clean.it.service.PaymentStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Creación y consulta de pagos")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentStore paymentStore;
    private final AuthenticatedUser authenticatedUser;

    public PaymentController(PaymentService paymentService,
                             PaymentStore paymentStore,
                             AuthenticatedUser authenticatedUser) {
        this.paymentService = paymentService;
        this.paymentStore = paymentStore;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping("/create-intent")
    @Operation(summary = "Crear o reutilizar un intent de pago")
    public ResponseEntity<PaymentResponse> createIntent(Authentication authentication,
                                                        @Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(authenticatedUser.id(authentication), req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago por ID")
    public ResponseEntity<PaymentSummary> getPayment(Authentication authentication,
                                                     @PathVariable("id") Long id) {
        return paymentStore.findByIdVisibleToUser(id, authenticatedUser.id(authentication))
                .map(this::toSummary)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Listar pagos accesibles por el usuario")
    public ResponseEntity<List<PaymentSummary>> findByReservation(
            Authentication authentication,
            @RequestParam(value = "reservationId", required = false) Long reservationId) {
        Long userId = authenticatedUser.id(authentication);
        List<Payment> payments = reservationId == null
                ? paymentStore.findVisibleToUser(userId)
                : paymentStore.findByReservationIdVisibleToUser(reservationId, userId);
        return ResponseEntity.ok(payments.stream().map(this::toSummary).toList());
    }

    private PaymentSummary toSummary(Payment payment) {
        PaymentSummary summary = new PaymentSummary();
        summary.setId(payment.getId());
        summary.setReservationId(payment.getReservationId());
        summary.setAmountCents(payment.getAmountCents());
        summary.setCurrency(payment.getCurrency());
        summary.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        summary.setStatus(payment.getStatus());
        summary.setCreatedAt(payment.getCreatedAt());
        summary.setUpdatedAt(payment.getUpdatedAt());
        return summary;
    }
}
