        package com.clean.it.controller;

        import com.clean.it.service.StripeWebhookService;
        import com.stripe.model.Event;
        import org.junit.jupiter.api.Test;

        import javax.crypto.Mac;
        import javax.crypto.spec.SecretKeySpec;
        import java.nio.charset.StandardCharsets;
        import java.time.Instant;

        import static org.assertj.core.api.Assertions.assertThat;
        import static org.mockito.ArgumentMatchers.any;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.verify;
        import static org.mockito.Mockito.when;

        class PaymentWebhookControllerTest {

            @Test
            void verifiesWithStripeSdkAndDelegatesTheParsedEvent() throws Exception {
                String secret = "whsec_test_secret";
                StripeWebhookService service = mock(StripeWebhookService.class);
                when(service.process(any(Event.class))).thenReturn(true);
                PaymentWebhookController controller = new PaymentWebhookController(service, secret);

                String payload = """
                        {"id":"evt_test_1","object":"event","api_version":"2025-12-15.clover",
                         "created":1893456000,"livemode":false,"pending_webhooks":1,
                         "type":"payment_intent.succeeded","data":{"object":{
                           "id":"pi_test_123","object":"payment_intent","amount":5000,
                           "currency":"eur","status":"succeeded"}}}
                        """.replace("\n", "").replace("  ", "");
                long timestamp = Instant.now().getEpochSecond();
                String signature = "t=" + timestamp + ",v1=" + sign(timestamp + "." + payload, secret);

                var response = controller.handleWebhook(signature, payload);

                assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(response.getBody().toString()).contains("duplicate=false");
                verify(service).process(any(Event.class));
            }

            @Test
            void rejectsAnInvalidSignature() {
                StripeWebhookService service = mock(StripeWebhookService.class);
                PaymentWebhookController controller = new PaymentWebhookController(service, "whsec_test_secret");

                var response = controller.handleWebhook("t=1,v1=invalid", "{}");

                assertThat(response.getStatusCode().value()).isEqualTo(400);
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
