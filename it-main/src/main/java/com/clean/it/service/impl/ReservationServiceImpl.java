package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.ReservationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final JobRepository jobRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  JobRepository jobRepository) {
        this.reservationRepository = reservationRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional
    public ReservationResponse reserve(String clientEmail, ReservationRequest req) {
        Job job = jobRepository.findById(req.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (job.getClientEmail() == null || !job.getClientEmail().equalsIgnoreCase(clientEmail)) {
            throw new AccessDeniedException("Only the job owner can create its reservation");
        }
        if (job.getPriceCents() == null || job.getPriceCents() <= 0) {
            throw new IllegalStateException("Job has no valid payable amount");
        }
        if (job.getCleanerEmail() != null
                && !job.getCleanerEmail().equalsIgnoreCase(req.getCleanerEmail())) {
            throw new IllegalStateException("Reservation cleaner does not match the accepted job cleaner");
        }

        Instant start = req.getStartAt();
        Instant end = start.plus(Duration.ofMinutes(req.getDurationMinutes()));
        if (reservationRepository.existsActiveOverlap(req.getCleanerEmail(), start, end)) {
            throw new IllegalStateException("Cleaner not available at requested time");
        }

        Reservation reservation = new Reservation();
        reservation.setJobId(job.getId());
        reservation.setClientEmail(clientEmail);
        reservation.setCleanerEmail(req.getCleanerEmail());
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setDurationMinutes(req.getDurationMinutes());
        reservation.setAgreedAmountCents(job.getPriceCents());
        reservation.setCurrency("eur");
        reservation.setStatus("SCHEDULED");
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

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> listForUser(String userEmail) {
        List<ReservationResponse> responses = new ArrayList<>();
        for (Reservation reservation : reservationRepository
                .findByClientEmailOrCleanerEmail(userEmail, userEmail)) {
            responses.add(toDto(reservation));
        }
        return responses;
    }
}
