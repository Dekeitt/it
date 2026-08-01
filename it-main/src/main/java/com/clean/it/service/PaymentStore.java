package com.clean.it.service;

import com.clean.it.domain.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentStore {
    Optional<Payment> findByStripePaymentIntentId(String id);
    Optional<Payment> findFirstByReservationId(Long reservationId);
    Optional<Payment> findByIdVisibleToUser(Long id, String email);
    List<Payment> findVisibleToUser(String email);
    List<Payment> findByReservationIdVisibleToUser(Long reservationId, String email);
    Payment savePayment(Payment payment);
}

