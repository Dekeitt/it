package com.clean.it.repository;

import com.clean.it.domain.Cleaner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CleanerRepository extends JpaRepository<Cleaner, Long> {
    Optional<Cleaner> findByEmail(String email);
    Optional<Cleaner> findByEmailIgnoreCase(String email);
    Optional<Cleaner> findFirstByUserId(Long userId);
}
