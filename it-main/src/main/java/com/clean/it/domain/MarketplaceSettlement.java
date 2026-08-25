package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="marketplace_settlements")
public class MarketplaceSettlement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="reservation_id",nullable=false,unique=true) private Long reservationId;
 @Column(name="payment_id",nullable=false,unique=true) private Long paymentId;
 @Column(name="cleaner_id",nullable=false) private Long cleanerId;
 @Column(name="connect_account_id",nullable=false) private Long connectAccountId;
 @Column(name="gross_cents",nullable=false) private Long grossCents;
 @Column(name="platform_fee_cents",nullable=false) private Long platformFeeCents;
 @Column(name="provider_amount_cents",nullable=false) private Long providerAmountCents;
 @Column(nullable=false,length=3) private String currency;
 @Column(name="stripe_payment_intent_id") private String stripePaymentIntentId;
 @Column(name="stripe_charge_id") private String stripeChargeId;
 @Column(name="stripe_transfer_id") private String stripeTransferId;
 @Column(name="stripe_application_fee_id") private String stripeApplicationFeeId;
 @Column(nullable=false,length=32) private String status="PENDING";
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getReservationId(){return reservationId;} public void setReservationId(Long v){reservationId=v;} public Long getPaymentId(){return paymentId;} public void setPaymentId(Long v){paymentId=v;} public Long getCleanerId(){return cleanerId;} public void setCleanerId(Long v){cleanerId=v;} public Long getConnectAccountId(){return connectAccountId;} public void setConnectAccountId(Long v){connectAccountId=v;} public Long getGrossCents(){return grossCents;} public void setGrossCents(Long v){grossCents=v;} public Long getPlatformFeeCents(){return platformFeeCents;} public void setPlatformFeeCents(Long v){platformFeeCents=v;} public Long getProviderAmountCents(){return providerAmountCents;} public void setProviderAmountCents(Long v){providerAmountCents=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public String getStripePaymentIntentId(){return stripePaymentIntentId;} public void setStripePaymentIntentId(String v){stripePaymentIntentId=v;} public String getStripeChargeId(){return stripeChargeId;} public void setStripeChargeId(String v){stripeChargeId=v;} public String getStripeTransferId(){return stripeTransferId;} public void setStripeTransferId(String v){stripeTransferId=v;} public String getStripeApplicationFeeId(){return stripeApplicationFeeId;} public void setStripeApplicationFeeId(String v){stripeApplicationFeeId=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
