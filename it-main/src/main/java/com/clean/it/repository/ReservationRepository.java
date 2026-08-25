package com.clean.it.repository;

import com.clean.it.domain.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("select case when count(r)>0 then true else false end from Reservation r where r.cleanerId=:cleanerId and upper(r.status)<>'CANCELLED' and r.startAt<:requestedEnd and r.endAt>:requestedStart")
    boolean existsActiveOverlap(@Param("cleanerId") Long cleanerId,@Param("requestedStart") Instant requestedStart,@Param("requestedEnd") Instant requestedEnd);
    @Query("select case when count(r)>0 then true else false end from Reservation r where r.id<>:reservationId and r.cleanerId=:cleanerId and upper(r.status)<>'CANCELLED' and r.startAt<:requestedEnd and r.endAt>:requestedStart")
    boolean existsActiveOverlapExcludingReservation(@Param("reservationId") Long reservationId,@Param("cleanerId") Long cleanerId,@Param("requestedStart") Instant requestedStart,@Param("requestedEnd") Instant requestedEnd);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Reservation r where r.id=:id") Optional<Reservation> findLockedById(@Param("id") Long id);
    List<Reservation> findByClientIdOrCleanerId(Long clientId,Long cleanerId);
    List<Reservation> findByStatusIgnoreCaseAndStartAtBetween(String status,Instant from,Instant to);

    @Query("""
      select r from Reservation r
      where :q='' or lower(r.clientEmail) like lower(concat('%',:q,'%'))
         or lower(r.cleanerEmail) like lower(concat('%',:q,'%'))
         or lower(r.status) like lower(concat('%',:q,'%'))
         or cast(r.id as string)=:q or cast(r.jobId as string)=:q
      order by r.startAt desc
      """)
    List<Reservation> adminSearch(@Param("q") String q, Pageable pageable);
}
