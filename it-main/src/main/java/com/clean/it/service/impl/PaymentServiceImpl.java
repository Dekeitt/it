package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.Payment;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.PaymentRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
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
    private final ReservationRepository reservationRepository;
    private final JobRepository jobRepository;
    private final String stripeSecretKey;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              ReservationRepository reservationRepository,
                              JobRepository jobRepository,
                              @Value("${stripe.secret-key:}") String stripeSecretKey) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.jobRepository = jobRepository;
        this.stripeSecretKey = stripeSecretKey;
    }

    @Override
    public PaymentResponse createPaymentIntent(String userEmail, PaymentRequest req) {
        Reservation reservation = reservationRepository.findById(req.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (reservation.getClientEmail() == null || !reservation.getClientEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Only the reservation client can create its payment");
        }
        Job job = jobRepository.findById(reservation.getJobId())
                .orElseThrow(() -> new IllegalStateException("Job linked to reservation not found"));
        long amountCents = job.getPriceCents() == null ? 0 : job.getPriceCents();
        if (amountCents <= 0) {
            throw new IllegalStateException("Reservation has no valid payable amount");
        }
        if (stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        try {
            String form = new StringBuilder()
                    .append("amount=").append(URLEncoder.encode(String.valueOf(amountCents), StandardCharsets.UTF_8))
                    .append("&currency=").append(URLEncoder.encode("eur", StandardCharsets.UTF_8))
                    .append("&automatic_payment_methods[enabled]=true")
                    .append("&metadata[reservationId]=")
                    .append(URLEncoder.encode(String.valueOf(req.getReservationId()), StandardCharsets.UTF_8))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.stripe.com/v1/payment_intents"))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Stripe API returned status {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Stripe API error: status=" + response.statusCode());
            }

            JsonNode node = mapper.readTree(response.body());
            Payment payment = new Payment();
            payment.setReservationId(req.getReservationId());
            payment.setAmountCents(amountCents);
            payment.setStripePaymentIntentId(node.path("id").asText(null));
            payment.setClientSecret(node.path("client_secret").asText(null));
            payment.setStatus(node.path("status").asText("created"));
            paymentRepository.save(payment);

            PaymentResponse result = new PaymentResponse();
            result.setClientSecret(payment.getClientSecret());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create payment intent", e);
        }
    }
}
