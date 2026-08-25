package com.clean.it.service;

public interface StripeGateway {
    boolean isConfigured();
    String publishableKey();
    IntentSnapshot createPaymentIntent(long amountCents, String currency,long reservationId, String idempotencyKey);
    IntentSnapshot createDestinationPaymentIntent(long amountCents,String currency,long reservationId,String destinationAccount,long platformFeeCents,boolean onBehalfOf,String idempotencyKey);
    IntentSnapshot retrievePaymentIntent(String paymentIntentId);
    IntentSnapshot cancelPaymentIntent(String paymentIntentId);
    RefundSnapshot refundPaymentIntent(String paymentIntentId, String idempotencyKey);
    RefundSnapshot refundDestinationPaymentIntent(String paymentIntentId,String idempotencyKey,boolean reverseTransfer,boolean refundApplicationFee);
    ReconciliationSnapshot reconcileDestinationPaymentIntent(String paymentIntentId);

    record IntentSnapshot(String id, String clientSecret, String status, String rawJson) {}
    record RefundSnapshot(String id, String status, String rawJson) {}
    record ReconciliationSnapshot(String paymentIntentId,String chargeId,String transferId,String applicationFeeId) {}
}
