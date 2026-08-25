package com.clean.it.controller;

import com.clean.it.domain.UserAccount;
import com.clean.it.dto.JobDtos.CreateJobRequest;
import com.clean.it.dto.JobDtos.JobResponse;
import com.clean.it.security.AuthenticatedUser;
import com.clean.it.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Operaciones de creación y asignación de jobs")
public class JobController {

    private final JobService jobService;
    private final AuthenticatedUser authenticatedUser;

    public JobController(JobService jobService, AuthenticatedUser authenticatedUser) {
        this.jobService = jobService;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear un job")
    public ResponseEntity<JobResponse> createJob(Authentication authentication,
                                                 @Valid @RequestBody CreateJobRequest req) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(jobService.createJob(account.getId(), account.getEmail(), req));
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
    public ResponseEntity<JobResponse> acceptJob(Authentication authentication, @PathVariable("id") Long id) {
        UserAccount account = authenticatedUser.account(authentication);
        return ResponseEntity.ok(jobService.acceptJob(account.getId(), account.getEmail(), id));
    }
}
