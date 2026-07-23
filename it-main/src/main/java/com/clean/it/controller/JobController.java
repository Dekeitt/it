package com.clean.it.controller;

import com.clean.it.dto.JobDtos.CreateJobRequest;
import com.clean.it.dto.JobDtos.JobResponse;
import com.clean.it.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Operaciones de creación y asignación de jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear un job")
    public ResponseEntity<?> createJob(Authentication authentication, @Valid @RequestBody CreateJobRequest req) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Unauthenticated request: missing principal (ensure BasicAuth or Bearer token is provided)"));
        }
        String email = authentication.getName();
        JobResponse resp = jobService.createJob(email, req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('CLIENT','CLEANER')")
    @Operation(summary = "Listar jobs abiertos")
    public ResponseEntity<List<JobResponse>> listOpenJobs() {
        return ResponseEntity.ok(jobService.listOpenJobs());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('CLEANER')")
    @Operation(summary = "Aceptar un job")
    public ResponseEntity<?> acceptJob(Authentication authentication, @PathVariable("id") Long id) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Unauthenticated request: missing principal (ensure BasicAuth or Bearer token is provided)"));
        }
        String cleaner = authentication.getName();
        JobResponse resp = jobService.acceptJob(cleaner, id);
        return ResponseEntity.ok(resp);
    }
}
