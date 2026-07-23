package com.clean.it.repository;

import com.clean.it.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    Optional<PaymentEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}

