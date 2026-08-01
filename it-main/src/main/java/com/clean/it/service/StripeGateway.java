package com.clean.it.service;

public interface StripeGateway {
    boolean isConfigured();
    String publishableKey();
    IntentSnapshot createPaymentIntent(long amountCents, String currency,
                                       long reservationId, String idempotencyKey);
    IntentSnapshot retrievePaymentIntent(String paymentIntentId);

    record IntentSnapshot(String id, String clientSecret, String status, String rawJson) {
    }
}
