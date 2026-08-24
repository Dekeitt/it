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
@RequestMapping("/api/cleaners")
@Tag(name = "Reviews", description = "Reseñas verificadas asociadas a cleaners")
public class ReviewController {
    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService) { this.reviewService = reviewService; }

    @GetMapping("/{email}/reviews")
    @Operation(summary = "Listar reseñas verificadas de un cleaner")
    public ResponseEntity<List<ReviewResponse>> list(@PathVariable("email") String email) {
        return ResponseEntity.ok(reviewService.listReviews(email));
    }
}
