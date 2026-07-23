package com.clean.it.repository;

import com.clean.it.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCleanerEmailAndStartAtBetween(String cleanerEmail, Instant from, Instant to);
    List<Reservation> findByClientEmailOrCleanerEmail(String clientEmail, String cleanerEmail);
}

