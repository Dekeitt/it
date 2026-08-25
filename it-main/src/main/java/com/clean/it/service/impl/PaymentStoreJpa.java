package com.clean.it.service.impl;

import com.clean.it.domain.Payment;
import com.clean.it.repository.PaymentRepository;
import com.clean.it.service.PaymentStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentStoreJpa implements PaymentStore {

    private final PaymentRepository paymentRepository;

    public PaymentStoreJpa(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Optional<Payment> findByStripePaymentIntentId(String id) {
        return paymentRepository.findByStripePaymentIntentId(id);
    }

    @Override
    public Optional<Payment> findFirstByReservationId(Long reservationId) {
        return paymentRepository.findFirstByReservationId(reservationId);
    }

    @Override
    public Optional<Payment> findByIdVisibleToUser(Long id, Long userId) {
        return paymentRepository.findByIdVisibleToUser(id, userId);
    }

    @Override
    public List<Payment> findVisibleToUser(Long userId) {
        return paymentRepository.findVisibleToUser(userId);
    }

    @Override
    public List<Payment> findByReservationIdVisibleToUser(Long reservationId, Long userId) {
        return paymentRepository.findByReservationIdVisibleToUser(reservationId, userId);
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }
}
