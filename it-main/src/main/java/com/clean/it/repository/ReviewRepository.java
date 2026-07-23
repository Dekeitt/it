package com.clean.it.repository;

import com.clean.it.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCleanerEmailOrderByCreatedAtDesc(String cleanerEmail);
}

