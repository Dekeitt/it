package com.clean.it.repository;

import com.clean.it.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO payment_events(event_id, type, status, event_created_at, claimed_at)
            VALUES (:eventId, :type, 'PROCESSING', :eventCreatedAt, :claimedAt)
            ON CONFLICT (event_id) DO UPDATE
               SET type = EXCLUDED.type,
                   status = 'PROCESSING',
                   event_created_at = EXCLUDED.event_created_at,
                   claimed_at = EXCLUDED.claimed_at,
                   processed_at = NULL,
                   failure_reason = NULL
             WHERE payment_events.status = 'FAILED'
                OR (payment_events.status = 'PROCESSING'
                    AND payment_events.claimed_at < :staleBefore)
            """, nativeQuery = true)
    int claim(@Param("eventId") String eventId,
              @Param("type") String type,
              @Param("eventCreatedAt") Instant eventCreatedAt,
              @Param("claimedAt") Instant claimedAt,
              @Param("staleBefore") Instant staleBefore);

    @Modifying
    @Query(value = "UPDATE payment_events SET payment_id=:paymentId, stripe_payment_intent_id=:intentId WHERE event_id=:eventId AND status='PROCESSING'", nativeQuery = true)
    int linkPayment(@Param("eventId") String eventId,@Param("paymentId") Long paymentId,@Param("intentId") String intentId);

    List<PaymentEvent> findByPaymentIdOrderByEventCreatedAtAsc(Long paymentId);

    @Modifying
    @Query(value = """
            UPDATE payment_events
               SET status = 'PROCESSED', processed_at = :processedAt, failure_reason = NULL
             WHERE event_id = :eventId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markProcessed(@Param("eventId") String eventId, @Param("processedAt") Instant processedAt);

    @Modifying
    @Query(value = """
            UPDATE payment_events
               SET status = 'FAILED', failure_reason = :failureReason
             WHERE event_id = :eventId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markFailed(@Param("eventId") String eventId, @Param("failureReason") String failureReason);
}
