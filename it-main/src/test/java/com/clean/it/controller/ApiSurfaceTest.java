package com.clean.it.controller;

import com.clean.it.dto.AppDtos.ReviewRequest;
import com.clean.it.dto.AppDtos.ReviewResponse;
import com.clean.it.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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
    void reviewApiCreatesAndListsReviews() {
        InMemoryReviewService service = new InMemoryReviewService();
        ReviewApiController controller = new ReviewApiController(service);
        Authentication auth = new UsernamePasswordAuthenticationToken("client@example.com", "secret", List.of());

        ReviewRequest request = new ReviewRequest();
        request.setCleanerEmail("cleaner@example.com");
        request.setRating(5);
        request.setComment("Excelente servicio");

        ResponseEntity<ReviewResponse> created = controller.addReview(auth, request);

        assertThat(created.getStatusCode().value()).isEqualTo(200);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().getCleanerEmail()).isEqualTo("cleaner@example.com");
        assertThat(created.getBody().getClientEmail()).isEqualTo("client@example.com");

        ResponseEntity<List<ReviewResponse>> listed = controller.list("cleaner@example.com");

        assertThat(listed.getStatusCode().value()).isEqualTo(200);
        assertThat(listed.getBody()).hasSize(1);
        assertThat(listed.getBody().get(0).getComment()).isEqualTo("Excelente servicio");
    }

    static class InMemoryReviewService implements ReviewService {
        private final List<ReviewResponse> reviews = new ArrayList<>();

        @Override
        public ReviewResponse addReview(String clientEmail, ReviewRequest req) {
            ReviewResponse resp = new ReviewResponse();
            resp.setId((long) reviews.size() + 1);
            resp.setCleanerEmail(req.getCleanerEmail());
            resp.setClientEmail(clientEmail);
            resp.setRating(req.getRating());
            resp.setComment(req.getComment());
            reviews.add(resp);
            return resp;
        }

        @Override
        public List<ReviewResponse> listReviews(String cleanerEmail) {
            return reviews.stream()
                    .filter(review -> cleanerEmail.equals(review.getCleanerEmail()))
                    .toList();
        }
    }
}
