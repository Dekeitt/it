package com.clean.it.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public static class PreferenceResponse {
        private boolean emailEnabled;
        private boolean pushEnabled;
        public boolean isEmailEnabled(){return emailEnabled;} public void setEmailEnabled(boolean value){emailEnabled=value;}
        public boolean isPushEnabled(){return pushEnabled;} public void setPushEnabled(boolean value){pushEnabled=value;}
    }
    public static class PreferenceRequest {
        private boolean emailEnabled=true;
        private boolean pushEnabled=true;
        public boolean isEmailEnabled(){return emailEnabled;} public void setEmailEnabled(boolean value){emailEnabled=value;}
        public boolean isPushEnabled(){return pushEnabled;} public void setPushEnabled(boolean value){pushEnabled=value;}
    }
    public static class NotificationResponse {
        private Long id; private String eventType; private String channel; private String subject; private String body; private String status; private Instant createdAt; private Instant sentAt;
        public Long getId(){return id;} public void setId(Long id){this.id=id;}
        public String getEventType(){return eventType;} public void setEventType(String value){eventType=value;}
        public String getChannel(){return channel;} public void setChannel(String value){channel=value;}
        public String getSubject(){return subject;} public void setSubject(String value){subject=value;}
        public String getBody(){return body;} public void setBody(String value){body=value;}
        public String getStatus(){return status;} public void setStatus(String value){status=value;}
        public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant value){createdAt=value;}
        public Instant getSentAt(){return sentAt;} public void setSentAt(Instant value){sentAt=value;}
    }
    public static class PushSubscriptionRequest {
        @NotBlank @Size(max=2048) private String endpoint;
        @NotNull private Keys keys;
        public String getEndpoint(){return endpoint;} public void setEndpoint(String endpoint){this.endpoint=endpoint;}
        public Keys getKeys(){return keys;} public void setKeys(Keys keys){this.keys=keys;}
        public static class Keys {
            @NotBlank @Size(max=255) private String p256dh;
            @NotBlank @Size(max=255) private String auth;
            public String getP256dh(){return p256dh;} public void setP256dh(String value){p256dh=value;}
            public String getAuth(){return auth;} public void setAuth(String value){auth=value;}
        }
    }
    public static class PushConfigResponse {
        private boolean enabled; private String publicKey;
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean enabled){this.enabled=enabled;}
        public String getPublicKey(){return publicKey;} public void setPublicKey(String publicKey){this.publicKey=publicKey;}
    }
}
