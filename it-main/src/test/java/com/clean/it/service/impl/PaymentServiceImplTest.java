package com.clean.it.service.impl;

import com.clean.it.domain.Payment;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.PaymentRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.StripeGateway;
import com.clean.it.service.StripeGateway.IntentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    @Test
    void createsAnIntentFromTheFrozenReservationPriceWithAStableIdempotencyKey() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        StripeGateway stripe = mock(StripeGateway.class);

        Reservation reservation = reservation(42L);
        when(reservations.findLockedById(42L)).thenReturn(Optional.of(reservation));
        when(payments.findFirstByReservationId(42L)).thenReturn(Optional.empty());
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.publishableKey()).thenReturn("pk_test_123");
        when(stripe.createPaymentIntent(5_000L, "eur", 42L,
                "reservation:42:payment-intent:v1"))
                .thenReturn(new IntentSnapshot("pi_123", "secret_123", "requires_payment_method", "{}"));
        when(payments.save(org.mockito.ArgumentMatchers.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest();
        request.setReservationId(42L);
        var response = new PaymentServiceImpl(payments, reservations, jobs, stripe)
                .createPaymentIntent("client@example.com", request);

        assertThat(response.getAmountCents()).isEqualTo(5_000L);
        assertThat(response.getCurrency()).isEqualTo("eur");
        assertThat(response.getClientSecret()).isEqualTo("secret_123");
        verify(stripe).createPaymentIntent(5_000L, "eur", 42L,
                "reservation:42:payment-intent:v1");
    }

    @Test
    void reusesTheExistingIntentInsteadOfCreatingAnotherCharge() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ReservationRepository reservations = mock(ReservationRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        StripeGateway stripe = mock(StripeGateway.class);

        Reservation reservation = reservation(42L);
        Payment existing = new Payment();
        existing.setId(7L);
        existing.setReservationId(42L);
        existing.setAmountCents(5_000L);
        existing.setCurrency("eur");
        existing.setStripePaymentIntentId("pi_existing");
        existing.setClientSecret("secret_existing");
        existing.setStatus("requires_payment_method");

        when(reservations.findLockedById(42L)).thenReturn(Optional.of(reservation));
        when(payments.findFirstByReservationId(42L)).thenReturn(Optional.of(existing));
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.publishableKey()).thenReturn("pk_test_123");

        PaymentRequest request = new PaymentRequest();
        request.setReservationId(42L);
        var response = new PaymentServiceImpl(payments, reservations, jobs, stripe)
                .createPaymentIntent("client@example.com", request);

        assertThat(response.getPaymentId()).isEqualTo(7L);
        assertThat(response.getClientSecret()).isEqualTo("secret_existing");
        verify(stripe, never()).createPaymentIntent(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private Reservation reservation(long id) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setJobId(3L);
        reservation.setClientEmail("client@example.com");
        reservation.setCleanerEmail("cleaner@example.com");
        reservation.setAgreedAmountCents(5_000L);
        reservation.setCurrency("eur");
        return reservation;
    }
}
