package com.clean.it.repository;

import com.clean.it.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Payment> findFirstByReservationId(Long reservationId);


    @Query("""
            select p from Payment p, Reservation r
            where p.reservationId = r.id
              and p.id = :paymentId
              and (lower(r.clientEmail) = lower(:email) or lower(r.cleanerEmail) = lower(:email))
            """)
    Optional<Payment> findByIdVisibleToUser(@Param("paymentId") Long paymentId,
                                            @Param("email") String email);

    @Query("""
            select p from Payment p, Reservation r
            where p.reservationId = r.id
              and (lower(r.clientEmail) = lower(:email) or lower(r.cleanerEmail) = lower(:email))
            order by p.createdAt desc
            """)
    List<Payment> findVisibleToUser(@Param("email") String email);

    @Query("""
            select p from Payment p, Reservation r
            where p.reservationId = r.id
              and r.id = :reservationId
              and (lower(r.clientEmail) = lower(:email) or lower(r.cleanerEmail) = lower(:email))
            order by p.createdAt desc
            """)
    List<Payment> findByReservationIdVisibleToUser(@Param("reservationId") Long reservationId,
                                                   @Param("email") String email);
}

