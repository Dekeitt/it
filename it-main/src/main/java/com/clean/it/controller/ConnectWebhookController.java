package com.clean.it.controller;

import com.clean.it.service.StripeConnectWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/connect")
public class ConnectWebhookController {
 private static final Logger log=LoggerFactory.getLogger(ConnectWebhookController.class);private final StripeConnectWebhookService service;private final String secret;
 public ConnectWebhookController(StripeConnectWebhookService service,@Value("${stripe.connect-webhook-secret:}") String secret){this.service=service;this.secret=secret;}
 @PostMapping("/webhook") @Hidden
 public ResponseEntity<?> webhook(@RequestHeader(value="Stripe-Signature",required=false) String signature,@RequestBody String payload){if(secret.isBlank())return ResponseEntity.status(503).body(Map.of("error","connect webhook verification unavailable"));if(signature==null||signature.isBlank())return ResponseEntity.badRequest().body(Map.of("error","missing Stripe-Signature"));try{Event event=Webhook.constructEvent(payload,signature,secret);boolean processed=service.process(event);return ResponseEntity.ok(Map.of("received",true,"duplicate",!processed));}catch(SignatureVerificationException|IllegalArgumentException e){log.warn("Rejected Stripe Connect webhook: {}",e.getMessage());return ResponseEntity.badRequest().body(Map.of("error","invalid webhook"));}catch(Exception e){log.error("Failed to process Stripe Connect webhook",e);return ResponseEntity.internalServerError().body(Map.of("error","webhook processing failed"));}}
}
