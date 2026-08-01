package com.clean.it.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentWebhookControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class InMemoryStore implements com.clean.it.service.PaymentStore {
        Map<Long, com.clean.it.domain.Payment> payments = new HashMap<>();
        Map<String, com.clean.it.domain.PaymentEvent> events = new HashMap<>();
        long seq = 1;

        @Override
        public Optional<com.clean.it.domain.Payment> findByStripePaymentIntentId(String id) {
            return payments.values().stream().filter(p -> id.equals(p.getStripePaymentIntentId())).findFirst();
        }

        @Override
        public com.clean.it.domain.Payment savePayment(com.clean.it.domain.Payment p) {
            if (p.getId() == null) p.setId(seq++);
            payments.put(p.getId(), p);
            return p;
        }

        @Override
        public Optional<com.clean.it.domain.Payment> findById(Long id) { return Optional.ofNullable(payments.get(id)); }

        @Override
        public List<com.clean.it.domain.Payment> findByReservationId(Long reservationId) {
            List<com.clean.it.domain.Payment> list = new ArrayList<>();
            for (com.clean.it.domain.Payment p : payments.values()) if (Objects.equals(p.getReservationId(), reservationId)) list.add(p);
            return list;
        }

        @Override
        public List<com.clean.it.domain.Payment> findAll() { return new ArrayList<>(payments.values()); }

        @Override
        public boolean eventExists(String eventId) { return events.containsKey(eventId); }

        @Override
        public void saveEvent(String eventId, String type) {
            com.clean.it.domain.PaymentEvent e = new com.clean.it.domain.PaymentEvent();
            e.setEventId(eventId);
            e.setType(type);
            events.put(eventId, e);
        }
    }

    @Test
    public void webhookProcessesEventAndIsIdempotent() throws Exception {
        String secret = "whsec-test-secret";
        InMemoryStore store = new InMemoryStore();
        com.clean.it.controller.PaymentWebhookController controller = new com.clean.it.controller.PaymentWebhookController(store, secret);

        // create a payment record as if created earlier
        com.clean.it.domain.Payment p = new com.clean.it.domain.Payment();
        p.setReservationId(42L);
        p.setAmountCents(5000L);
        p.setStripePaymentIntentId("pi_test_123");
        p.setStatus("requires_payment_method");
        store.savePayment(p);

        // build a stripe-like event payload with data.object.id matching the payment
        String payload = mapper.writeValueAsString(
                mapper.createObjectNode()
                        .put("id", "evt_test_1")
                        .put("type", "payment_intent.succeeded")
                        .set("data", mapper.createObjectNode().set("object",
                                mapper.createObjectNode().put("id", "pi_test_123").put("status", "succeeded")
                        ))
        );

        long timestamp = Instant.now().getEpochSecond();
        String signature = "t=" + timestamp + ",v1=" + sign(timestamp + "." + payload, secret);

        assertThat(controller.handleWebhook(signature, payload).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(store.findByStripePaymentIntentId("pi_test_123").orElseThrow().getStatus()).isEqualTo("succeeded");
        assertThat(store.eventExists("evt_test_1")).isTrue();
        assertThat(controller.handleWebhook(signature, payload).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(store.events).hasSize(1);
    }

    private static String sign(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder result = new StringBuilder();
        for (byte b : mac.doFinal(value.getBytes(StandardCharsets.UTF_8))) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

