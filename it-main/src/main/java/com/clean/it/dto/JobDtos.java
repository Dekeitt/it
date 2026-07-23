package com.clean.it.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class JobDtos {

    public static class CreateJobRequest {
        @NotBlank
        @Size(max = 1000)
        private String description;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class JobResponse {
        private Long id;
        private String clientEmail;
        private String cleanerEmail;
        private String status;
        private String description;
        private Instant createdAt;
        private Instant updatedAt;

        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getClientEmail() { return clientEmail; }
        public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
        public String getCleanerEmail() { return cleanerEmail; }
        public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}

