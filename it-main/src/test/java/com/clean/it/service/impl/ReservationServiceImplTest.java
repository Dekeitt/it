package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceImplTest {
    @Test void rejectsAnOverlappingReservation() {
        ReservationRepository reservations=mock(ReservationRepository.class); JobRepository jobs=mock(JobRepository.class); PaymentService payments=mock(PaymentService.class);
        Job job=new Job(); job.setId(1L); job.setClientEmail("client@example.com"); job.setPriceCents(5_000L); when(jobs.findById(1L)).thenReturn(Optional.of(job));
        when(reservations.existsActiveOverlap("cleaner@example.com",Instant.parse("2030-01-01T11:00:00Z"),Instant.parse("2030-01-01T12:00:00Z"))).thenReturn(true);
        ReservationRequest request=new ReservationRequest(); request.setJobId(1L);request.setCleanerEmail("cleaner@example.com");request.setStartAt(Instant.parse("2030-01-01T11:00:00Z"));request.setDurationMinutes(60);
        ReservationServiceImpl service=new ReservationServiceImpl(reservations,jobs,payments);
        assertThatThrownBy(()->service.reserve("client@example.com",request)).isInstanceOf(IllegalStateException.class).hasMessageContaining("not available");
    }
    @Test void clientCanCancelScheduledReservationAndPaymentIsSettledFirst() {
        ReservationRepository reservations=mock(ReservationRepository.class); JobRepository jobs=mock(JobRepository.class); PaymentService payments=mock(PaymentService.class); Reservation reservation=reservation("SCHEDULED");
        when(reservations.findLockedById(9L)).thenReturn(Optional.of(reservation));when(reservations.save(reservation)).thenReturn(reservation);
        var response=new ReservationServiceImpl(reservations,jobs,payments).cancel("client@example.com",9L);
        verify(payments).cancelOrRefundReservationPayment(9L);assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }
    @Test void cleanerMustStartBeforeCompleting() {
        ReservationRepository reservations=mock(ReservationRepository.class); JobRepository jobs=mock(JobRepository.class); PaymentService payments=mock(PaymentService.class);when(reservations.findLockedById(9L)).thenReturn(Optional.of(reservation("SCHEDULED")));
        assertThatThrownBy(()->new ReservationServiceImpl(reservations,jobs,payments).complete("cleaner@example.com",9L)).isInstanceOf(IllegalStateException.class).hasMessageContaining("in-progress");
    }
    @Test void rescheduleExcludesTheReservationItselfFromOverlapCheck() {
        ReservationRepository reservations=mock(ReservationRepository.class); JobRepository jobs=mock(JobRepository.class); PaymentService payments=mock(PaymentService.class); Reservation reservation=reservation("SCHEDULED");
        when(reservations.findLockedById(9L)).thenReturn(Optional.of(reservation));when(reservations.existsActiveOverlapExcludingReservation(9L,"cleaner@example.com",Instant.parse("2030-02-01T10:00:00Z"),Instant.parse("2030-02-01T12:00:00Z"))).thenReturn(false);when(reservations.saveAndFlush(reservation)).thenReturn(reservation);
        ReservationRescheduleRequest request=new ReservationRescheduleRequest();request.setStartAt(Instant.parse("2030-02-01T10:00:00Z"));request.setDurationMinutes(120);
        var response=new ReservationServiceImpl(reservations,jobs,payments).reschedule("client@example.com",9L,request);assertThat(response.getStartAt()).isEqualTo(request.getStartAt());
    }
    private Reservation reservation(String status){Reservation r=new Reservation();r.setId(9L);r.setJobId(1L);r.setClientEmail("client@example.com");r.setCleanerEmail("cleaner@example.com");r.setStartAt(Instant.parse("2030-01-01T10:00:00Z"));r.setEndAt(Instant.parse("2030-01-01T12:00:00Z"));r.setDurationMinutes(120);r.setAgreedAmountCents(5_000L);r.setCurrency("eur");r.setStatus(status);return r;}
}
