package com.clean.it.service;

import com.clean.it.domain.Payment;
import com.clean.it.repository.PaymentEventRepository;
import com.clean.it.repository.PaymentRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class StripeWebhookService {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(15);
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final Set<String> PAYMENT_INTENT_EVENTS = Set.of(
            "payment_intent.processing",
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled"
    );

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;

    public StripeWebhookService(PaymentEventRepository paymentEventRepository,
                                PaymentRepository paymentRepository,
                                PlatformTransactionManager transactionManager) {
        this.paymentEventRepository = paymentEventRepository;
        this.paymentRepository = paymentRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public boolean process(Event event) {
        Instant now = Instant.now();
        Instant eventCreatedAt = event.getCreated() == null
                ? now
                : Instant.ofEpochSecond(event.getCreated());
        Boolean claimed = transactionTemplate.execute(status -> paymentEventRepository.claim(
                event.getId(), event.getType(), eventCreatedAt, now, now.minus(PROCESSING_LEASE)) > 0);
        if (!Boolean.TRUE.equals(claimed)) {
            return false;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> processClaimedEvent(event));
            return true;
        } catch (RuntimeException exception) {
            String failureReason = safeFailureReason(exception);
            transactionTemplate.executeWithoutResult(status ->
                    paymentEventRepository.markFailed(event.getId(), failureReason));
            throw exception;
        }
    }

    private void processClaimedEvent(Event event) {
        if (PAYMENT_INTENT_EVENTS.contains(event.getType())) {
            PaymentIntent intent = paymentIntent(event);
            paymentRepository.findByStripePaymentIntentId(intent.getId()).ifPresentOrElse(payment -> {
                updatePayment(payment, intent);
                paymentRepository.save(payment);
            }, () -> log.warn("Stripe event {} references unknown PaymentIntent {}",
                    event.getId(), intent.getId()));
        } else {
            log.info("Ignoring unsupported Stripe event type {} ({})", event.getType(), event.getId());
        }

        if (paymentEventRepository.markProcessed(event.getId(), Instant.now()) != 1) {
            throw new IllegalStateException("Stripe event processing lease was lost");
        }
    }

    private PaymentIntent paymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject object = deserializer.getObject().orElseGet(() -> deserializeUnsafe(deserializer, event));
        if (!(object instanceof PaymentIntent intent)) {
            throw new IllegalArgumentException("Stripe event does not contain a PaymentIntent");
        }
        return intent;
    }

    private StripeObject deserializeUnsafe(EventDataObjectDeserializer deserializer, Event event) {
        try {
            log.warn("Unsafe Stripe event deserialization for API version {} and event {}",
                    event.getApiVersion(), event.getId());
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new IllegalArgumentException("Stripe event payload could not be deserialized", e);
        }
    }

    private void updatePayment(Payment payment, PaymentIntent intent) {
        String intentCurrency = intent.getCurrency() == null
                ? null
                : intent.getCurrency().toLowerCase(Locale.ROOT);
        String expectedCurrency = payment.getCurrency() == null
                ? null
                : payment.getCurrency().toLowerCase(Locale.ROOT);
        if (!Objects.equals(payment.getAmountCents(), intent.getAmount())
                || !Objects.equals(expectedCurrency, intentCurrency)) {
            throw new IllegalStateException("Stripe payment amount or currency does not match the reservation");
        }

        payment.setStatus(intent.getStatus());
        payment.setRawJson(intent.toJson());
        if (intent.getClientSecret() != null) {
            payment.setClientSecret(intent.getClientSecret());
        }
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_FAILURE_REASON_LENGTH
                ? message
                : message.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
