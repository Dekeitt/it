package com.clean.it.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "cleaner_id", nullable = false)
    private Long cleanerId;

    @Column(name = "cleaner_email", nullable = false)
    private String cleanerEmail;

    @Column(name = "service_type_id")
    private Long serviceTypeId;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    private String status;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "agreed_amount_cents")
    private Long agreedAmountCents;

    @Column(length = 3, nullable = false)
    private String currency = "eur";

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public Long getCleanerId() { return cleanerId; }
    public void setCleanerId(Long cleanerId) { this.cleanerId = cleanerId; }
    public String getCleanerEmail() { return cleanerEmail; }
    public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
    public Long getServiceTypeId() { return serviceTypeId; }
    public void setServiceTypeId(Long serviceTypeId) { this.serviceTypeId = serviceTypeId; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    public Long getAgreedAmountCents() { return agreedAmountCents; }
    public void setAgreedAmountCents(Long agreedAmountCents) { this.agreedAmountCents = agreedAmountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
