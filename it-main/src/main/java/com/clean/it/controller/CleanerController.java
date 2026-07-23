package com.clean.it.controller;

import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.service.CleanerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/cleaners")
@Tag(name = "Cleaners", description = "Gestión de cleaners")
public class CleanerController {

    private final CleanerService cleanerService;

    public CleanerController(CleanerService cleanerService) {
        this.cleanerService = cleanerService;
    }

    @GetMapping
    @Operation(summary = "Listar cleaners")
    public ResponseEntity<List<CleanerDto>> listCleaners() {
        return ResponseEntity.ok(cleanerService.listCleaners());
    }

    @PostMapping
    @Operation(summary = "Crear un cleaner")
    public ResponseEntity<CleanerDto> createCleaner(@RequestBody CleanerDto dto) {
        CleanerDto created = cleanerService.createCleaner(dto);
        return ResponseEntity.ok(created);
    }
}
