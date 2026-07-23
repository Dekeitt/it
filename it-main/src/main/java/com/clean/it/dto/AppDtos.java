package com.clean.it.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class AppDtos {

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
        @NotNull
        private Long jobId;
        @NotBlank
        private String cleanerEmail;
        @NotNull
        private Instant startAt;
        @NotNull
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
        private Integer durationMinutes;
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
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class PaymentRequest {
        @NotNull
        private Long reservationId;
        @NotNull
        private Integer amountCents;
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
        public Integer getAmountCents() { return amountCents; }
        public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }
    }

    public static class PaymentResponse {
        private String clientSecret;
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }

    public static class ReviewRequest {
        @NotBlank
        private String cleanerEmail;
        @NotNull @Min(1) @Max(5)
        private Integer rating;
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

