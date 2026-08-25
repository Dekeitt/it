package com.clean.it.service;

import com.clean.it.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class ReservationReminderScheduler {
    private final ReservationRepository reservations;private final MarketplaceNotificationService notifications;private final boolean enabled;
    public ReservationReminderScheduler(ReservationRepository reservations,MarketplaceNotificationService notifications,@Value("${notifications.reminders.enabled:false}") boolean enabled){this.reservations=reservations;this.notifications=notifications;this.enabled=enabled;}
    @Scheduled(fixedDelayString="${notifications.reminders.delay-ms:3600000}") @Transactional
    public void enqueueUpcoming(){if(!enabled)return;Instant target=Instant.now().plus(Duration.ofHours(24));Instant from=target.minus(Duration.ofMinutes(30)),to=target.plus(Duration.ofMinutes(30));reservations.findByStatusIgnoreCaseAndStartAtBetween("SCHEDULED",from,to).forEach(notifications::reminder24h);}
}
