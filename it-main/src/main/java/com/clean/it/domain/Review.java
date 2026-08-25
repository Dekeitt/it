package com.clean.it.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", unique = true)
    private Long reservationId;

    @Column(name = "cleaner_id", nullable = false)
    private Long cleanerId;

    @Column(name = "cleaner_email", nullable = false)
    private String cleanerEmail;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @Column(name = "moderation_status", nullable = false, length = 32)
    private String moderationStatus = "VISIBLE";

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderated_by_user_id")
    private Long moderatedByUserId;

    @Column(name = "moderation_reason", length = 1000)
    private String moderationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getCleanerId() { return cleanerId; }
    public void setCleanerId(Long cleanerId) { this.cleanerId = cleanerId; }
    public String getCleanerEmail() { return cleanerEmail; }
    public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
    public Instant getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(Instant moderatedAt) { this.moderatedAt = moderatedAt; }
    public Long getModeratedByUserId() { return moderatedByUserId; }
    public void setModeratedByUserId(Long moderatedByUserId) { this.moderatedByUserId = moderatedByUserId; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
