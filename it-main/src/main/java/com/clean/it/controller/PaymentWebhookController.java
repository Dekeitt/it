package com.clean.it.controller;

import com.clean.it.domain.Payment;
import com.clean.it.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final com.clean.it.service.PaymentStore paymentStore;

    public PaymentWebhookController(com.clean.it.service.PaymentStore paymentStore) {
        this.paymentStore = paymentStore;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
                                           @RequestBody String payload) {
        String secret = System.getenv("STRIPE_WEBHOOK_SECRET");
        if (secret != null && !secret.isBlank()) {
            try {
                if (!verifySignature(payload, sigHeader, secret)) {
                    log.warn("Invalid Stripe webhook signature");
                    return ResponseEntity.status(400).body("Invalid signature");
                }
            } catch (Exception e) {
                log.error("Error verifying stripe signature", e);
                return ResponseEntity.status(400).body("Invalid signature");
            }
        } else {
            log.warn("STRIPE_WEBHOOK_SECRET not configured - accepting webhook without verification (unsafe)");
        }

        try {
            JsonNode event = mapper.readTree(payload);
            String type = event.path("type").asText();
            String eventId = event.path("id").asText(null);
            // idempotency: if we've already processed this eventId, return OK
            if (eventId != null) {
                if (this.paymentStore.eventExists(eventId)) {
                    log.info("Duplicate stripe event {} ignored", eventId);
                    return ResponseEntity.ok(java.util.Map.of("received", true, "duplicate", true));
                }
            }

            JsonNode obj = event.path("data").path("object");
            String intentId = obj.path("id").asText(null);
            String status = obj.path("status").asText(null);

            if (intentId != null) {
                Optional<Payment> maybe = paymentStore.findByStripePaymentIntentId(intentId);
                if (maybe.isPresent()) {
                    Payment p = maybe.get();
                    p.setStatus(status != null ? status : p.getStatus());
                    // store raw JSON of the payment_intent object
                    try {
                        p.setRawJson(mapper.writeValueAsString(obj));
                    } catch (Exception e) {
                        log.warn("Could not serialize payment_intent json", e);
                    }
                    paymentStore.savePayment(p);
                    log.info("Updated payment {} status={} from stripe event {}", p.getId(), p.getStatus(), type);
                } else {
                    log.info("Received stripe event for unknown payment intent {} type={}", intentId, type);
                }
            }

            // record event processed for idempotency
            if (eventId != null) {
                try {
                    paymentStore.saveEvent(eventId, type);
                } catch (Exception e) {
                    log.warn("Failed to persist PaymentEvent for {}", eventId, e);
                }
            }

            return ResponseEntity.ok().body(java.util.Map.of("received", true));
        } catch (Exception e) {
            log.error("Failed to handle stripe webhook", e);
            return ResponseEntity.status(500).body("error");
        }
    }

    private boolean verifySignature(String payload, String sigHeader, String secret) throws Exception {
        if (sigHeader == null) return false;
        // header format: t=timestamp,v1=signature[,v0=...]
        String[] parts = sigHeader.split(",");
        String t = null;
        String v1 = null;
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) t = kv[1];
            if (kv[0].equals("v1")) v1 = kv[1];
        }
        if (t == null || v1 == null) return false;
        long timestamp = Long.parseLong(t);
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > 300) { // 5 minutes tolerance
            log.warn("Stripe webhook timestamp outside tolerance: {}", timestamp);
            return false;
        }
        String signedPayload = t + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        String expectedHex = bytesToHex(expected);
        // Stripe's signature is hex lowercase
        return expectedHex.equalsIgnoreCase(v1);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

