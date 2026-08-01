package com.clean.it.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AppDtos {
    private AppDtos() {
    }

    public static class CleanerDto {
        private Long id;
        private String email;
        private String name;
        private Double rating;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
    }

    public static class ReservationRequest {
        @NotNull @Positive
        private Long jobId;
        @NotBlank @Email
        private String cleanerEmail;
        @NotNull @Future
        private Instant startAt;
        @NotNull @Min(30) @Max(1440)
        private Integer durationMinutes;
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getCleanerEmail() { return cleanerEmail; }
        public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
        public Instant getStartAt() { return startAt; }
        public void setStartAt(Instant startAt) { this.startAt = startAt; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    }

    public static class ReservationResponse {
        private Long id;
        private Long jobId;
        private String clientEmail;
        private String cleanerEmail;
        private Instant startAt;
        private Instant endAt;
        private Integer durationMinutes;
        private Long agreedAmountCents;
        private String currency;
        private String status;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public String getCleanerEmail() { return cleanerEmail; }
        public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
        public Instant getStartAt() { return startAt; }
        public void setStartAt(Instant startAt) { this.startAt = startAt; }
        public Instant getEndAt() { return endAt; }
        public void setEndAt(Instant endAt) { this.endAt = endAt; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public Long getAgreedAmountCents() { return agreedAmountCents; }
        public void setAgreedAmountCents(Long agreedAmountCents) { this.agreedAmountCents = agreedAmountCents; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class PaymentRequest {
        @NotNull @Positive
        private Long reservationId;
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    }

    public static class PaymentResponse {
        private Long paymentId;
        private String clientSecret;
        private Long amountCents;
        private String currency;
        private String status;
        private String publishableKey;
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public Long getAmountCents() { return amountCents; }
        public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }
    }

    public static class PaymentSummary {
        private Long id;
        private Long reservationId;
        private Long amountCents;
        private String currency;
        private String stripePaymentIntentId;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
        public Long getAmountCents() { return amountCents; }
        public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStripePaymentIntentId() { return stripePaymentIntentId; }
        public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ReviewRequest {
        @NotBlank @Email
        private String cleanerEmail;
        @NotNull @Min(1) @Max(5)
        private Integer rating;
        @Size(max = 2000)
        private String comment;
        public String getCleanerEmail() { return cleanerEmail; }
        public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class ReviewResponse {
        private Long id;
        private String cleanerEmail;
        private String clientEmail;
        private Integer rating;
        private String comment;
        private Instant createdAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCleanerEmail() { return cleanerEmail; }
        public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
