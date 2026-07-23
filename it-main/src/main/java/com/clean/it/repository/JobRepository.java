package com.clean.it.repository;

import com.clean.it.domain.Job;
import com.clean.it.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    @Query("select count(j) > 0 from Job j where j.cleanerEmail = :cleanerEmail and j.status in :active")
    boolean existsByCleanerEmailAndStatusIn(@Param("cleanerEmail") String cleanerEmail, @Param("active") List<JobStatus> active);
}

