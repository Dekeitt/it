package com.clean.it.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "cleaner_availability", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cleaner_availability_slot",
                columnNames = {"cleaner_email", "day_of_week", "start_time", "end_time", "zone_id"})
})
public class CleanerAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cleaner_email", nullable = false)
    private String cleanerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "zone_id", nullable = false, length = 64)
    private String zoneId = "Europe/Madrid";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCleanerEmail() { return cleanerEmail; }
    public void setCleanerEmail(String cleanerEmail) { this.cleanerEmail = cleanerEmail; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
}
