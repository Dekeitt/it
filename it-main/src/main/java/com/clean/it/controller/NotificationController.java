package com.clean.it.controller;

import com.clean.it.dto.NotificationDtos.*;import com.clean.it.security.AuthenticatedUser;import com.clean.it.service.NotificationOutboxService;import com.clean.it.service.NotificationPreferenceService;import com.clean.it.service.WebPushSubscriptionService;import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.tags.Tag;import jakarta.validation.Valid;import org.springframework.http.ResponseEntity;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.List;

@RestController @RequestMapping("/api/notifications") @Tag(name="Notifications",description="Preferencias, historial y suscripciones push")
public class NotificationController {
 private final AuthenticatedUser user;private final NotificationOutboxService outbox;private final NotificationPreferenceService preferences;private final WebPushSubscriptionService push;
 public NotificationController(AuthenticatedUser user,NotificationOutboxService outbox,NotificationPreferenceService preferences,WebPushSubscriptionService push){this.user=user;this.outbox=outbox;this.preferences=preferences;this.push=push;}
 @GetMapping @Operation(summary="Listar notificaciones recientes del usuario") public ResponseEntity<List<NotificationResponse>> recent(Authentication a){return ResponseEntity.ok(outbox.recent(user.id(a)));}
 @GetMapping("/preferences") public ResponseEntity<PreferenceResponse> preferences(Authentication a){return ResponseEntity.ok(preferences.get(user.id(a)));}
 @PutMapping("/preferences") public ResponseEntity<PreferenceResponse> update(Authentication a,@RequestBody PreferenceRequest r){return ResponseEntity.ok(preferences.update(user.id(a),r));}
 @GetMapping("/push/config") public ResponseEntity<PushConfigResponse> pushConfig(){return ResponseEntity.ok(push.config());}
 @PostMapping("/push/subscriptions") public ResponseEntity<Void> subscribe(Authentication a,@Valid @RequestBody PushSubscriptionRequest r){push.subscribe(user.id(a),r);return ResponseEntity.noContent().build();}
 @PostMapping("/push/unsubscribe") public ResponseEntity<Void> unsubscribe(Authentication a,@Valid @RequestBody PushUnsubscribeRequest r){push.unsubscribe(user.id(a),r.getEndpoint());return ResponseEntity.noContent().build();}
}
