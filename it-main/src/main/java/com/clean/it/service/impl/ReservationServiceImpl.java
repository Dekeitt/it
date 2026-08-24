package com.clean.it.service.impl;

import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
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

@Service
public class ReservationServiceImpl implements ReservationService {
    private static final String SCHEDULED = "SCHEDULED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";
    private final ReservationRepository reservationRepository;
    private final JobRepository jobRepository;
    private final PaymentService paymentService;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                  JobRepository jobRepository,
                                  PaymentService paymentService) {
        this.reservationRepository = reservationRepository;
        this.jobRepository = jobRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public ReservationResponse reserve(String clientEmail, ReservationRequest req) {
        Job job = jobRepository.findById(req.getJobId()).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (job.getClientEmail() == null || !job.getClientEmail().equalsIgnoreCase(clientEmail))
            throw new AccessDeniedException("Only the job owner can create its reservation");
        if (job.getPriceCents() == null || job.getPriceCents() <= 0)
            throw new IllegalStateException("Job has no valid payable amount");
        if (job.getCleanerEmail() != null && !job.getCleanerEmail().equalsIgnoreCase(req.getCleanerEmail()))
            throw new IllegalStateException("Reservation cleaner does not match the accepted job cleaner");
        Instant start = req.getStartAt();
        Instant end = start.plus(Duration.ofMinutes(req.getDurationMinutes()));
        if (reservationRepository.existsActiveOverlap(req.getCleanerEmail(), start, end))
            throw new IllegalStateException("Cleaner not available at requested time");
        Reservation reservation = new Reservation();
        reservation.setJobId(job.getId()); reservation.setClientEmail(clientEmail); reservation.setCleanerEmail(req.getCleanerEmail());
        reservation.setStartAt(start); reservation.setEndAt(end); reservation.setDurationMinutes(req.getDurationMinutes());
        reservation.setAgreedAmountCents(job.getPriceCents()); reservation.setCurrency("eur"); reservation.setStatus(SCHEDULED);
        return saveWithOverlapTranslation(reservation);
    }

    @Override @Transactional(readOnly = true)
    public List<ReservationResponse> listForUser(String userEmail) {
        List<ReservationResponse> responses = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findByClientEmailOrCleanerEmail(userEmail, userEmail)) responses.add(toDto(reservation));
        return responses;
    }

    @Override @Transactional(readOnly = true)
    public ReservationResponse getForUser(String userEmail, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        ensureParticipant(userEmail, reservation); return toDto(reservation);
    }

    @Override @Transactional
    public ReservationResponse cancel(String clientEmail, Long reservationId) {
        Reservation reservation = locked(reservationId); ensureClient(clientEmail, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be cancelled");
        paymentService.cancelOrRefundReservationPayment(reservationId);
        reservation.setStatus(CANCELLED); return toDto(reservationRepository.save(reservation));
    }

    @Override @Transactional
    public ReservationResponse reschedule(String clientEmail, Long reservationId, ReservationRescheduleRequest request) {
        Reservation reservation = locked(reservationId); ensureClient(clientEmail, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be rescheduled");
        Instant start = request.getStartAt(); Instant end = start.plus(Duration.ofMinutes(request.getDurationMinutes()));
        if (reservationRepository.existsActiveOverlapExcludingReservation(reservationId, reservation.getCleanerEmail(), start, end))
            throw new IllegalStateException("Cleaner not available at requested time");
        reservation.setStartAt(start); reservation.setEndAt(end); reservation.setDurationMinutes(request.getDurationMinutes());
        return saveWithOverlapTranslation(reservation);
    }

    @Override @Transactional
    public ReservationResponse start(String cleanerEmail, Long reservationId) {
        Reservation reservation = locked(reservationId); ensureCleaner(cleanerEmail, reservation);
        requireStatus(reservation, SCHEDULED, "Only scheduled reservations can be started");
        reservation.setStatus(IN_PROGRESS); return toDto(reservationRepository.save(reservation));
    }

    @Override @Transactional
    public ReservationResponse complete(String cleanerEmail, Long reservationId) {
        Reservation reservation = locked(reservationId); ensureCleaner(cleanerEmail, reservation);
        requireStatus(reservation, IN_PROGRESS, "Only in-progress reservations can be completed");
        reservation.setStatus(COMPLETED); return toDto(reservationRepository.save(reservation));
    }

    private Reservation locked(Long reservationId) { return reservationRepository.findLockedById(reservationId).orElseThrow(() -> new IllegalArgumentException("Reservation not found")); }
    private ReservationResponse saveWithOverlapTranslation(Reservation reservation) {
        try { return toDto(reservationRepository.saveAndFlush(reservation)); }
        catch (DataIntegrityViolationException exception) {
            String cause = exception.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains("reservations_no_cleaner_overlap")) throw new IllegalStateException("Cleaner not available at requested time", exception);
            throw exception;
        }
    }
    private void ensureParticipant(String email, Reservation r) { if (!same(email,r.getClientEmail()) && !same(email,r.getCleanerEmail())) throw new AccessDeniedException("Reservation is not visible to this user"); }
    private void ensureClient(String email, Reservation r) { if (!same(email,r.getClientEmail())) throw new AccessDeniedException("Only the reservation client can perform this operation"); }
    private void ensureCleaner(String email, Reservation r) { if (!same(email,r.getCleanerEmail())) throw new AccessDeniedException("Only the assigned cleaner can perform this operation"); }
    private void requireStatus(Reservation r, String expected, String message) { String current=r.getStatus()==null?"":r.getStatus().toUpperCase(Locale.ROOT); if(!expected.equals(current)) throw new IllegalStateException(message+" (current status: "+current+")"); }
    private boolean same(String a,String b){ return a!=null&&b!=null&&a.equalsIgnoreCase(b); }
    private ReservationResponse toDto(Reservation r) { ReservationResponse x=new ReservationResponse(); x.setId(r.getId());x.setJobId(r.getJobId());x.setClientEmail(r.getClientEmail());x.setCleanerEmail(r.getCleanerEmail());x.setStartAt(r.getStartAt());x.setEndAt(r.getEndAt());x.setDurationMinutes(r.getDurationMinutes());x.setAgreedAmountCents(r.getAgreedAmountCents());x.setCurrency(r.getCurrency());x.setStatus(r.getStatus());return x; }
}
