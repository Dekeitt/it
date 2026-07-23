package com.clean.it.service.impl;

import com.clean.it.domain.Review;
import com.clean.it.dto.AppDtos.ReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.repository.ReviewRepository;
import com.clean.it.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public ReviewResponse addReview(String clientEmail, ReviewRequest req) {
        Review r = new Review();
        r.setCleanerEmail(req.getCleanerEmail());
        r.setClientEmail(clientEmail);
        r.setRating(req.getRating());
        r.setComment(req.getComment());
        Review saved = reviewRepository.save(r);
        return toDto(saved);
    }

    @Override
    public List<ReviewResponse> listReviews(String cleanerEmail) {
        return reviewRepository.findByCleanerEmailOrderByCreatedAtDesc(cleanerEmail).stream().map(this::toDto).collect(Collectors.toList());
    }

    private ReviewResponse toDto(Review r) {
        ReviewResponse resp = new ReviewResponse();
        resp.setId(r.getId());
        resp.setCleanerEmail(r.getCleanerEmail());
        resp.setClientEmail(r.getClientEmail());
        resp.setRating(r.getRating());
        resp.setComment(r.getComment());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}

