package com.clean.it.controller;

import com.clean.it.domain.Payment;
import com.clean.it.service.PaymentStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Webhooks", description = "Recepción de eventos de Stripe")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final PaymentStore paymentStore;
    private final String webhookSecret;

    public PaymentWebhookController(PaymentStore paymentStore,
                                    @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.paymentStore = paymentStore;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Procesar webhook de Stripe")
    public ResponseEntity<?> handleWebhook(@RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
                                           @RequestBody String payload) {
        if (webhookSecret.isBlank()) {
            log.error("Stripe webhook rejected because STRIPE_WEBHOOK_SECRET is not configured");
            return ResponseEntity.status(503).body("Webhook verification unavailable");
        }
        try {
            if (!verifySignature(payload, sigHeader, webhookSecret)) {
                log.warn("Invalid Stripe webhook signature");
                return ResponseEntity.status(400).body("Invalid signature");
            }
        } catch (Exception e) {
            log.error("Error verifying Stripe signature", e);
            return ResponseEntity.status(400).body("Invalid signature");
        }

        try {
            JsonNode event = mapper.readTree(payload);
            String type = event.path("type").asText();
            String eventId = event.path("id").asText(null);
            if (eventId != null && paymentStore.eventExists(eventId)) {
                log.info("Duplicate Stripe event {} ignored", eventId);
                return ResponseEntity.ok(java.util.Map.of("received", true, "duplicate", true));
            }

            JsonNode obj = event.path("data").path("object");
            String intentId = obj.path("id").asText(null);
            String status = obj.path("status").asText(null);

            if (intentId != null) {
                Optional<Payment> maybe = paymentStore.findByStripePaymentIntentId(intentId);
                if (maybe.isPresent()) {
                    Payment payment = maybe.get();
                    payment.setStatus(status != null ? status : payment.getStatus());
                    payment.setRawJson(mapper.writeValueAsString(obj));
                    paymentStore.savePayment(payment);
                    log.info("Updated payment {} status={} from Stripe event {}", payment.getId(), payment.getStatus(), type);
                } else {
                    log.info("Received Stripe event for unknown payment intent {} type={}", intentId, type);
                }
            }

            if (eventId != null) {
                paymentStore.saveEvent(eventId, type);
            }

            return ResponseEntity.ok(java.util.Map.of("received", true));
        } catch (Exception e) {
            log.error("Failed to handle Stripe webhook", e);
            return ResponseEntity.status(500).body("error");
        }
    }

    private boolean verifySignature(String payload, String sigHeader, String secret) throws Exception {
        if (sigHeader == null) return false;
        String timestampValue = null;
        String signatureValue = null;
        for (String part : sigHeader.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) continue;
            if (pair[0].equals("t")) timestampValue = pair[1];
            if (pair[0].equals("v1")) signatureValue = pair[1];
        }
        if (timestampValue == null || signatureValue == null) return false;
        long timestamp = Long.parseLong(timestampValue);
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) return false;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((timestampValue + "." + payload).getBytes(StandardCharsets.UTF_8));
        byte[] supplied = hexToBytes(signatureValue);
        return supplied != null && MessageDigest.isEqual(expected, supplied);
    }

    private static byte[] hexToBytes(String value) {
        if (value.length() % 2 != 0) return null;
        byte[] bytes = new byte[value.length() / 2];
        try {
            for (int i = 0; i < value.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
