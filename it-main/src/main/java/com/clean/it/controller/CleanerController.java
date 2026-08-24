package com.clean.it.controller;

import com.clean.it.dto.AppDtos.AvailabilitySlotRequest;
import com.clean.it.dto.AppDtos.AvailabilitySlotResponse;
import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.CleanerAvailabilityService;
import com.clean.it.service.CleanerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/cleaners")
@Tag(name = "Cleaners", description = "Gestión y disponibilidad de cleaners")
@Validated
public class CleanerController {

    private final CleanerService cleanerService;
    private final CleanerAvailabilityService availabilityService;
    private final AuthenticatedUser authenticatedUser;

    public CleanerController(CleanerService cleanerService,
                             CleanerAvailabilityService availabilityService,
                             AuthenticatedUser authenticatedUser) {
        this.cleanerService = cleanerService;
        this.availabilityService = availabilityService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping
    @Operation(summary = "Listar cleaners")
    public ResponseEntity<List<CleanerDto>> listCleaners() {
        return ResponseEntity.ok(cleanerService.listCleaners());
    }

    @PostMapping
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Crear un perfil de cleaner")
    public ResponseEntity<CleanerDto> createCleaner(Authentication authentication, @RequestBody CleanerDto dto) {
        dto.setEmail(authenticatedUser.email(authentication));
        return ResponseEntity.ok(cleanerService.createCleaner(dto));
    }

    @GetMapping("/{email}/availability")
    @Operation(summary = "Consultar disponibilidad recurrente de un cleaner")
    public ResponseEntity<List<AvailabilitySlotResponse>> availability(@PathVariable String email) {
        return ResponseEntity.ok(availabilityService.list(email));
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Reemplazar la disponibilidad recurrente del cleaner autenticado")
    public ResponseEntity<List<AvailabilitySlotResponse>> replaceAvailability(
            Authentication authentication,
            @Valid @RequestBody List<AvailabilitySlotRequest> slots) {
        return ResponseEntity.ok(availabilityService.replace(authenticatedUser.email(authentication), slots));
    }

    @GetMapping("/available")
    @Operation(summary = "Buscar cleaners disponibles en un intervalo")
    public ResponseEntity<List<CleanerDto>> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam @Min(30) @Max(1440) int durationMinutes) {
        return ResponseEntity.ok(availabilityService.findAvailable(startAt, durationMinutes));
    }
}
