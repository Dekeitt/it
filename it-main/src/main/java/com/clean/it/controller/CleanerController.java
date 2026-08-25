package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.AppDtos.AvailabilitySlotRequest;
import com.clean.it.dto.AppDtos.AvailabilitySlotResponse;
import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.dto.BookingDtos.CleanerOfferingRequest;
import com.clean.it.dto.BookingDtos.CleanerOfferingResponse;
import com.clean.it.dto.BookingDtos.ServiceAreaRequest;
import com.clean.it.dto.BookingDtos.ServiceAreaResponse;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.BookingService;
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
@Tag(name = "Cleaners", description = "Gestión, servicios, cobertura y disponibilidad de cleaners")
@Validated
public class CleanerController {

    private final CleanerService cleanerService;
    private final CleanerAvailabilityService availabilityService;
    private final BookingService bookingService;
    private final AuthenticatedUser authenticatedUser;

    public CleanerController(CleanerService cleanerService,
                             CleanerAvailabilityService availabilityService,
                             BookingService bookingService,
                             AuthenticatedUser authenticatedUser) {
        this.cleanerService = cleanerService;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping
    @Operation(summary = "Listar cleaners")
    public ResponseEntity<List<CleanerDto>> listCleaners() {
        return ResponseEntity.ok(cleanerService.listCleaners());
    }

    @PostMapping
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Crear o actualizar el perfil del cleaner autenticado")
    public ResponseEntity<CleanerDto> createCleaner(Authentication authentication, @RequestBody CleanerDto dto) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(cleanerService.createCleaner(account.getId(), account.getEmail(), dto));
    }

    @GetMapping("/{email}/availability")
    @Operation(summary = "Consultar disponibilidad recurrente de un cleaner")
    public ResponseEntity<List<AvailabilitySlotResponse>> availability(@PathVariable String email) {
        return ResponseEntity.ok(availabilityService.list(email));
    }

    @GetMapping("/{email}/services")
    @Operation(summary = "Consultar servicios y tarifas de un cleaner")
    public ResponseEntity<List<CleanerOfferingResponse>> services(@PathVariable String email) {
        return ResponseEntity.ok(bookingService.offeringsForCleanerEmail(email));
    }

    @GetMapping("/{email}/service-areas")
    @Operation(summary = "Consultar zonas de servicio de un cleaner")
    public ResponseEntity<List<ServiceAreaResponse>> serviceAreas(@PathVariable String email) {
        return ResponseEntity.ok(bookingService.serviceAreasForCleanerEmail(email));
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Reemplazar la disponibilidad recurrente del cleaner autenticado")
    public ResponseEntity<List<AvailabilitySlotResponse>> replaceAvailability(
            Authentication authentication,
            @Valid @RequestBody List<AvailabilitySlotRequest> slots) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(availabilityService.replace(account.getId(), account.getEmail(), slots));
    }

    @PutMapping("/me/services")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Reemplazar servicios y tarifas del cleaner autenticado")
    public ResponseEntity<List<CleanerOfferingResponse>> replaceServices(
            Authentication authentication,
            @Valid @RequestBody List<CleanerOfferingRequest> requests) {
        return ResponseEntity.ok(bookingService.replaceOfferings(authenticatedUser.id(authentication), requests));
    }

    @PutMapping("/me/service-areas")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Reemplazar zonas de servicio del cleaner autenticado")
    public ResponseEntity<List<ServiceAreaResponse>> replaceServiceAreas(
            Authentication authentication,
            @Valid @RequestBody List<ServiceAreaRequest> requests) {
        return ResponseEntity.ok(bookingService.replaceServiceAreas(authenticatedUser.id(authentication), requests));
    }

    @GetMapping("/available")
    @Operation(summary = "Buscar cleaners disponibles en un intervalo")
    public ResponseEntity<List<CleanerDto>> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam @Min(30) @Max(1440) int durationMinutes) {
        return ResponseEntity.ok(availabilityService.findAvailable(startAt, durationMinutes));
    }
}
