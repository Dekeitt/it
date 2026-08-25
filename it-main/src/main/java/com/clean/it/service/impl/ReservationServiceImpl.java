package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.service.MarketplaceNotificationService;
import com.clean.it.service.PaymentService;
import com.clean.it.service.ReservationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ReservationServiceImpl implements ReservationService {
    private static final String SCHEDULED="SCHEDULED",IN_PROGRESS="IN_PROGRESS",COMPLETED="COMPLETED",CANCELLED="CANCELLED";
    private final ReservationRepository reservationRepository;private final JobRepository jobRepository;private final CleanerRepository cleanerRepository;private final PaymentService paymentService;private final MarketplaceNotificationService notifications;
    public ReservationServiceImpl(ReservationRepository reservationRepository,JobRepository jobRepository,CleanerRepository cleanerRepository,PaymentService paymentService,MarketplaceNotificationService notifications){this.reservationRepository=reservationRepository;this.jobRepository=jobRepository;this.cleanerRepository=cleanerRepository;this.paymentService=paymentService;this.notifications=notifications;}

    @Override @Transactional public ReservationResponse reserve(Long clientId,String clientEmail,ReservationRequest req){
        Job job=jobRepository.findById(req.getJobId()).orElseThrow(()->new IllegalArgumentException("Job not found"));if(!Objects.equals(job.getClientId(),clientId))throw new AccessDeniedException("Only the job owner can create its reservation");if(job.getPriceCents()==null||job.getPriceCents()<=0)throw new IllegalStateException("Job has no valid payable amount");
        Cleaner cleaner=cleanerRepository.findByEmailIgnoreCase(req.getCleanerEmail()).orElseThrow(()->new IllegalArgumentException("Cleaner profile not found"));if(job.getCleanerId()!=null&&!Objects.equals(job.getCleanerId(),cleaner.getUserId()))throw new IllegalStateException("Reservation cleaner does not match the accepted job cleaner");
        Instant start=req.getStartAt(),end=start.plus(Duration.ofMinutes(req.getDurationMinutes()));if(reservationRepository.existsActiveOverlap(cleaner.getUserId(),start,end))throw new IllegalStateException("Cleaner not available at requested time");
        Reservation r=new Reservation();r.setJobId(job.getId());r.setClientId(clientId);r.setClientEmail(clientEmail);r.setCleanerId(cleaner.getUserId());r.setCleanerEmail(cleaner.getEmail());r.setStartAt(start);r.setEndAt(end);r.setDurationMinutes(req.getDurationMinutes());r.setAgreedAmountCents(job.getPriceCents());r.setCurrency("eur");r.setStatus(SCHEDULED);
        r=saveWithOverlapTranslation(r);notifications.reservationCreated(r);return toDto(r);
    }
    @Override @Transactional(readOnly=true) public List<ReservationResponse> listForUser(Long userId){List<ReservationResponse> out=new ArrayList<>();for(Reservation r:reservationRepository.findByClientIdOrCleanerId(userId,userId))out.add(toDto(r));return out;}
    @Override @Transactional(readOnly=true) public ReservationResponse getForUser(Long userId,Long id){Reservation r=reservationRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Reservation not found"));ensureParticipant(userId,r);return toDto(r);}
    @Override @Transactional public ReservationResponse cancel(Long clientId,Long id){Reservation r=locked(id);ensureClient(clientId,r);requireStatus(r,SCHEDULED,"Only scheduled reservations can be cancelled");paymentService.cancelOrRefundReservationPayment(id);r.setStatus(CANCELLED);r=reservationRepository.save(r);notifications.reservationCancelled(r);return toDto(r);}
    @Override @Transactional public ReservationResponse reschedule(Long clientId,Long id,ReservationRescheduleRequest request){Reservation r=locked(id);ensureClient(clientId,r);requireStatus(r,SCHEDULED,"Only scheduled reservations can be rescheduled");Instant start=request.getStartAt(),end=start.plus(Duration.ofMinutes(request.getDurationMinutes()));if(reservationRepository.existsActiveOverlapExcludingReservation(id,r.getCleanerId(),start,end))throw new IllegalStateException("Cleaner not available at requested time");r.setStartAt(start);r.setEndAt(end);r.setDurationMinutes(request.getDurationMinutes());r=saveWithOverlapTranslation(r);notifications.reservationRescheduled(r);return toDto(r);}
    @Override @Transactional public ReservationResponse start(Long cleanerId,Long id){Reservation r=locked(id);ensureCleaner(cleanerId,r);requireStatus(r,SCHEDULED,"Only scheduled reservations can be started");r.setStatus(IN_PROGRESS);r=reservationRepository.save(r);notifications.reservationStarted(r);return toDto(r);}
    @Override @Transactional public ReservationResponse complete(Long cleanerId,Long id){Reservation r=locked(id);ensureCleaner(cleanerId,r);requireStatus(r,IN_PROGRESS,"Only in-progress reservations can be completed");r.setStatus(COMPLETED);r=reservationRepository.save(r);notifications.reservationCompleted(r);return toDto(r);}
    private Reservation locked(Long id){return reservationRepository.findLockedById(id).orElseThrow(()->new IllegalArgumentException("Reservation not found"));}
    private Reservation saveWithOverlapTranslation(Reservation r){try{return reservationRepository.saveAndFlush(r);}catch(DataIntegrityViolationException ex){String cause=ex.getMostSpecificCause().getMessage();if(cause!=null&&cause.contains("reservations_no_cleaner_overlap"))throw new IllegalStateException("Cleaner not available at requested time",ex);throw ex;}}
    private void ensureParticipant(Long id,Reservation r){if(!Objects.equals(id,r.getClientId())&&!Objects.equals(id,r.getCleanerId()))throw new AccessDeniedException("Reservation is not visible to this user");}
    private void ensureClient(Long id,Reservation r){if(!Objects.equals(id,r.getClientId()))throw new AccessDeniedException("Only the reservation client can perform this operation");}
    private void ensureCleaner(Long id,Reservation r){if(!Objects.equals(id,r.getCleanerId()))throw new AccessDeniedException("Only the assigned cleaner can perform this operation");}
    private void requireStatus(Reservation r,String expected,String message){String current=r.getStatus()==null?"":r.getStatus().toUpperCase(Locale.ROOT);if(!expected.equals(current))throw new IllegalStateException(message+" (current status: "+current+")");}
    private ReservationResponse toDto(Reservation r){ReservationResponse x=new ReservationResponse();x.setId(r.getId());x.setJobId(r.getJobId());x.setClientEmail(r.getClientEmail());x.setCleanerEmail(r.getCleanerEmail());x.setStartAt(r.getStartAt());x.setEndAt(r.getEndAt());x.setDurationMinutes(r.getDurationMinutes());x.setAgreedAmountCents(r.getAgreedAmountCents());x.setCurrency(r.getCurrency());x.setStatus(r.getStatus());return x;}
}
