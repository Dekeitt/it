package com.clean.it.repository;

import com.clean.it.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCleanerEmailOrderByCreatedAtDesc(String cleanerEmail);
    boolean existsByReservationId(Long reservationId);

    @Query("select avg(r.rating) from Review r where lower(r.cleanerEmail) = lower(:cleanerEmail)")
    Double averageRating(@Param("cleanerEmail") String cleanerEmail);
}
