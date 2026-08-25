package com.clean.it.repository;

import com.clean.it.domain.ConnectEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface ConnectEventRepository extends JpaRepository<ConnectEvent,Long> {
 @Modifying
 @Query(value="""
  INSERT INTO connect_events(stripe_event_id,event_type,stripe_account_id,event_created_at,status,attempts,claimed_at)
  VALUES (:eventId,:eventType,:accountId,:eventCreatedAt,'PROCESSING',1,:claimedAt)
  ON CONFLICT (stripe_event_id) DO UPDATE
    SET event_type=EXCLUDED.event_type,stripe_account_id=EXCLUDED.stripe_account_id,
        event_created_at=EXCLUDED.event_created_at,status='PROCESSING',
        attempts=connect_events.attempts+1,claimed_at=EXCLUDED.claimed_at,
        processed_at=NULL,failure_reason=NULL
  WHERE connect_events.attempts < 6
    AND (connect_events.status='FAILED'
      OR (connect_events.status='PROCESSING' AND connect_events.claimed_at < :staleBefore))
 """,nativeQuery=true)
 int claim(@Param("eventId") String eventId,@Param("eventType") String eventType,@Param("accountId") String accountId,@Param("eventCreatedAt") Instant eventCreatedAt,@Param("claimedAt") Instant claimedAt,@Param("staleBefore") Instant staleBefore);
 @Modifying @Query(value="UPDATE connect_events SET status='PROCESSED',processed_at=:processedAt,failure_reason=NULL WHERE stripe_event_id=:eventId AND status='PROCESSING'",nativeQuery=true)
 int markProcessed(@Param("eventId") String eventId,@Param("processedAt") Instant processedAt);
 @Modifying @Query(value="UPDATE connect_events SET status='FAILED',failure_reason=:reason WHERE stripe_event_id=:eventId AND status='PROCESSING'",nativeQuery=true)
 int markFailed(@Param("eventId") String eventId,@Param("reason") String reason);
}
