package com.clean.it.service;

import com.clean.it.domain.NotificationOutbox;
import com.clean.it.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

@Service
public class NotificationWorker {
    private static final Logger log=LoggerFactory.getLogger(NotificationWorker.class);
    private final NotificationOutboxRepository repository;
    private final EmailNotificationSender emailSender;
    private final WebPushNotificationSender pushSender;
    private final TransactionTemplate transactions;
    private final boolean enabled;

    public NotificationWorker(NotificationOutboxRepository repository,EmailNotificationSender emailSender,
                              WebPushNotificationSender pushSender,PlatformTransactionManager tx,
                              @Value("${notifications.worker.enabled:false}") boolean enabled){
        this.repository=repository;this.emailSender=emailSender;this.pushSender=pushSender;this.transactions=new TransactionTemplate(tx);this.enabled=enabled;
    }

    @Scheduled(fixedDelayString="${notifications.worker.delay-ms:5000}")
    public void drain(){if(!enabled)return;for(int i=0;i<20;i++){NotificationOutbox item=claim();if(item==null)return;deliver(item);}}

    NotificationOutbox claim(){return transactions.execute(status->{Instant now=Instant.now();var items=repository.findClaimable(now,now.minus(Duration.ofMinutes(10)),PageRequest.of(0,1));if(items.isEmpty())return null;NotificationOutbox item=items.get(0);item.setStatus("PROCESSING");item.setClaimedAt(now);item.setAttempts(item.getAttempts()+1);return repository.save(item);});}

    void deliver(NotificationOutbox claimed){
        try{
            var result="EMAIL".equals(claimed.getChannel())?emailSender.send(claimed):pushSender.send(claimed);
            transactions.executeWithoutResult(status->{NotificationOutbox item=repository.findById(claimed.getId()).orElseThrow();item.setStatus(result==EmailNotificationSender.DeliveryResult.SENT?"SENT":"SKIPPED");item.setSentAt(Instant.now());item.setLastError(null);repository.save(item);});
        }catch(RuntimeException ex){
            log.warn("Notification outbox {} delivery failed",claimed.getId(),ex);
            transactions.executeWithoutResult(status->{NotificationOutbox item=repository.findById(claimed.getId()).orElseThrow();item.setStatus(item.getAttempts()>=6?"FAILED":"PENDING");item.setAvailableAt(Instant.now().plus(Duration.ofMinutes(Math.min(60,1L<<Math.min(5,item.getAttempts())))));item.setLastError(safe(ex));repository.save(item);});
        }
    }
    private String safe(Throwable ex){String message=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();return message.length()>1000?message.substring(0,1000):message;}
}
