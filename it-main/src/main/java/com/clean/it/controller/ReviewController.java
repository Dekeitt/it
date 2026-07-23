package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/cleaners")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{email}/reviews")
    public ResponseEntity<ReviewResponse> addReview(Authentication authentication, @PathVariable("email") String email, @Valid @RequestBody ReviewRequest req) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        // ignore path email and use req.cleanerEmail for flexibility
        ReviewResponse resp = reviewService.addReview(authentication.getName(), req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{email}/reviews")
    public ResponseEntity<List<ReviewResponse>> list(@PathVariable("email") String email) {
        return ResponseEntity.ok(reviewService.listReviews(email));
    }
}

