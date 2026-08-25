package com.clean.it.service;

import com.clean.it.dto.AppDtos.AvailabilitySlotRequest;
import com.clean.it.dto.AppDtos.AvailabilitySlotResponse;
import com.clean.it.dto.AppDtos.CleanerDto;

import java.time.Instant;
import java.util.List;

public interface CleanerAvailabilityService {
    List<AvailabilitySlotResponse> list(String cleanerEmail);
    List<AvailabilitySlotResponse> replace(Long cleanerId, String cleanerEmail, List<AvailabilitySlotRequest> slots);
    List<CleanerDto> findAvailable(Instant startAt, int durationMinutes);
}
