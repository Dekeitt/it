package com.clean.it.repository;

import com.clean.it.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("select r from Review r where r.cleanerId=:cleanerId and r.moderationStatus='VISIBLE' order by r.createdAt desc")
    List<Review> findByCleanerIdOrderByCreatedAtDesc(@Param("cleanerId") Long cleanerId);
    boolean existsByReservationId(Long reservationId);

    @Query("select avg(r.rating) from Review r where r.cleanerId = :cleanerId and r.moderationStatus='VISIBLE'")
    Double averageRating(@Param("cleanerId") Long cleanerId);

    @Query("""
      select r from Review r
      where :q='' or lower(r.cleanerEmail) like lower(concat('%',:q,'%'))
         or lower(r.clientEmail) like lower(concat('%',:q,'%'))
         or lower(coalesce(r.comment,'')) like lower(concat('%',:q,'%'))
         or cast(r.id as string)=:q or cast(r.reservationId as string)=:q
      order by r.createdAt desc
      """)
    List<Review> adminSearch(@Param("q") String q, Pageable pageable);
}
