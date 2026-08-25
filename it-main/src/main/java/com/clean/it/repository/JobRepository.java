package com.clean.it.repository;

import com.clean.it.domain.Job;
import com.clean.it.domain.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Job j where j.id = :id")
    Optional<Job> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(j) > 0 from Job j where j.cleanerId = :cleanerId and j.status in :active")
    boolean existsByCleanerIdAndStatusIn(@Param("cleanerId") Long cleanerId, @Param("active") List<JobStatus> active);

    @Query("""
      select j from Job j
      where :q='' or lower(coalesce(j.clientEmail,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(j.cleanerEmail,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(j.title,'')) like lower(concat('%',:q,'%'))
         or lower(coalesce(j.description,'')) like lower(concat('%',:q,'%'))
         or cast(j.id as string)=:q
      order by j.createdAt desc
      """)
    List<Job> adminSearch(@Param("q") String q, Pageable pageable);
}
