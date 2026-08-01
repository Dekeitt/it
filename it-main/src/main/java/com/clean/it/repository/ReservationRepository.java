package com.clean.it.repository;

import com.clean.it.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Reservation> findByCleanerEmail(String cleanerEmail);

    List<Reservation> findByClientEmailOrCleanerEmail(String clientEmail, String cleanerEmail);
}

