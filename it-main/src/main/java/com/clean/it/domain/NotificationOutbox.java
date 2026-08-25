package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="event_key",nullable=false,unique=true,length=255) private String eventKey;
    @Column(name="event_type",nullable=false,length=80) private String eventType;
    @Column(name="recipient_user_id",nullable=false) private Long recipientUserId;
    @Column(nullable=false,length=16) private String channel;
    @Column(nullable=false,length=255) private String subject;
    @Column(nullable=false,columnDefinition="TEXT") private String body;
    @Column(nullable=false,length=20) private String status="PENDING";
    @Column(nullable=false) private Integer attempts=0;
    @Column(name="available_at",nullable=false) private Instant availableAt=Instant.now();
    @Column(name="claimed_at") private Instant claimedAt;
    @Column(name="sent_at") private Instant sentAt;
    @Column(name="last_error",length=1000) private String lastError;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getEventKey(){return eventKey;} public void setEventKey(String eventKey){this.eventKey=eventKey;}
    public String getEventType(){return eventType;} public void setEventType(String eventType){this.eventType=eventType;}
    public Long getRecipientUserId(){return recipientUserId;} public void setRecipientUserId(Long recipientUserId){this.recipientUserId=recipientUserId;}
    public String getChannel(){return channel;} public void setChannel(String channel){this.channel=channel;}
    public String getSubject(){return subject;} public void setSubject(String subject){this.subject=subject;}
    public String getBody(){return body;} public void setBody(String body){this.body=body;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public Integer getAttempts(){return attempts;} public void setAttempts(Integer attempts){this.attempts=attempts;}
    public Instant getAvailableAt(){return availableAt;} public void setAvailableAt(Instant availableAt){this.availableAt=availableAt;}
    public Instant getClaimedAt(){return claimedAt;} public void setClaimedAt(Instant claimedAt){this.claimedAt=claimedAt;}
    public Instant getSentAt(){return sentAt;} public void setSentAt(Instant sentAt){this.sentAt=sentAt;}
    public String getLastError(){return lastError;} public void setLastError(String lastError){this.lastError=lastError;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
