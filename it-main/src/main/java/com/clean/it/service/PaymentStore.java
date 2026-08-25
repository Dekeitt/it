package com.clean.it.service;

import com.clean.it.domain.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentStore {
    Optional<Payment> findByStripePaymentIntentId(String id);
    Optional<Payment> findFirstByReservationId(Long reservationId);
    Optional<Payment> findByIdVisibleToUser(Long id, Long userId);
    List<Payment> findVisibleToUser(Long userId);
    List<Payment> findByReservationIdVisibleToUser(Long reservationId, Long userId);
    Payment savePayment(Payment payment);
}
