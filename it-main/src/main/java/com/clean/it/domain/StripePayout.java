package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="stripe_payouts")
public class StripePayout {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="connect_account_id",nullable=false) private Long connectAccountId;
 @Column(name="stripe_payout_id",nullable=false,unique=true) private String stripePayoutId;
 @Column(name="amount_cents",nullable=false) private Long amountCents;
 @Column(nullable=false,length=3) private String currency;
 @Column(nullable=false,length=32) private String status;
 @Column(name="arrival_at") private Instant arrivalAt;
 @Column(name="failure_code",length=160) private String failureCode;
 @Lob @Column(name="raw_json",columnDefinition="text") private String rawJson;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getConnectAccountId(){return connectAccountId;} public void setConnectAccountId(Long v){connectAccountId=v;} public String getStripePayoutId(){return stripePayoutId;} public void setStripePayoutId(String v){stripePayoutId=v;} public Long getAmountCents(){return amountCents;} public void setAmountCents(Long v){amountCents=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getArrivalAt(){return arrivalAt;} public void setArrivalAt(Instant v){arrivalAt=v;} public String getFailureCode(){return failureCode;} public void setFailureCode(String v){failureCode=v;} public String getRawJson(){return rawJson;} public void setRawJson(String v){rawJson=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
