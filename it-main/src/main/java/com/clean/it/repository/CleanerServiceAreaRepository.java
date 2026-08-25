package com.clean.it.repository;

import com.clean.it.domain.CleanerServiceArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleanerServiceAreaRepository extends JpaRepository<CleanerServiceArea, Long> {
    List<CleanerServiceArea> findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(Long cleanerId);
    void deleteByCleanerId(Long cleanerId);
}
