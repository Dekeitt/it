package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews API", description = "Interfaz alternativa para crear y consultar reseñas")
public class ReviewApiController {

    private final ReviewService reviewService;

    public ReviewApiController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Crear una reseña")
    public ResponseEntity<ReviewResponse> addReview(Authentication authentication, @Valid @RequestBody ReviewRequest req) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        ReviewResponse resp = reviewService.addReview(authentication.getName(), req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{email}")
    @Operation(summary = "Listar reseñas de un cleaner")
    public ResponseEntity<List<ReviewResponse>> list(@PathVariable("email") String email) {
        return ResponseEntity.ok(reviewService.listReviews(email));
    }
}
