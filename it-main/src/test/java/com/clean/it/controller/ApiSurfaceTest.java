package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReservationReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSurfaceTest {

    @Test
    void apiInfoEndpointStillExposesDocsAfterFrontendExtraction() {
        HomeController controller = new HomeController();

        var response = controller.info();

        assertThat(response).containsEntry("status", "ok");
        assertThat(response).containsEntry("name", "it API");
        assertThat(response.toString()).contains("swaggerUi");
        assertThat(response.toString()).contains("openApi");
    }

    @Test
    void reviewApiOnlyListsVerifiedReviews() {
        InMemoryReviewService service = new InMemoryReviewService();
        ReviewApiController controller = new ReviewApiController(service);
        service.addVerifiedReview(42L, "client@example.com", "cleaner@example.com", 5, "Excelente servicio");

        ResponseEntity<List<ReviewResponse>> listed = controller.list("cleaner@example.com");

        assertThat(listed.getStatusCode().value()).isEqualTo(200);
        assertThat(listed.getBody()).hasSize(1);
        assertThat(listed.getBody().get(0).getReservationId()).isEqualTo(42L);
        assertThat(listed.getBody().get(0).getComment()).isEqualTo("Excelente servicio");
    }

    static class InMemoryReviewService implements ReviewService {
        private final List<ReviewResponse> reviews = new ArrayList<>();

        void addVerifiedReview(long reservationId, String clientEmail, String cleanerEmail,
                               int rating, String comment) {
            ReviewResponse response = new ReviewResponse();
            response.setId((long) reviews.size() + 1);
            response.setReservationId(reservationId);
            response.setCleanerEmail(cleanerEmail);
            response.setClientEmail(clientEmail);
            response.setRating(rating);
            response.setComment(comment);
            reviews.add(response);
        }

        @Override
        public ReviewResponse addReviewForReservation(Long clientId, String clientEmail, Long reservationId,
                                                      ReservationReviewRequest request) {
            throw new UnsupportedOperationException("Not needed by this controller surface test");
        }

        @Override
        public List<ReviewResponse> listReviews(String cleanerEmail) {
            return reviews.stream()
                    .filter(review -> cleanerEmail.equals(review.getCleanerEmail()))
                    .toList();
        }
    }
}
