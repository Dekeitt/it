package com.clean.it.service.impl;

import com.clean.it.service.StripeGateway;
import com.clean.it.service.StripeGatewayException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeSdkGateway implements StripeGateway {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_NETWORK_RETRIES = 2;

    private final String secretKey;
    private final String publicKey;

    public StripeSdkGateway(@Value("${stripe.secret-key:}") String secretKey,
                            @Value("${stripe.publishable-key:}") String publicKey) {
        this.secretKey = secretKey;
        this.publicKey = publicKey;
    }

    @Override
    public boolean isConfigured() {
        return !secretKey.isBlank() && !publicKey.isBlank();
    }

    @Override
    public String publishableKey() {
        return publicKey;
    }

    @Override
    public IntentSnapshot createPaymentIntent(long amountCents, String currency,
                                              long reservationId, String idempotencyKey) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountCents);
            params.put("currency", currency);
            params.put("automatic_payment_methods", Map.of("enabled", true));
            params.put("metadata", Map.of("reservationId", String.valueOf(reservationId)));
            params.put("description", "Clean IT reservation " + reservationId);

            RequestOptions options = requestOptions()
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            return snapshot(PaymentIntent.create(params, options));
        } catch (StripeException exception) {
            throw new StripeGatewayException("Stripe could not initialize the payment", exception);
        }
    }

    @Override
    public IntentSnapshot retrievePaymentIntent(String paymentIntentId) {
        try {
            return snapshot(PaymentIntent.retrieve(paymentIntentId, (Map<String, Object>) null, requestOptions().build()));
        } catch (StripeException exception) {
            throw new StripeGatewayException("Stripe could not retrieve the existing payment", exception);
        }
    }

    private RequestOptions.RequestOptionsBuilder requestOptions() {
        return RequestOptions.builder()
                .setApiKey(secretKey)
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setReadTimeout(READ_TIMEOUT_MS)
                .setMaxNetworkRetries(MAX_NETWORK_RETRIES);
    }

    private IntentSnapshot snapshot(PaymentIntent intent) {
        return new IntentSnapshot(intent.getId(), intent.getClientSecret(), intent.getStatus(), intent.toJson());
    }
}
