package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.Reservation;
import com.clean.it.domain.Review;
import com.clean.it.dto.AppDtos.ReservationReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.repository.ReviewRepository;
import com.clean.it.service.ReviewService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final CleanerRepository cleanerRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ReservationRepository reservationRepository,
                             CleanerRepository cleanerRepository) {
        this.reviewRepository = reviewRepository;
        this.reservationRepository = reservationRepository;
        this.cleanerRepository = cleanerRepository;
    }

    @Override
    @Transactional
    public ReviewResponse addReviewForReservation(Long clientId, String clientEmail, Long reservationId,
                                                  ReservationReviewRequest request) {
        Reservation reservation = reservationRepository.findLockedById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (!Objects.equals(reservation.getClientId(), clientId)) {
            throw new AccessDeniedException("Only the reservation client can review it");
        }
        if (!"COMPLETED".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("A review can only be created after the reservation is completed");
        }
        if (reviewRepository.existsByReservationId(reservationId)) {
            throw new IllegalStateException("This reservation already has a review");
        }

        Review review = new Review();
        review.setReservationId(reservationId);
        review.setCleanerId(reservation.getCleanerId());
        review.setCleanerEmail(reservation.getCleanerEmail());
        review.setClientId(clientId);
        review.setClientEmail(clientEmail);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);
        refreshCleanerRating(reservation.getCleanerId());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews(String cleanerEmail) {
        Cleaner cleaner = cleanerRepository.findByEmailIgnoreCase(cleanerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
        return reviewRepository.findByCleanerIdOrderByCreatedAtDesc(cleaner.getUserId())
                .stream().map(this::toDto).toList();
    }

    private void refreshCleanerRating(Long cleanerId) {
        Double average = reviewRepository.averageRating(cleanerId);
        cleanerRepository.findFirstByUserId(cleanerId).ifPresent(cleaner -> {
            cleaner.setRating(average == null ? 0.0 : Math.round(average * 100.0) / 100.0);
            cleanerRepository.save(cleaner);
        });
    }

    private ReviewResponse toDto(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setReservationId(review.getReservationId());
        response.setCleanerEmail(review.getCleanerEmail());
        response.setClientEmail(review.getClientEmail());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
