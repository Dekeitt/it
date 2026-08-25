package com.clean.it.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="notification_preferences")
public class NotificationPreference {
    @Id @Column(name="user_id") private Long userId;
    @Column(name="email_enabled",nullable=false) private boolean emailEnabled=true;
    @Column(name="push_enabled",nullable=false) private boolean pushEnabled=true;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public boolean isEmailEnabled(){return emailEnabled;} public void setEmailEnabled(boolean emailEnabled){this.emailEnabled=emailEnabled;}
    public boolean isPushEnabled(){return pushEnabled;} public void setPushEnabled(boolean pushEnabled){this.pushEnabled=pushEnabled;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
