package com.clean.it.service;

import com.clean.it.domain.Payment;
import com.clean.it.repository.PaymentEventRepository;
import com.clean.it.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripeWebhookServiceTest {

    @Test
    void claimsAndProcessesAPaymentIntentExactlyOnce() {
        PaymentEventRepository events = mock(PaymentEventRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PlatformTransactionManager transactions = transactionManager();
        Event event = event("evt_1", 5_000L, "eur", "succeeded");
        Payment payment = payment(5_000L, "eur");

        when(events.claim(anyString(), anyString(), any(Instant.class), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(events.markProcessed(anyString(), any(Instant.class))).thenReturn(1);
        when(payments.findByStripePaymentIntentId("pi_1")).thenReturn(Optional.of(payment));

        boolean processed = new StripeWebhookService(events, payments, transactions).process(event);

        assertThat(processed).isTrue();
        assertThat(payment.getStatus()).isEqualTo("succeeded");
        verify(payments).save(payment);
        verify(events).markProcessed(anyString(), any(Instant.class));
    }

    @Test
    void recordsFailureWhenStripeAmountDoesNotMatchTheFrozenPrice() {
        PaymentEventRepository events = mock(PaymentEventRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PlatformTransactionManager transactions = transactionManager();
        Event event = event("evt_2", 6_000L, "eur", "succeeded");

        when(events.claim(anyString(), anyString(), any(Instant.class), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(payments.findByStripePaymentIntentId("pi_1")).thenReturn(Optional.of(payment(5_000L, "eur")));

        StripeWebhookService service = new StripeWebhookService(events, payments, transactions);
        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");

        verify(events).markFailed(anyString(), anyString());
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        return manager;
    }

    private Event event(String eventId, long amount, String currency, String status) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getType()).thenReturn("payment_intent.succeeded");
        when(event.getCreated()).thenReturn(1_893_456_000L);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(intent));
        when(intent.getId()).thenReturn("pi_1");
        when(intent.getAmount()).thenReturn(amount);
        when(intent.getCurrency()).thenReturn(currency);
        when(intent.getStatus()).thenReturn(status);
        when(intent.toJson()).thenReturn("{}");
        return event;
    }

    private Payment payment(long amount, String currency) {
        Payment payment = new Payment();
        payment.setStripePaymentIntentId("pi_1");
        payment.setAmountCents(amount);
        payment.setCurrency(currency);
        payment.setStatus("processing");
        return payment;
    }
}
