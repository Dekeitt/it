package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="web_push_subscriptions")
public class WebPushSubscription {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=2048,unique=true) private String endpoint;
    @Column(nullable=false,length=255) private String p256dh;
    @Column(name="auth_secret",nullable=false,length=255) private String authSecret;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    @Column(name="last_success_at") private Instant lastSuccessAt;
    @Column(name="disabled_at") private Instant disabledAt;
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public String getEndpoint(){return endpoint;} public void setEndpoint(String endpoint){this.endpoint=endpoint;}
    public String getP256dh(){return p256dh;} public void setP256dh(String p256dh){this.p256dh=p256dh;}
    public String getAuthSecret(){return authSecret;} public void setAuthSecret(String authSecret){this.authSecret=authSecret;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public Instant getLastSuccessAt(){return lastSuccessAt;} public void setLastSuccessAt(Instant lastSuccessAt){this.lastSuccessAt=lastSuccessAt;}
    public Instant getDisabledAt(){return disabledAt;} public void setDisabledAt(Instant disabledAt){this.disabledAt=disabledAt;}
}
