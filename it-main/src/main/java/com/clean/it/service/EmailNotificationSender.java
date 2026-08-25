package com.clean.it.service;

import com.clean.it.domain.NotificationOutbox;
import com.clean.it.domain.UserAccount;
import com.clean.it.repository.UserAccountRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final UserAccountRepository users;
    private final NotificationPreferenceService preferences;
    private final boolean enabled;
    private final String from;

    public EmailNotificationSender(ObjectProvider<JavaMailSender> mailSender,
                                   UserAccountRepository users,
                                   NotificationPreferenceService preferences,
                                   @Value("${notifications.email.enabled:false}") boolean enabled,
                                   @Value("${notifications.email.from:no-reply@cleanit.local}") String from){
        this.mailSender=mailSender;this.users=users;this.preferences=preferences;this.enabled=enabled;this.from=from;
    }

    public DeliveryResult send(NotificationOutbox item){
        if(!enabled||!preferences.emailEnabled(item.getRecipientUserId())) return DeliveryResult.SKIPPED;
        UserAccount user=users.findById(item.getRecipientUserId()).orElseThrow(()->new IllegalStateException("Notification recipient not found"));
        if(user.getEmail()==null||user.getEmail().isBlank()) return DeliveryResult.SKIPPED;
        JavaMailSender sender=mailSender.getIfAvailable();
        if(sender==null) throw new IllegalStateException("Email notifications are enabled but SMTP is not configured");
        SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(user.getEmail());message.setSubject(item.getSubject());message.setText(item.getBody());sender.send(message);return DeliveryResult.SENT;
    }

    public enum DeliveryResult { SENT, SKIPPED }
}
