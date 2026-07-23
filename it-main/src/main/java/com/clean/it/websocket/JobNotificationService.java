package com.clean.it.websocket;

import com.clean.it.dto.JobDtos.JobResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JobNotificationService {

    private final SimpMessagingTemplate template;
    private static final Logger log = LoggerFactory.getLogger(JobNotificationService.class);

    public JobNotificationService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publishJobUpdate(JobResponse job) {
        // Publish to a topic all clients can subscribe to for job updates
        try {
            template.convertAndSend("/topic/jobs", job);
        } catch (Exception ex) {
            // Log and continue - notifications must not break business logic
            log.warn("Failed to publish job update for id={}: {}", job.getId(), ex.getMessage(), ex);
        }
    }
}

