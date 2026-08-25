package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.CleanerAvailability;
import com.clean.it.dto.AppDtos.AvailabilitySlotRequest;
import com.clean.it.dto.AppDtos.AvailabilitySlotResponse;
import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.repository.CleanerAvailabilityRepository;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.CleanerAvailabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CleanerAvailabilityServiceImpl implements CleanerAvailabilityService {

    private final CleanerAvailabilityRepository availabilityRepository;
    private final CleanerRepository cleanerRepository;
    private final ReservationRepository reservationRepository;

    public CleanerAvailabilityServiceImpl(CleanerAvailabilityRepository availabilityRepository,
                                          CleanerRepository cleanerRepository,
                                          ReservationRepository reservationRepository) {
        this.availabilityRepository = availabilityRepository;
        this.cleanerRepository = cleanerRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> list(String cleanerEmail) {
        Cleaner cleaner = cleanerRepository.findByEmailIgnoreCase(cleanerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
        return availabilityRepository.findByCleanerIdOrderByDayOfWeekAscStartTimeAsc(cleaner.getUserId())
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<AvailabilitySlotResponse> replace(Long cleanerId, String cleanerEmail, List<AvailabilitySlotRequest> slots) {
        Cleaner cleaner = cleanerRepository.findFirstByUserId(cleanerId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
        validate(slots);
        availabilityRepository.deleteByCleanerId(cleanerId);
        availabilityRepository.flush();

        List<CleanerAvailability> entities = new ArrayList<>();
        for (AvailabilitySlotRequest slot : slots) {
            CleanerAvailability entity = new CleanerAvailability();
            entity.setCleanerId(cleanerId);
            entity.setCleanerEmail(cleaner.getEmail() == null ? cleanerEmail : cleaner.getEmail());
            entity.setDayOfWeek(slot.getDayOfWeek());
            entity.setStartTime(slot.getStartTime());
            entity.setEndTime(slot.getEndTime());
            entity.setZoneId(slot.getZoneId());
            entities.add(entity);
        }
        return availabilityRepository.saveAll(entities).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CleanerDto> findAvailable(Instant startAt, int durationMinutes) {
        if (durationMinutes < 30 || durationMinutes > 1440) {
            throw new IllegalArgumentException("durationMinutes must be between 30 and 1440");
        }
        Instant endAt = startAt.plus(Duration.ofMinutes(durationMinutes));
        List<CleanerDto> result = new ArrayList<>();
        for (Cleaner cleaner : cleanerRepository.findAll()) {
            if (isAvailable(cleaner.getUserId(), startAt, endAt)) {
                result.add(toDto(cleaner));
            }
        }
        return result;
    }

    private boolean isAvailable(Long cleanerId, Instant startAt, Instant endAt) {
        if (reservationRepository.existsActiveOverlap(cleanerId, startAt, endAt)) {
            return false;
        }
        for (CleanerAvailability slot : availabilityRepository.findByCleanerIdOrderByDayOfWeekAscStartTimeAsc(cleanerId)) {
            ZoneId zone = zone(slot.getZoneId());
            ZonedDateTime localStart = startAt.atZone(zone);
            ZonedDateTime localEnd = endAt.atZone(zone);
            if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
                continue;
            }
            if (localStart.getDayOfWeek() != slot.getDayOfWeek()) {
                continue;
            }
            LocalTime requestedStart = localStart.toLocalTime();
            LocalTime requestedEnd = localEnd.toLocalTime();
            if (!requestedStart.isBefore(slot.getStartTime()) && !requestedEnd.isAfter(slot.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    private void validate(List<AvailabilitySlotRequest> slots) {
        for (AvailabilitySlotRequest slot : slots) {
            if (!slot.getStartTime().isBefore(slot.getEndTime())) {
                throw new IllegalArgumentException("Availability startTime must be before endTime");
            }
            zone(slot.getZoneId());
        }
    }

    private ZoneId zone(String zoneId) {
        try {
            return ZoneId.of(zoneId);
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException("Unknown time zone: " + zoneId, exception);
        }
    }

    private AvailabilitySlotResponse toDto(CleanerAvailability slot) {
        AvailabilitySlotResponse response = new AvailabilitySlotResponse();
        response.setId(slot.getId());
        response.setCleanerEmail(slot.getCleanerEmail());
        response.setDayOfWeek(slot.getDayOfWeek());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setZoneId(slot.getZoneId());
        return response;
    }

    private CleanerDto toDto(Cleaner cleaner) {
        CleanerDto dto = new CleanerDto();
        dto.setId(cleaner.getId());
        dto.setEmail(cleaner.getEmail());
        dto.setName(cleaner.getName());
        dto.setRating(cleaner.getRating());
        return dto;
    }
}
