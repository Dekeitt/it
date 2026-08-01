package com.clean.it.controller;

import com.clean.it.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final StripeWebhookService stripeWebhookService;
    private final String webhookSecret;

    public PaymentWebhookController(StripeWebhookService stripeWebhookService,
                                    @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.stripeWebhookService = stripeWebhookService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    @Hidden
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader,
            @RequestBody String payload) {
        if (webhookSecret.isBlank()) {
            log.error("Stripe webhook rejected because STRIPE_WEBHOOK_SECRET is not configured");
            return ResponseEntity.status(503).body(Map.of("error", "webhook verification unavailable"));
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing Stripe-Signature"));
        }

        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            boolean processed = stripeWebhookService.process(event);
            return ResponseEntity.ok(Map.of("received", true, "duplicate", !processed));
        } catch (SignatureVerificationException | IllegalArgumentException e) {
            log.warn("Rejected Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "invalid webhook"));
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "webhook processing failed"));
        }
    }
}
