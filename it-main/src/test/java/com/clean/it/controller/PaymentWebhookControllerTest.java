package com.clean.it.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
        InMemoryStore store = new InMemoryStore();
        com.clean.it.controller.PaymentWebhookController controller = new com.clean.it.controller.PaymentWebhookController(store);

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

        // call webhook first time
        controller.handleWebhook(null, payload);

        // verify payment updated and event recorded
        com.clean.it.domain.Payment saved = store.findByStripePaymentIntentId("pi_test_123").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("succeeded");
        assertThat(store.eventExists("evt_test_1")).isTrue();

        // call webhook again (duplicate) - should be idempotent
        controller.handleWebhook(null, payload);

        // still only one event record
        assertThat(store.eventExists("evt_test_1")).isTrue();
    }
}


