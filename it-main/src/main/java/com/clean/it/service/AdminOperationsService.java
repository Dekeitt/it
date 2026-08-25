package com.clean.it.service;

import com.clean.it.domain.*;
import com.clean.it.dto.AdminDtos.*;
import com.clean.it.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminOperationsService {
 private final UserAccountRepository users; private final CleanerRepository cleaners; private final JobRepository jobs; private final ReservationRepository reservations; private final PaymentRepository payments; private final ReviewRepository reviews; private final PaymentEventRepository paymentEvents; private final AdminAuditLogRepository audit; private final MarketplaceSettlementRepository settlements; private final PaymentService paymentService; private final MarketplaceNotificationService notifications;
 public AdminOperationsService(UserAccountRepository users,CleanerRepository cleaners,JobRepository jobs,ReservationRepository reservations,PaymentRepository payments,ReviewRepository reviews,PaymentEventRepository paymentEvents,AdminAuditLogRepository audit,MarketplaceSettlementRepository settlements,PaymentService paymentService,MarketplaceNotificationService notifications){this.users=users;this.cleaners=cleaners;this.jobs=jobs;this.reservations=reservations;this.payments=payments;this.reviews=reviews;this.paymentEvents=paymentEvents;this.audit=audit;this.settlements=settlements;this.paymentService=paymentService;this.notifications=notifications;}

 @Transactional(readOnly=true)
 public List<AdminSearchItem> search(String type,String query,int limit){String q=query==null?"":query.trim();var page=PageRequest.of(0,Math.max(1,Math.min(limit,100)));return switch(type.toUpperCase(Locale.ROOT)){
  case "USER" -> users.adminSearch(q,page).stream().map(u->new AdminSearchItem("USER",String.valueOf(u.getId()),or(u.getEmail(),u.getSubject()),u.getDisplayName(),u.getBlockedAt()==null?"ACTIVE":"BLOCKED",u.getUpdatedAt())).toList();
  case "CLEANER" -> cleaners.adminSearch(q,page).stream().map(c->new AdminSearchItem("CLEANER",String.valueOf(c.getId()),c.getName(),c.getEmail(),"RATING "+(c.getRating()==null?0:c.getRating()),c.getCreatedAt())).toList();
  case "JOB" -> jobs.adminSearch(q,page).stream().map(j->new AdminSearchItem("JOB",String.valueOf(j.getId()),or(j.getTitle(),j.getDescription()),j.getClientEmail()+" → "+or(j.getCleanerEmail(),"sin asignar"),j.getStatus().name(),j.getCreatedAt())).toList();
  case "RESERVATION" -> reservations.adminSearch(q,page).stream().map(r->new AdminSearchItem("RESERVATION",String.valueOf(r.getId()),r.getClientEmail()+" → "+r.getCleanerEmail(),String.valueOf(r.getStartAt()),r.getStatus(),r.getCreatedAt())).toList();
  case "PAYMENT" -> payments.adminSearch(q,page).stream().map(p->new AdminSearchItem("PAYMENT",String.valueOf(p.getId()),or(p.getStripePaymentIntentId(),"sin PaymentIntent"),"reserva #"+p.getReservationId(),p.getStatus(),p.getCreatedAt())).toList();
  case "REVIEW" -> reviews.adminSearch(q,page).stream().map(r->new AdminSearchItem("REVIEW",String.valueOf(r.getId()),r.getCleanerEmail(),r.getComment(),r.getModerationStatus(),r.getCreatedAt())).toList();
  default -> throw new IllegalArgumentException("type must be USER, CLEANER, JOB, RESERVATION, PAYMENT or REVIEW");};}

 @Transactional
 public AdminActionResponse block(Long actorId,Long userId,AdminActionRequest request){if(Objects.equals(actorId,userId))throw new IllegalArgumentException("An admin cannot block their own account");if(alreadyApplied(request.idempotencyKey(),"BLOCK_USER","USER",userId))return response("BLOCK_USER","USER",userId,"BLOCKED");UserAccount user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));user.setBlockedAt(Instant.now());user.setBlockedReason(request.reason().trim());user.setBlockedByUserId(actorId);users.save(user);record(actorId,"BLOCK_USER","USER",userId,request.idempotencyKey(),request.reason());return response("BLOCK_USER","USER",userId,"BLOCKED");}

 @Transactional
 public AdminActionResponse unblock(Long actorId,Long userId,AdminActionRequest request){if(alreadyApplied(request.idempotencyKey(),"UNBLOCK_USER","USER",userId))return response("UNBLOCK_USER","USER",userId,"ACTIVE");UserAccount user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));user.setBlockedAt(null);user.setBlockedReason(null);user.setBlockedByUserId(null);users.save(user);record(actorId,"UNBLOCK_USER","USER",userId,request.idempotencyKey(),request.reason());return response("UNBLOCK_USER","USER",userId,"ACTIVE");}

 @Transactional
 public AdminActionResponse moderateReview(Long actorId,Long reviewId,ReviewModerationRequest request){String status=request.status().trim().toUpperCase(Locale.ROOT);if(!Set.of("VISIBLE","HIDDEN").contains(status))throw new IllegalArgumentException("status must be VISIBLE or HIDDEN");if(alreadyApplied(request.idempotencyKey(),"MODERATE_REVIEW","REVIEW",reviewId))return response("MODERATE_REVIEW","REVIEW",reviewId,status);Review review=reviews.findById(reviewId).orElseThrow(()->new IllegalArgumentException("Review not found"));review.setModerationStatus(status);review.setModeratedAt(Instant.now());review.setModeratedByUserId(actorId);review.setModerationReason(request.reason().trim());reviews.save(review);refreshRating(review.getCleanerId());record(actorId,"MODERATE_REVIEW","REVIEW",reviewId,request.idempotencyKey(),status+": "+request.reason());return response("MODERATE_REVIEW","REVIEW",reviewId,status);}

 @Transactional
 public AdminActionResponse cancelReservation(Long actorId,Long reservationId,AdminActionRequest request){if(alreadyApplied(request.idempotencyKey(),"CANCEL_RESERVATION","RESERVATION",reservationId))return response("CANCEL_RESERVATION","RESERVATION",reservationId,"CANCELLED");Reservation r=reservations.findLockedById(reservationId).orElseThrow(()->new IllegalArgumentException("Reservation not found"));if("CANCELLED".equalsIgnoreCase(r.getStatus())){record(actorId,"CANCEL_RESERVATION","RESERVATION",reservationId,request.idempotencyKey(),"already cancelled: "+request.reason());return response("CANCEL_RESERVATION","RESERVATION",reservationId,"CANCELLED");}if(!"SCHEDULED".equalsIgnoreCase(r.getStatus()))throw new IllegalStateException("Only scheduled reservations can be cancelled by operations");paymentService.cancelOrRefundReservationPayment(reservationId);r.setStatus("CANCELLED");reservations.save(r);notifications.reservationCancelled(r);record(actorId,"CANCEL_RESERVATION","RESERVATION",reservationId,request.idempotencyKey(),request.reason());return response("CANCEL_RESERVATION","RESERVATION",reservationId,"CANCELLED");}

 @Transactional
 public AdminActionResponse refundReservation(Long actorId,Long reservationId,AdminActionRequest request){if(alreadyApplied(request.idempotencyKey(),"REFUND_RESERVATION","RESERVATION",reservationId))return response("REFUND_RESERVATION","RESERVATION",reservationId,currentPaymentStatus(reservationId));reservations.findById(reservationId).orElseThrow(()->new IllegalArgumentException("Reservation not found"));paymentService.cancelOrRefundReservationPayment(reservationId);String status=currentPaymentStatus(reservationId);record(actorId,"REFUND_RESERVATION","RESERVATION",reservationId,request.idempotencyKey(),request.reason());return response("REFUND_RESERVATION","RESERVATION",reservationId,status);}

 @Transactional(readOnly=true)
 public ReservationTimeline timeline(Long reservationId){Reservation r=reservations.findById(reservationId).orElseThrow(()->new IllegalArgumentException("Reservation not found"));Payment p=payments.findFirstByReservationId(reservationId).orElse(null);List<TimelineItem> events=new ArrayList<>();events.add(new TimelineItem(r.getCreatedAt(),"RESERVATION_CREATED",r.getStatus(),"Reserva creada"));if(p!=null){events.add(new TimelineItem(p.getCreatedAt(),"PAYMENT_CREATED",p.getStatus(),or(p.getStripePaymentIntentId(),"Pago local")));for(PaymentEvent e:paymentEvents.findByPaymentIdOrderByEventCreatedAtAsc(p.getId()))events.add(new TimelineItem(e.getEventCreatedAt(),e.getType(),e.getStatus(),or(e.getFailureReason(),e.getEventId())));}for(AdminAuditLog a:audit.findTop100ByTargetTypeAndTargetIdOrderByCreatedAtDesc("RESERVATION",String.valueOf(reservationId)))events.add(new TimelineItem(a.getCreatedAt(),"ADMIN_"+a.getAction(),"AUDITED",a.getDetails()));events.sort(Comparator.comparing(TimelineItem::occurredAt,Comparator.nullsLast(Comparator.naturalOrder())));SettlementView settlement=settlements.findByReservationId(reservationId).map(this::view).orElse(null);return new ReservationTimeline(reservationId,r.getStatus(),p==null?null:p.getId(),p==null?null:p.getStatus(),events,settlement);}

 private SettlementView view(MarketplaceSettlement s){return new SettlementView(s.getId(),s.getReservationId(),s.getPaymentId(),s.getCleanerId(),s.getGrossCents(),s.getPlatformFeeCents(),s.getProviderAmountCents(),s.getCurrency(),s.getStripePaymentIntentId(),s.getStripeChargeId(),s.getStripeTransferId(),s.getStripeApplicationFeeId(),s.getStatus());}
 private void refreshRating(Long cleanerId){Double avg=reviews.averageRating(cleanerId);cleaners.findFirstByUserId(cleanerId).ifPresent(c->{c.setRating(avg==null?0.0:Math.round(avg*100.0)/100.0);cleaners.save(c);});}
 private boolean alreadyApplied(String key,String action,String targetType,Object targetId){return audit.findByIdempotencyKey(key).map(a->{if(!a.getAction().equals(action)||!a.getTargetType().equals(targetType)||!a.getTargetId().equals(String.valueOf(targetId)))throw new IllegalArgumentException("idempotencyKey was already used for a different operation");return true;}).orElse(false);}
 private void record(Long actorId,String action,String targetType,Object targetId,String key,String details){AdminAuditLog a=new AdminAuditLog();a.setActorUserId(actorId);a.setAction(action);a.setTargetType(targetType);a.setTargetId(String.valueOf(targetId));a.setIdempotencyKey(key);a.setDetails(details==null?null:details.trim());audit.save(a);}
 private AdminActionResponse response(String action,String type,Object id,String status){return new AdminActionResponse(action,type,String.valueOf(id),status,Instant.now());}
 private String currentPaymentStatus(Long reservationId){return payments.findFirstByReservationId(reservationId).map(Payment::getStatus).orElse("NO_PAYMENT");}
 private String or(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
}
