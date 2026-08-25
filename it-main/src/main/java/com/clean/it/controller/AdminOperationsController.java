package com.clean.it.controller;

import com.clean.it.dto.AdminDtos.*;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.AdminOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Tag(name="Operations admin",description="Soporte, moderación, auditoría y conciliación")
public class AdminOperationsController {
 private final AdminOperationsService service; private final AuthenticatedUser authenticatedUser;
 public AdminOperationsController(AdminOperationsService service,AuthenticatedUser authenticatedUser){this.service=service;this.authenticatedUser=authenticatedUser;}
 @GetMapping("/search") @Operation(summary="Buscar entidades operativas")
 public ResponseEntity<List<AdminSearchItem>> search(@RequestParam String type,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){return ResponseEntity.ok(service.search(type,q,limit));}
 @GetMapping("/reservations/{id}/timeline") @Operation(summary="Ver timeline y conciliación de una reserva")
 public ResponseEntity<ReservationTimeline> timeline(@PathVariable Long id){return ResponseEntity.ok(service.timeline(id));}
 @PostMapping("/users/{id}/block") @Operation(summary="Bloquear una cuenta con auditoría")
 public ResponseEntity<AdminActionResponse> block(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminActionRequest r){return ResponseEntity.ok(service.block(authenticatedUser.id(a),id,r));}
 @PostMapping("/users/{id}/unblock") @Operation(summary="Desbloquear una cuenta con auditoría")
 public ResponseEntity<AdminActionResponse> unblock(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminActionRequest r){return ResponseEntity.ok(service.unblock(authenticatedUser.id(a),id,r));}
 @PostMapping("/reviews/{id}/moderate") @Operation(summary="Ocultar o restaurar una reseña")
 public ResponseEntity<AdminActionResponse> moderate(Authentication a,@PathVariable Long id,@Valid @RequestBody ReviewModerationRequest r){return ResponseEntity.ok(service.moderateReview(authenticatedUser.id(a),id,r));}
 @PostMapping("/reservations/{id}/cancel") @Operation(summary="Cancelar una reserva desde operaciones")
 public ResponseEntity<AdminActionResponse> cancel(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminActionRequest r){return ResponseEntity.ok(service.cancelReservation(authenticatedUser.id(a),id,r));}
 @PostMapping("/reservations/{id}/refund") @Operation(summary="Cancelar o reembolsar el pago de una reserva")
 public ResponseEntity<AdminActionResponse> refund(Authentication a,@PathVariable Long id,@Valid @RequestBody AdminActionRequest r){return ResponseEntity.ok(service.refundReservation(authenticatedUser.id(a),id,r));}
}
