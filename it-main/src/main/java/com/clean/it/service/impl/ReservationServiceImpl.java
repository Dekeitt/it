package com.clean.it.service.impl;

import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public ReservationResponse reserve(String clientEmail, ReservationRequest req) {
        Instant start = req.getStartAt();
        Instant end = start.plus(Duration.ofMinutes(req.getDurationMinutes()));
        boolean overlaps = reservationRepository.findByCleanerEmail(req.getCleanerEmail()).stream()
                .filter(existing -> !"CANCELLED".equalsIgnoreCase(existing.getStatus()))
                .anyMatch(existing -> {
                    Instant existingEnd = existing.getStartAt()
                            .plus(Duration.ofMinutes(existing.getDurationMinutes()));
                    return start.isBefore(existingEnd) && existing.getStartAt().isBefore(end);
                });
        if (overlaps) {
            throw new IllegalStateException("Cleaner not available at requested time");
        }

        Reservation r = new Reservation();
        r.setJobId(req.getJobId());
        r.setClientEmail(clientEmail);
        r.setCleanerEmail(req.getCleanerEmail());
        r.setStartAt(start);
        r.setDurationMinutes(req.getDurationMinutes());
        r.setStatus("SCHEDULED");
        Reservation saved = reservationRepository.save(r);
        return toDto(saved);
    }

    private ReservationResponse toDto(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setJobId(r.getJobId());
        resp.setClientEmail(r.getClientEmail());
        resp.setCleanerEmail(r.getCleanerEmail());
        resp.setStartAt(r.getStartAt());
        resp.setDurationMinutes(r.getDurationMinutes());
        resp.setStatus(r.getStatus());
        return resp;
    }

    @Override
    public java.util.List<ReservationResponse> listForUser(String userEmail) {
        java.util.List<Reservation> list = reservationRepository.findByClientEmailOrCleanerEmail(userEmail, userEmail);
        java.util.List<ReservationResponse> resp = new java.util.ArrayList<>();
        for (Reservation r : list) resp.add(toDto(r));
        return resp;
    }
}
