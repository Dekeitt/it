package com.clean.it.service;

import com.clean.it.domain.NotificationOutbox;
import com.clean.it.dto.NotificationDtos.NotificationResponse;
import com.clean.it.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationOutboxService {
    private final NotificationOutboxRepository repository;
    public NotificationOutboxService(NotificationOutboxRepository repository){this.repository=repository;}

    @Transactional
    public void enqueueUserEvent(String baseKey,String eventType,Long userId,String subject,String emailBody,String pushBody){
        Instant now=Instant.now();
        repository.enqueue(baseKey+":email",eventType,userId,"EMAIL",subject,emailBody,now);
        repository.enqueue(baseKey+":push",eventType,userId,"WEB_PUSH",subject,pushBody,now);
    }

    @Transactional(readOnly=true)
    public List<NotificationResponse> recent(Long userId){
        return repository.findTop50ByRecipientUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    private NotificationResponse toDto(NotificationOutbox item){
        NotificationResponse response=new NotificationResponse();
        response.setId(item.getId());response.setEventType(item.getEventType());response.setChannel(item.getChannel());response.setSubject(item.getSubject());response.setBody(item.getBody());response.setStatus(item.getStatus());response.setCreatedAt(item.getCreatedAt());response.setSentAt(item.getSentAt());return response;
    }
}
