package com.clean.it.repository;

import com.clean.it.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    java.util.List<Payment> findByReservationId(Long reservationId);
}

