package com.clean.it.repository;

import com.clean.it.domain.NotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox,Long> {
    @Modifying
    @Query(value="""
        INSERT INTO notification_outbox(event_key,event_type,recipient_user_id,channel,subject,body,status,available_at)
        VALUES (:eventKey,:eventType,:recipientUserId,:channel,:subject,:body,'PENDING',:availableAt)
        ON CONFLICT (event_key) DO NOTHING
        """,nativeQuery=true)
    int enqueue(@Param("eventKey") String eventKey,
                @Param("eventType") String eventType,
                @Param("recipientUserId") Long recipientUserId,
                @Param("channel") String channel,
                @Param("subject") String subject,
                @Param("body") String body,
                @Param("availableAt") Instant availableAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select n from NotificationOutbox n
        where ((n.status in ('PENDING','FAILED') and n.availableAt <= :now)
            or (n.status = 'PROCESSING' and n.claimedAt < :leaseExpired))
        order by n.id
        """)
    List<NotificationOutbox> findClaimable(@Param("now") Instant now,
                                           @Param("leaseExpired") Instant leaseExpired,
                                           Pageable pageable);

    List<NotificationOutbox> findTop50ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
