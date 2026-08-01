package com.clean.it.service.impl;

import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationServiceImplTest {
    @Test
    void rejectsAnExistingReservationThatStartedEarlierButStillOverlaps() {
        ReservationRepository repository = mock(ReservationRepository.class);
        Reservation existing = new Reservation();
        existing.setCleanerEmail("cleaner@example.com");
        existing.setStartAt(Instant.parse("2030-01-01T10:00:00Z"));
        existing.setDurationMinutes(120);
        existing.setStatus("SCHEDULED");
        when(repository.findByCleanerEmail("cleaner@example.com")).thenReturn(List.of(existing));

        ReservationRequest request = new ReservationRequest();
        request.setJobId(1L);
        request.setCleanerEmail("cleaner@example.com");
        request.setStartAt(Instant.parse("2030-01-01T11:00:00Z"));
        request.setDurationMinutes(60);

        assertThatThrownBy(() -> new ReservationServiceImpl(repository).reserve("client@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }
}
