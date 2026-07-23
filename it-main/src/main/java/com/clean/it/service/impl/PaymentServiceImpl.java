package com.clean.it.service.impl;

import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.clean.it.domain.Payment;
import com.clean.it.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentResponse createPaymentIntent(PaymentRequest req) {
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("STRIPE_SECRET_KEY not configured, cannot create real PaymentIntent");
            throw new IllegalStateException("Stripe not configured: set STRIPE_SECRET_KEY environment variable to enable payments");
        }

        try {
            String form = new StringBuilder()
                    .append("amount=").append(URLEncoder.encode(String.valueOf(req.getAmountCents()), StandardCharsets.UTF_8))
                    .append("&currency=").append(URLEncoder.encode("eur", StandardCharsets.UTF_8))
                    .append("&automatic_payment_methods[enabled]=true")
                    .append("&metadata[reservationId]=")
                    .append(URLEncoder.encode(String.valueOf(req.getReservationId()), StandardCharsets.UTF_8))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/payment_intents"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = mapper.readTree(response.body());
                String clientSecret = node.path("client_secret").asText(null);
                String stripeIntentId = node.path("id").asText(null);
                String status = node.path("status").asText(null);

                // persist payment record
                try {
                    Payment p = new Payment();
                    p.setReservationId(req.getReservationId());
                    p.setAmountCents(req.getAmountCents().longValue());
                    p.setStripePaymentIntentId(stripeIntentId);
                    p.setClientSecret(clientSecret);
                    p.setStatus(status != null ? status : "created");
                    paymentRepository.save(p);
                } catch (Exception e) {
                    log.error("Failed to persist payment record", e);
                }

                PaymentResponse resp = new PaymentResponse();
                resp.setClientSecret(clientSecret);
                return resp;
            } else {
                log.error("Stripe API returned status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Stripe API error: status=" + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error calling Stripe API", e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }
}

