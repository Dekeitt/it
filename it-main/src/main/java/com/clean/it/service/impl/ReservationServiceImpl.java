package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.PaymentService;
import com.clean.it.service.ReservationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ReservationServiceImpl implements ReservationService {
    private static final String SCHEDULED = "SCHEDULED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";
    private final ReservationRepository reservationRepository;
    private final JobRepository jobRepository;
    private final CleanerRepository cleanerRepository;
    private final PaymentService paymentService;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  JobRepository jobRepository,
                                  CleanerRepository cleanerRepository,
                                  PaymentService paymentService) {
        this.reservationRepository = reservationRepository;
        this.jobRepository = jobRepository;
        this.cleanerRepository = cleanerRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public ReservationResponse reserve(Long clientId, String clientEmail, ReservationRequest req) {
        Job job = jobRepository.findById(req.getJobId()).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (!Objects.equals(job.getClientId(), clientId)) {
            throw new AccessDeniedException("Only the job owner can create its reservation");
        }
        if (job.getPriceCents() == null || job.getPriceCents() <= 0) {
            throw new IllegalStateException("Job has no valid payable amount");
        }

        Cleaner requestedCleaner = cleanerRepository.findByEmailIgnoreCase(req.getCleanerEmail())
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
        if (job.getCleanerId() != null && !Objects.equals(job.getCleanerId(), requestedCleaner.getUserId())) {
            throw new IllegalStateException("Reservation cleaner does not match the accepted job cleaner");
        }

        Instant start = req.getStartAt();
        Instant end = start.plus(Duration.ofMinutes(req.getDurationMinutes()));
        if (reservationRepository.existsActiveOverlap(requestedCleaner.getUserId(), start, end)) {
            throw new IllegalStateException("Cleaner not available at requested time");
        }
        Reservation reservation = new Reservation();
        reservation.setJobId(job.getId());
        reservation.setClientId(clientId);
        reservation.setClientEmail(clientEmail);
        reservation.setCleanerId(requestedCleaner.getUserId());
        reservation.setCleanerEmail(requestedCleaner.getEmail());
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setDurationMinutes(req.getDurationMinutes());
        reservation.setAgreedAmountCents(job.getPriceCents());
        reservation.setCurrency("eur");
        reservation.setStatus(SCHEDULED);
        return saveWithOverlapTranslation(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForUser(Long userId) {
        List<ReservationResponse> responses = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findByClientIdOrCleanerId(userId, userId)) {
            responses.add(toDto(reservation));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getForUser(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        ensureParticipant(userId, reservation);
        return toDto(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long clientId, Long reservationId) {
        Reservation reservation = locked(reservationId);
        ensureClient(clientId, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be cancelled");
        paymentService.cancelOrRefundReservationPayment(reservationId);
        reservation.setStatus(CANCELLED);
        return toDto(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse reschedule(Long clientId, Long reservationId, ReservationRescheduleRequest request) {
        Reservation reservation = locked(reservationId);
        ensureClient(clientId, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be rescheduled");
        Instant start = request.getStartAt();
        Instant end = start.plus(Duration.ofMinutes(request.getDurationMinutes()));
        if (reservationRepository.existsActiveOverlapExcludingReservation(
                reservationId, reservation.getCleanerId(), start, end)) {
            throw new IllegalStateException("Cleaner not available at requested time");
        }
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setDurationMinutes(request.getDurationMinutes());
        return saveWithOverlapTranslation(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse start(Long cleanerId, Long reservationId) {
        Reservation reservation = locked(reservationId);
        ensureCleaner(cleanerId, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be started");
        reservation.setStatus(IN_PROGRESS);
        return toDto(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse complete(Long cleanerId, Long reservationId) {
        Reservation reservation = locked(reservationId);
        ensureCleaner(cleanerId, reservation);
        requireStatus(reservation, IN_PROGRESS, "Only in-progress reservations can be completed");
        reservation.setStatus(COMPLETED);
        return toDto(reservationRepository.save(reservation));
    }

    private Reservation locked(Long reservationId) {
        return reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    private ReservationResponse saveWithOverlapTranslation(Reservation reservation) {
        try {
            return toDto(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            String cause = exception.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("reservations_no_cleaner_overlap")) {
                throw new IllegalStateException("Cleaner not available at requested time", exception);
            }
            throw exception;
        }
    }

    private void ensureParticipant(Long userId, Reservation reservation) {
        if (!Objects.equals(userId, reservation.getClientId()) && !Objects.equals(userId, reservation.getCleanerId())) {
            throw new AccessDeniedException("Reservation is not visible to this user");
        }
    }

    private void ensureClient(Long userId, Reservation reservation) {
        if (!Objects.equals(userId, reservation.getClientId())) {
            throw new AccessDeniedException("Only the reservation client can perform this operation");
        }
    }

    private void ensureCleaner(Long userId, Reservation reservation) {
        if (!Objects.equals(userId, reservation.getCleanerId())) {
            throw new AccessDeniedException("Only the assigned cleaner can perform this operation");
        }
    }

    private void requireStatus(Reservation reservation, String expected, String message) {
        String current = reservation.getStatus() == null ? "" : reservation.getStatus().toUpperCase(Locale.ROOT);
        if (!expected.equals(current)) {
            throw new IllegalStateException(message + " (current status: " + current + ")");
        }
    }

    private ReservationResponse toDto(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setJobId(reservation.getJobId());
        response.setClientEmail(reservation.getClientEmail());
        response.setCleanerEmail(reservation.getCleanerEmail());
        response.setStartAt(reservation.getStartAt());
        response.setEndAt(reservation.getEndAt());
        response.setDurationMinutes(reservation.getDurationMinutes());
        response.setAgreedAmountCents(reservation.getAgreedAmountCents());
        response.setCurrency(reservation.getCurrency());
        response.setStatus(reservation.getStatus());
        return response;
    }
}
