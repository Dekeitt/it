package com.clean.it.service;

import com.clean.it.domain.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentStore {
    Optional<Payment> findByStripePaymentIntentId(String id);
    Payment savePayment(Payment p);
    Optional<Payment> findById(Long id);
    List<Payment> findByReservationId(Long reservationId);
    List<Payment> findAll();
    boolean eventExists(String eventId);
    void saveEvent(String eventId, String type);
}

