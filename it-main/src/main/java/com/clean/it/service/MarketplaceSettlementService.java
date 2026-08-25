package com.clean.it.service;

import com.clean.it.domain.*;
import com.clean.it.repository.MarketplaceSettlementRepository;
import com.clean.it.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceSettlementService {
 private final MarketplaceSettlementRepository settlements; private final PaymentRepository payments; private final StripeGateway stripe;
 public MarketplaceSettlementService(MarketplaceSettlementRepository settlements,PaymentRepository payments,StripeGateway stripe){this.settlements=settlements;this.payments=payments;this.stripe=stripe;}

 @Transactional
 public MarketplaceSettlement prepare(Reservation r,Payment p,CleanerConnectAccount account,long fee){MarketplaceSettlement s=settlements.findByReservationId(r.getId()).orElseGet(MarketplaceSettlement::new);s.setReservationId(r.getId());s.setPaymentId(p.getId());s.setCleanerId(r.getCleanerId());s.setConnectAccountId(account.getId());s.setGrossCents(p.getAmountCents());s.setPlatformFeeCents(fee);s.setProviderAmountCents(p.getAmountCents()-fee);s.setCurrency(p.getCurrency());s.setStripePaymentIntentId(p.getStripePaymentIntentId());s.setStatus("PAYMENT_PENDING");return settlements.save(s);}

 @Transactional
 public void reconcileSucceeded(Payment payment){if(payment.getStripeDestinationAccount()==null||payment.getStripeDestinationAccount().isBlank())return;MarketplaceSettlement settlement=settlements.findByPaymentId(payment.getId()).orElseThrow(()->new IllegalStateException("Marketplace settlement missing for connected payment"));StripeGateway.ReconciliationSnapshot snapshot=stripe.reconcileDestinationPaymentIntent(payment.getStripePaymentIntentId());payment.setStripeChargeId(snapshot.chargeId());payment.setStripeTransferId(snapshot.transferId());payment.setStripeApplicationFeeId(snapshot.applicationFeeId());payments.save(payment);settlement.setStripePaymentIntentId(snapshot.paymentIntentId());settlement.setStripeChargeId(snapshot.chargeId());settlement.setStripeTransferId(snapshot.transferId());settlement.setStripeApplicationFeeId(snapshot.applicationFeeId());settlement.setStatus(snapshot.chargeId()==null?"PAYMENT_SUCCEEDED":"TRANSFERRED");settlements.save(settlement);}

 @Transactional
 public void markRefunded(Payment payment){settlements.findByPaymentId(payment.getId()).ifPresent(s->{s.setStatus("REFUNDED");settlements.save(s);});}
}
