package com.clean.it.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_reservation", columnNames = "reservation_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(length = 3, nullable = false)
    private String currency = "eur";

    @Column(name = "platform_fee_cents")
    private Long platformFeeCents;

    @Column(name = "provider_amount_cents")
    private Long providerAmountCents;

    @Column(name = "stripe_destination_account")
    private String stripeDestinationAccount;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "stripe_transfer_id")
    private String stripeTransferId;

    @Column(name = "stripe_application_fee_id")
    private String stripeApplicationFeeId;

    @JsonIgnore
    @Column(name = "client_secret")
    private String clientSecret;

    @Lob
    @Column(columnDefinition = "text")
    @JsonIgnore
    private String rawJson;

    private String status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(Long platformFeeCents) { this.platformFeeCents = platformFeeCents; }
    public Long getProviderAmountCents() { return providerAmountCents; }
    public void setProviderAmountCents(Long providerAmountCents) { this.providerAmountCents = providerAmountCents; }
    public String getStripeDestinationAccount() { return stripeDestinationAccount; }
    public void setStripeDestinationAccount(String stripeDestinationAccount) { this.stripeDestinationAccount = stripeDestinationAccount; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }
    public String getStripeTransferId() { return stripeTransferId; }
    public void setStripeTransferId(String stripeTransferId) { this.stripeTransferId = stripeTransferId; }
    public String getStripeApplicationFeeId() { return stripeApplicationFeeId; }
    public void setStripeApplicationFeeId(String stripeApplicationFeeId) { this.stripeApplicationFeeId = stripeApplicationFeeId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
