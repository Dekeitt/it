package com.clean.it.repository;

import com.clean.it.domain.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Payment> findFirstByReservationId(Long reservationId);

    @Query("select p from Payment p, Reservation r where p.reservationId=r.id and p.id=:paymentId and (r.clientId=:userId or r.cleanerId=:userId)")
    Optional<Payment> findByIdVisibleToUser(@Param("paymentId") Long paymentId,@Param("userId") Long userId);
    @Query("select p from Payment p, Reservation r where p.reservationId=r.id and (r.clientId=:userId or r.cleanerId=:userId) order by p.createdAt desc")
    List<Payment> findVisibleToUser(@Param("userId") Long userId);
    @Query("select p from Payment p, Reservation r where p.reservationId=r.id and r.id=:reservationId and (r.clientId=:userId or r.cleanerId=:userId) order by p.createdAt desc")
    List<Payment> findByReservationIdVisibleToUser(@Param("reservationId") Long reservationId,@Param("userId") Long userId);

    @Query("""
      select p from Payment p
      where :q='' or lower(coalesce(p.status,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(p.stripePaymentIntentId,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(p.stripeDestinationAccount,'')) like lower(concat('%',:q,'%'))
         or cast(p.id as string)=:q or cast(p.reservationId as string)=:q
      order by p.createdAt desc
      """)
    List<Payment> adminSearch(@Param("q") String q, Pageable pageable);
}
