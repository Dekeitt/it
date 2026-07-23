package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import java.util.List;

public interface ReviewService {
    ReviewResponse addReview(String clientEmail, ReviewRequest req);
    List<ReviewResponse> listReviews(String cleanerEmail);
}

