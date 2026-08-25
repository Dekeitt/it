package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="cleaner_connect_accounts")
public class CleanerConnectAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="cleaner_id",nullable=false,unique=true) private Long cleanerId;
 @Column(name="stripe_account_id",nullable=false,unique=true,length=255) private String stripeAccountId;
 @Column(name="country_code",length=2) private String countryCode;
 @Column(name="onboarding_status",nullable=false,length=32) private String onboardingStatus="PENDING";
 @Column(name="details_submitted",nullable=false) private boolean detailsSubmitted;
 @Column(name="charges_enabled",nullable=false) private boolean chargesEnabled;
 @Column(name="payouts_enabled",nullable=false) private boolean payoutsEnabled;
 @Lob @Column(name="requirements_json",columnDefinition="text") private String requirementsJson;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getCleanerId(){return cleanerId;} public void setCleanerId(Long v){cleanerId=v;} public String getStripeAccountId(){return stripeAccountId;} public void setStripeAccountId(String v){stripeAccountId=v;} public String getCountryCode(){return countryCode;} public void setCountryCode(String v){countryCode=v;} public String getOnboardingStatus(){return onboardingStatus;} public void setOnboardingStatus(String v){onboardingStatus=v;} public boolean isDetailsSubmitted(){return detailsSubmitted;} public void setDetailsSubmitted(boolean v){detailsSubmitted=v;} public boolean isChargesEnabled(){return chargesEnabled;} public void setChargesEnabled(boolean v){chargesEnabled=v;} public boolean isPayoutsEnabled(){return payoutsEnabled;} public void setPayoutsEnabled(boolean v){payoutsEnabled=v;} public String getRequirementsJson(){return requirementsJson;} public void setRequirementsJson(String v){requirementsJson=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
