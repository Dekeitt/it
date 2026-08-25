package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReservationReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse addReviewForReservation(Long clientId, String clientEmail, Long reservationId, ReservationReviewRequest request);
    List<ReviewResponse> listReviews(String cleanerEmail);
}
