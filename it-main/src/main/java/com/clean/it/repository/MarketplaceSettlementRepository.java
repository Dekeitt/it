package com.clean.it.repository;

import com.clean.it.domain.MarketplaceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarketplaceSettlementRepository extends JpaRepository<MarketplaceSettlement,Long> {
 Optional<MarketplaceSettlement> findByReservationId(Long reservationId);
 Optional<MarketplaceSettlement> findByPaymentId(Long paymentId);
 List<MarketplaceSettlement> findTop100ByCleanerIdOrderByCreatedAtDesc(Long cleanerId);
 List<MarketplaceSettlement> findTop100ByOrderByCreatedAtDesc();
}
