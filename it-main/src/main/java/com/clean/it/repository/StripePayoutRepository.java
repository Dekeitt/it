package com.clean.it.repository;

import com.clean.it.domain.StripePayout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StripePayoutRepository extends JpaRepository<StripePayout,Long> {
 Optional<StripePayout> findByStripePayoutId(String stripePayoutId);
 List<StripePayout> findTop100ByConnectAccountIdOrderByCreatedAtDesc(Long connectAccountId);
}
