package com.clean.it.repository;

import com.clean.it.domain.CleanerConnectAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CleanerConnectAccountRepository extends JpaRepository<CleanerConnectAccount,Long> {
 Optional<CleanerConnectAccount> findByCleanerId(Long cleanerId);
 Optional<CleanerConnectAccount> findByStripeAccountId(String stripeAccountId);
}
