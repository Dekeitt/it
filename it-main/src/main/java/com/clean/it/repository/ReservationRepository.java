package com.clean.it.repository;

import com.clean.it.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where lower(r.cleanerEmail) = lower(:cleanerEmail)
              and upper(r.status) <> 'CANCELLED'
              and r.startAt < :requestedEnd
              and r.endAt > :requestedStart
            """)
    boolean existsActiveOverlap(@Param("cleanerEmail") String cleanerEmail,
                                @Param("requestedStart") Instant requestedStart,
                                @Param("requestedEnd") Instant requestedEnd);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.id <> :reservationId
              and lower(r.cleanerEmail) = lower(:cleanerEmail)
              and upper(r.status) <> 'CANCELLED'
              and r.startAt < :requestedEnd
              and r.endAt > :requestedStart
            """)
    boolean existsActiveOverlapExcludingReservation(@Param("reservationId") Long reservationId,
                                                     @Param("cleanerEmail") String cleanerEmail,
                                                     @Param("requestedStart") Instant requestedStart,
                                                     @Param("requestedEnd") Instant requestedEnd);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(@Param("id") Long id);

    List<Reservation> findByClientEmailOrCleanerEmail(String clientEmail, String cleanerEmail);
}
