package com.clean.it.service;

import com.clean.it.domain.NotificationOutbox;
import com.clean.it.domain.WebPushSubscription;
import com.clean.it.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interaso.webpush.VapidKeys;
import com.interaso.webpush.WebPush;
import com.interaso.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebPushNotificationSender {
    private static final Logger log=LoggerFactory.getLogger(WebPushNotificationSender.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private final WebPushSubscriptionRepository subscriptions;
    private final NotificationPreferenceService preferences;
    private final boolean enabled;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public WebPushNotificationSender(WebPushSubscriptionRepository subscriptions,
                                     NotificationPreferenceService preferences,
                                     @Value("${notifications.push.enabled:false}") boolean enabled,
                                     @Value("${notifications.push.public-key:}") String publicKey,
                                     @Value("${notifications.push.private-key:}") String privateKey,
                                     @Value("${notifications.push.subject:mailto:admin@cleanit.local}") String subject){
        this.subscriptions=subscriptions;this.preferences=preferences;this.enabled=enabled;this.publicKey=publicKey;this.privateKey=privateKey;this.subject=subject;
    }

    @Transactional
    public EmailNotificationSender.DeliveryResult send(NotificationOutbox item){
        if(!enabled||publicKey.isBlank()||privateKey.isBlank()||!preferences.pushEnabled(item.getRecipientUserId())) return EmailNotificationSender.DeliveryResult.SKIPPED;
        List<WebPushSubscription> active=subscriptions.findByUserIdAndDisabledAtIsNull(item.getRecipientUserId());
        if(active.isEmpty()) return EmailNotificationSender.DeliveryResult.SKIPPED;
        WebPushService service=new WebPushService(subject,VapidKeys.fromUncompressedBytes(publicKey,privateKey));
        String payload=payload(item);
        RuntimeException lastFailure=null;boolean delivered=false;
        for(WebPushSubscription subscription:active){
            try{
                WebPush.SubscriptionState state=service.send(payload,subscription.getEndpoint(),subscription.getP256dh(),subscription.getAuthSecret(),86400,null,WebPush.Urgency.Normal);
                if(state==WebPush.SubscriptionState.EXPIRED){subscription.setDisabledAt(Instant.now());}
                else {subscription.setLastSuccessAt(Instant.now());delivered=true;}
                subscriptions.save(subscription);
            }catch(Exception exception){lastFailure=new IllegalStateException("Web Push delivery failed",exception);log.warn("Web Push failed for subscription {}",subscription.getId(),exception);}
        }
        if(delivered) return EmailNotificationSender.DeliveryResult.SENT;
        if(lastFailure!=null) throw lastFailure;
        return EmailNotificationSender.DeliveryResult.SKIPPED;
    }

    private String payload(NotificationOutbox item){
        Map<String,Object> value=new LinkedHashMap<>();value.put("title",item.getSubject());value.put("body",item.getBody());value.put("url","/notifications");
        try{return JSON.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize Web Push payload",e);}
    }
}
