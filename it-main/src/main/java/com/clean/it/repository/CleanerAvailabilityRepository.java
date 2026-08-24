package com.clean.it.repository;

import com.clean.it.domain.CleanerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleanerAvailabilityRepository extends JpaRepository<CleanerAvailability, Long> {
    List<CleanerAvailability> findByCleanerEmailIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(String cleanerEmail);
    void deleteByCleanerEmailIgnoreCase(String cleanerEmail);
}
