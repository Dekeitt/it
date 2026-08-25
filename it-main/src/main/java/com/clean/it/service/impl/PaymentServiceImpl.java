package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.Payment;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.PaymentRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.PaymentService;
import com.clean.it.service.StripeGateway;
import com.clean.it.service.StripeGateway.IntentSnapshot;
import com.clean.it.service.StripeGateway.RefundSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final Set<String> TERMINAL_CANCELLED = Set.of("canceled", "refunded");
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final JobRepository jobRepository;
    private final StripeGateway stripeGateway;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              ReservationRepository reservationRepository,
                              JobRepository jobRepository,
                              StripeGateway stripeGateway) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.jobRepository = jobRepository;
        this.stripeGateway = stripeGateway;
    }

    @Override
    @Transactional
    public PaymentResponse createPaymentIntent(Long userId, PaymentRequest req) {
        Reservation reservation = reservationRepository.findLockedById(req.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (!Objects.equals(reservation.getClientId(), userId)) {
            throw new AccessDeniedException("Only the reservation client can create its payment");
        }
        if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("Cancelled reservations cannot be paid");
        }
        if (!stripeGateway.isConfigured()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        freezePriceIfNecessary(reservation);
        Payment existing = paymentRepository.findFirstByReservationId(reservation.getId()).orElse(null);
        if (existing != null && existing.getStripePaymentIntentId() != null) {
            if (TERMINAL_CANCELLED.contains(normalize(existing.getStatus()))) {
                throw new IllegalStateException("The reservation payment is no longer payable");
            }
            if (existing.getClientSecret() == null || existing.getClientSecret().isBlank()) {
                existing = refreshPaymentIntent(existing);
            }
            return toResponse(existing);
        }

        String idempotencyKey = "reservation:" + reservation.getId() + ":payment-intent:v1";
        try {
            IntentSnapshot intent = stripeGateway.createPaymentIntent(
                    reservation.getAgreedAmountCents(), reservation.getCurrency(),
                    reservation.getId(), idempotencyKey);
            Payment payment = existing == null ? new Payment() : existing;
            payment.setReservationId(reservation.getId());
            payment.setAmountCents(reservation.getAgreedAmountCents());
            payment.setCurrency(reservation.getCurrency());
            applyIntent(payment, intent);
            Payment saved = paymentRepository.save(payment);
            reservation.setPaymentIntentId(intent.id());
            reservationRepository.save(reservation);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            log.error("Stripe failed to create a PaymentIntent for reservation {}", reservation.getId(), exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void cancelOrRefundReservationPayment(Long reservationId) {
        Payment payment = paymentRepository.findFirstByReservationId(reservationId).orElse(null);
        if (payment == null || payment.getStripePaymentIntentId() == null || payment.getStripePaymentIntentId().isBlank()) {
            return;
        }
        if (!stripeGateway.isConfigured()) {
            throw new IllegalStateException("Stripe is not configured, so an existing payment cannot be cancelled safely");
        }
        String status = normalize(payment.getStatus());
        if (TERMINAL_CANCELLED.contains(status)) {
            return;
        }
        if ("succeeded".equals(status)) {
            RefundSnapshot refund = stripeGateway.refundPaymentIntent(
                    payment.getStripePaymentIntentId(),
                    "reservation:" + reservationId + ":refund:v1");
            payment.setStatus("refunded");
            payment.setRawJson(refund.rawJson());
            paymentRepository.save(payment);
            return;
        }
        IntentSnapshot cancelled = stripeGateway.cancelPaymentIntent(payment.getStripePaymentIntentId());
        applyIntent(payment, cancelled);
        paymentRepository.save(payment);
    }

    private void freezePriceIfNecessary(Reservation reservation) {
        if (reservation.getAgreedAmountCents() != null && reservation.getAgreedAmountCents() > 0
                && reservation.getCurrency() != null && !reservation.getCurrency().isBlank()) return;
        Job job = jobRepository.findById(reservation.getJobId())
                .orElseThrow(() -> new IllegalStateException("Job linked to reservation not found"));
        long amountCents = job.getPriceCents() == null ? 0 : job.getPriceCents();
        if (amountCents <= 0) throw new IllegalStateException("Reservation has no valid payable amount");
        reservation.setAgreedAmountCents(amountCents);
        reservation.setCurrency("eur");
        reservationRepository.save(reservation);
    }

    private Payment refreshPaymentIntent(Payment payment) {
        IntentSnapshot intent = stripeGateway.retrievePaymentIntent(payment.getStripePaymentIntentId());
        applyIntent(payment, intent);
        return paymentRepository.save(payment);
    }

    private void applyIntent(Payment payment, IntentSnapshot intent) {
        payment.setStripePaymentIntentId(intent.id());
        payment.setClientSecret(intent.clientSecret());
        payment.setStatus(intent.status());
        payment.setRawJson(intent.rawJson());
    }

    private String normalize(String status) { return status == null ? "" : status.toLowerCase(Locale.ROOT); }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setClientSecret(payment.getClientSecret());
        response.setAmountCents(payment.getAmountCents());
        response.setCurrency(payment.getCurrency() == null ? null : payment.getCurrency().toLowerCase(Locale.ROOT));
        response.setStatus(payment.getStatus());
        response.setPublishableKey(stripeGateway.publishableKey());
        return response;
    }
}
