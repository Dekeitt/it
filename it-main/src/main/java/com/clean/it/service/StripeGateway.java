package com.clean.it.service;

public interface StripeGateway {
    boolean isConfigured();
    String publishableKey();
    IntentSnapshot createPaymentIntent(long amountCents, String currency,
                                       long reservationId, String idempotencyKey);
    IntentSnapshot retrievePaymentIntent(String paymentIntentId);
    IntentSnapshot cancelPaymentIntent(String paymentIntentId);
    RefundSnapshot refundPaymentIntent(String paymentIntentId, String idempotencyKey);

    record IntentSnapshot(String id, String clientSecret, String status, String rawJson) {
    }

    record RefundSnapshot(String id, String status, String rawJson) {
    }
}
