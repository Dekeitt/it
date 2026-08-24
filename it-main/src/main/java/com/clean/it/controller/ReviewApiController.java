package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews API", description = "Consulta de reseñas verificadas")
public class ReviewApiController {
    private final ReviewService reviewService;
    public ReviewApiController(ReviewService reviewService) { this.reviewService = reviewService; }

    @GetMapping("/{email}")
    @Operation(summary = "Listar reseñas verificadas de un cleaner")
    public ResponseEntity<List<ReviewResponse>> list(@PathVariable("email") String email) {
        return ResponseEntity.ok(reviewService.listReviews(email));
    }
}
