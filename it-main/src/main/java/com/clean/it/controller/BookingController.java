package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.dto.BookingDtos.AddressRequest;
import com.clean.it.dto.BookingDtos.AddressResponse;
import com.clean.it.dto.BookingDtos.AvailableCleanerResponse;
import com.clean.it.dto.BookingDtos.DirectBookingRequest;
import com.clean.it.dto.BookingDtos.ServiceTypeResponse;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.BookingService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/booking")
@Tag(name = "Direct booking", description = "Reserva directa sin IDs técnicos de jobs")
@Validated
public class BookingController {
    private final BookingService bookingService;
    private final AuthenticatedUser authenticatedUser;

    public BookingController(BookingService bookingService, AuthenticatedUser authenticatedUser) {
        this.bookingService = bookingService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/catalog")
    @Operation(summary = "Consultar catálogo de servicios reservables")
    public ResponseEntity<List<ServiceTypeResponse>> catalog() {
        return ResponseEntity.ok(bookingService.catalog());
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Listar direcciones del cliente autenticado")
    public ResponseEntity<List<AddressResponse>> addresses(Authentication authentication) {
        return ResponseEntity.ok(bookingService.addresses(authenticatedUser.id(authentication)));
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Guardar una dirección estructurada")
    public ResponseEntity<AddressResponse> createAddress(Authentication authentication,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(bookingService.createAddress(authenticatedUser.id(authentication), request));
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Buscar cleaners por servicio, zona y horario")
    public ResponseEntity<List<AvailableCleanerResponse>> available(
            Authentication authentication,
            @RequestParam String serviceCode,
            @RequestParam Long addressId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam @Min(30) @Max(1440) int durationMinutes) {
        return ResponseEntity.ok(bookingService.available(authenticatedUser.id(authentication), serviceCode,
                addressId, startAt, durationMinutes));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear una reserva directa con precio calculado en servidor")
    public ResponseEntity<ReservationResponse> book(Authentication authentication,
                                                    @Valid @RequestBody DirectBookingRequest request) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(bookingService.book(account.getId(), account.getEmail(), request));
    }
}
