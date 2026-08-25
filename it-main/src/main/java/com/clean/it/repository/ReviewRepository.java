package com.clean.it.repository;

import com.clean.it.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCleanerIdOrderByCreatedAtDesc(Long cleanerId);
    boolean existsByReservationId(Long reservationId);

    @Query("select avg(r.rating) from Review r where r.cleanerId = :cleanerId")
    Double averageRating(@Param("cleanerId") Long cleanerId);
}
