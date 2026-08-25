package com.clean.it.repository;

import com.clean.it.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    Optional<ServiceType> findByCodeIgnoreCase(String code);
    List<ServiceType> findByActiveTrueOrderByNameAsc();
}
