package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="connect_events")
public class ConnectEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="stripe_event_id",nullable=false,unique=true) private String stripeEventId;
 @Column(name="event_type",nullable=false,length=160) private String eventType;
 @Column(name="stripe_account_id") private String stripeAccountId;
 @Column(name="event_created_at",nullable=false) private Instant eventCreatedAt;
 @Column(nullable=false,length=32) private String status;
 @Column(nullable=false) private Integer attempts;
 @Column(name="claimed_at",nullable=false) private Instant claimedAt;
 @Column(name="processed_at") private Instant processedAt;
 @Column(name="failure_reason",length=1000) private String failureReason;
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getStripeEventId(){return stripeEventId;} public void setStripeEventId(String v){stripeEventId=v;} public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;} public String getStripeAccountId(){return stripeAccountId;} public void setStripeAccountId(String v){stripeAccountId=v;} public Instant getEventCreatedAt(){return eventCreatedAt;} public void setEventCreatedAt(Instant v){eventCreatedAt=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Integer getAttempts(){return attempts;} public void setAttempts(Integer v){attempts=v;} public Instant getClaimedAt(){return claimedAt;} public void setClaimedAt(Instant v){claimedAt=v;} public Instant getProcessedAt(){return processedAt;} public void setProcessedAt(Instant v){processedAt=v;} public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
}
