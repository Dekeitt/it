package com.clean.it.controller;

import com.clean.it.domain.Payment;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.PaymentService;
import com.clean.it.service.PaymentStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Creación y consulta de pagos")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentStore paymentStore;
    private final ReservationRepository reservationRepository;

    public PaymentController(PaymentService paymentService,
                             PaymentStore paymentStore,
                             ReservationRepository reservationRepository) {
        this.paymentService = paymentService;
        this.paymentStore = paymentStore;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping("/create-intent")
    @Operation(summary = "Crear un intent de pago")
    public ResponseEntity<PaymentResponse> createIntent(Authentication authentication,
                                                        @Valid @RequestBody PaymentRequest req) {
        PaymentResponse response = paymentService.createPaymentIntent(authentication.getName(), req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago por ID")
    public ResponseEntity<?> getPayment(Authentication authentication, @PathVariable("id") Long id) {
        return paymentStore.findById(id)
                .map(payment -> canAccess(authentication.getName(), payment)
                        ? ResponseEntity.ok(payment)
                        : ResponseEntity.status(403).body(java.util.Map.of("error", "forbidden")))
                .orElseGet(() -> ResponseEntity.status(404).body(java.util.Map.of("error", "not found")));
    }

    @GetMapping
    @Operation(summary = "Listar pagos")
    public ResponseEntity<List<Payment>> findByReservation(Authentication authentication,
                                                           @RequestParam(value = "reservationId", required = false) Long reservationId) {
        List<Payment> source = reservationId == null
                ? paymentStore.findAll()
                : paymentStore.findByReservationId(reservationId);
        List<Payment> visible = source.stream()
                .filter(payment -> canAccess(authentication.getName(), payment))
                .toList();
        return ResponseEntity.ok(visible);
    }

    private boolean canAccess(String userEmail, Payment payment) {
        return reservationRepository.findById(payment.getReservationId())
                .map(reservation -> isParticipant(userEmail, reservation))
                .orElse(false);
    }

    private boolean isParticipant(String userEmail, Reservation reservation) {
        return userEmail != null && (userEmail.equalsIgnoreCase(reservation.getClientEmail())
                || userEmail.equalsIgnoreCase(reservation.getCleanerEmail()));
    }
}
