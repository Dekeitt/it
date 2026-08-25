package com.clean.it.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_accounts_issuer_subject", columnNames = {"issuer", "subject"})
})
public class UserAccount {
    public static final String LEGACY_EMAIL_ISSUER = "legacy:email";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String issuer;

    @Column(nullable = false, length = 512)
    private String subject;

    @Column(length = 320)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 1000)
    private String roles;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason", length = 1000)
    private String blockedReason;

    @Column(name = "blocked_by_user_id")
    private Long blockedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public Long getBlockedByUserId() { return blockedByUserId; }
    public void setBlockedByUserId(Long blockedByUserId) { this.blockedByUserId = blockedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
