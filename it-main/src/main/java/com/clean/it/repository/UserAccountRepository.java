package com.clean.it.repository;

import com.clean.it.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByIssuerAndSubject(String issuer, String subject);
    Optional<UserAccount> findFirstByEmailIgnoreCaseAndIssuer(String email, String issuer);
}
