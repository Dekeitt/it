package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="admin_audit_log")
public class AdminAuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="actor_user_id",nullable=false) private Long actorUserId;
 @Column(nullable=false,length=96) private String action;
 @Column(name="target_type",nullable=false,length=64) private String targetType;
 @Column(name="target_id",nullable=false,length=160) private String targetId;
 @Column(name="idempotency_key",length=255,unique=true) private String idempotencyKey;
 @Lob @Column(columnDefinition="text") private String details;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getActorUserId(){return actorUserId;} public void setActorUserId(Long v){actorUserId=v;} public String getAction(){return action;} public void setAction(String v){action=v;} public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=v;} public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;} public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;} public String getDetails(){return details;} public void setDetails(String v){details=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
