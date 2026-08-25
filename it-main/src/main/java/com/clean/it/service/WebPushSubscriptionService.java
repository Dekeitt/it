package com.clean.it.service;

import com.clean.it.domain.WebPushSubscription;
import com.clean.it.dto.NotificationDtos.PushConfigResponse;
import com.clean.it.dto.NotificationDtos.PushSubscriptionRequest;
import com.clean.it.repository.WebPushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class WebPushSubscriptionService {
    private final WebPushSubscriptionRepository repository;
    private final String publicKey;
    private final boolean enabled;

    public WebPushSubscriptionService(WebPushSubscriptionRepository repository,
                                      @Value("${notifications.push.public-key:}") String publicKey,
                                      @Value("${notifications.push.enabled:false}") boolean enabled){
        this.repository=repository;this.publicKey=publicKey;this.enabled=enabled;
    }

    public PushConfigResponse config(){PushConfigResponse response=new PushConfigResponse();response.setEnabled(enabled&&!publicKey.isBlank());response.setPublicKey(response.isEnabled()?publicKey:null);return response;}

    @Transactional
    public void subscribe(Long userId,PushSubscriptionRequest request){
        WebPushSubscription subscription=repository.findByEndpoint(request.getEndpoint()).orElseGet(WebPushSubscription::new);
        if(subscription.getUserId()!=null&&!Objects.equals(subscription.getUserId(),userId)) throw new IllegalArgumentException("Push endpoint already belongs to another user");
        subscription.setUserId(userId);subscription.setEndpoint(request.getEndpoint());subscription.setP256dh(request.getKeys().getP256dh());subscription.setAuthSecret(request.getKeys().getAuth());subscription.setDisabledAt(null);repository.save(subscription);
    }

    @Transactional
    public void unsubscribe(Long userId,String endpoint){
        repository.findByEndpoint(endpoint).filter(subscription->Objects.equals(subscription.getUserId(),userId)).ifPresent(subscription->{subscription.setDisabledAt(Instant.now());repository.save(subscription);});
    }
}
