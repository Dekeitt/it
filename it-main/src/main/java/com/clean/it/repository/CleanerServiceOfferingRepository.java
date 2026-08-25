package com.clean.it.repository;

import com.clean.it.domain.CleanerServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CleanerServiceOfferingRepository extends JpaRepository<CleanerServiceOffering, Long> {
    List<CleanerServiceOffering> findByCleanerIdOrderByServiceTypeIdAsc(Long cleanerId);
    List<CleanerServiceOffering> findByServiceTypeIdAndActiveTrue(Long serviceTypeId);
    Optional<CleanerServiceOffering> findByCleanerIdAndServiceTypeId(Long cleanerId, Long serviceTypeId);
    void deleteByCleanerId(Long cleanerId);
}
