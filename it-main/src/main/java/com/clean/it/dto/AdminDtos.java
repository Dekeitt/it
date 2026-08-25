package com.clean.it.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AdminDtos {
 private AdminDtos(){}
 public record AdminSearchItem(String type,String id,String primaryText,String secondaryText,String status,Instant occurredAt){}
 public record AdminActionRequest(@NotBlank @Size(max=255) String idempotencyKey,@NotBlank @Size(max=1000) String reason){}
 public record ReviewModerationRequest(@NotBlank @Size(max=255) String idempotencyKey,@NotBlank String status,@NotBlank @Size(max=1000) String reason){}
 public record AdminActionResponse(String action,String targetType,String targetId,String status,Instant occurredAt){}
 public record TimelineItem(Instant occurredAt,String type,String status,String detail){}
 public record SettlementView(Long id,Long reservationId,Long paymentId,Long cleanerId,Long connectAccountId,Long grossCents,Long platformFeeCents,Long providerAmountCents,String currency,String stripePaymentIntentId,String stripeChargeId,String stripeTransferId,String stripeApplicationFeeId,String status){}
 public record PayoutView(Long id,String stripePayoutId,Long amountCents,String currency,String status,Instant arrivalAt,String failureCode,Instant createdAt){}
 public record ReservationTimeline(Long reservationId,String reservationStatus,Long paymentId,String paymentStatus,List<TimelineItem> events,SettlementView settlement,List<PayoutView> connectedAccountPayouts){}
}
