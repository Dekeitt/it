package com.clean.it.service.impl;

import com.clean.it.domain.Payment;
import com.clean.it.domain.PaymentEvent;
import com.clean.it.repository.PaymentEventRepository;
import com.clean.it.repository.PaymentRepository;
import com.clean.it.service.PaymentStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentStoreJpa implements PaymentStore {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentStoreJpa(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Override
    public Optional<Payment> findByStripePaymentIntentId(String id) {
        return paymentRepository.findByStripePaymentIntentId(id);
    }

    @Override
    public Payment savePayment(Payment p) {
        return paymentRepository.save(p);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    public List<Payment> findByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    public boolean eventExists(String eventId) {
        return paymentEventRepository.existsByEventId(eventId);
    }

    @Override
    public void saveEvent(String eventId, String type) {
        PaymentEvent pe = new PaymentEvent();
        pe.setEventId(eventId);
        pe.setType(type);
        paymentEventRepository.save(pe);
    }
}

