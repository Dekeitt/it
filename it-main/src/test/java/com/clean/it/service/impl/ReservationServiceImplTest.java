package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationServiceImplTest {
    @Test
    void rejectsAnExistingReservationThatStartedEarlierButStillOverlaps() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);

        Job job = new Job();
        job.setId(1L);
        job.setClientEmail("client@example.com");
        job.setPriceCents(5_000L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        when(reservationRepository.existsActiveOverlap(
                "cleaner@example.com",
                Instant.parse("2030-01-01T11:00:00Z"),
                Instant.parse("2030-01-01T12:00:00Z")))
                .thenReturn(true);

        ReservationRequest request = new ReservationRequest();
        request.setJobId(1L);
        request.setCleanerEmail("cleaner@example.com");
        request.setStartAt(Instant.parse("2030-01-01T11:00:00Z"));
        request.setDurationMinutes(60);

        ReservationServiceImpl service = new ReservationServiceImpl(reservationRepository, jobRepository);
        assertThatThrownBy(() -> service.reserve("client@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }
}
