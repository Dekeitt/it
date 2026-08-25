package com.clean.it.service;

import com.clean.it.domain.Job;
import com.clean.it.domain.Payment;
import com.clean.it.domain.Reservation;
import com.clean.it.domain.Review;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceNotificationService {
    private final NotificationOutboxService outbox;
    public MarketplaceNotificationService(NotificationOutboxService outbox){this.outbox=outbox;}

    public void jobAccepted(Job job){
        outbox.enqueueUserEvent("job:"+job.getId()+":accepted","JOB_ACCEPTED",job.getClientId(),"Tu trabajo ha sido aceptado","El trabajo #"+job.getId()+" ha sido aceptado por un profesional.","Un profesional ha aceptado tu trabajo.");
    }
    public void reservationCreated(Reservation r){
        outbox.enqueueUserEvent("reservation:"+r.getId()+":created:client","RESERVATION_CREATED",r.getClientId(),"Reserva confirmada","Tu reserva #"+r.getId()+" está programada para "+r.getStartAt()+".","Tu reserva ha sido confirmada.");
        outbox.enqueueUserEvent("reservation:"+r.getId()+":created:cleaner","RESERVATION_CREATED",r.getCleanerId(),"Nueva reserva asignada","Tienes la reserva #"+r.getId()+" programada para "+r.getStartAt()+".","Tienes una nueva reserva asignada.");
    }
    public void reservationRescheduled(Reservation r){
        String version=String.valueOf(r.getStartAt().toEpochMilli());
        outbox.enqueueUserEvent("reservation:"+r.getId()+":rescheduled:"+version+":client","RESERVATION_RESCHEDULED",r.getClientId(),"Reserva reprogramada","Tu reserva #"+r.getId()+" se ha reprogramado para "+r.getStartAt()+".","Tu reserva ha cambiado de horario.");
        outbox.enqueueUserEvent("reservation:"+r.getId()+":rescheduled:"+version+":cleaner","RESERVATION_RESCHEDULED",r.getCleanerId(),"Reserva reprogramada","La reserva #"+r.getId()+" se ha reprogramado para "+r.getStartAt()+".","Una reserva asignada ha cambiado de horario.");
    }
    public void reservationCancelled(Reservation r){
        outbox.enqueueUserEvent("reservation:"+r.getId()+":cancelled:client","RESERVATION_CANCELLED",r.getClientId(),"Reserva cancelada","La reserva #"+r.getId()+" ha sido cancelada.","Tu reserva ha sido cancelada.");
        outbox.enqueueUserEvent("reservation:"+r.getId()+":cancelled:cleaner","RESERVATION_CANCELLED",r.getCleanerId(),"Reserva cancelada","La reserva #"+r.getId()+" ha sido cancelada.","Una reserva asignada ha sido cancelada.");
    }
    public void reservationStarted(Reservation r){outbox.enqueueUserEvent("reservation:"+r.getId()+":started","SERVICE_STARTED",r.getClientId(),"Servicio iniciado","El servicio de la reserva #"+r.getId()+" ha comenzado.","Tu servicio ha comenzado.");}
    public void reservationCompleted(Reservation r){outbox.enqueueUserEvent("reservation:"+r.getId()+":completed","SERVICE_COMPLETED",r.getClientId(),"Servicio completado","El servicio de la reserva #"+r.getId()+" se ha completado. Ya puedes dejar una reseña verificada.","Tu servicio se ha completado.");}
    public void paymentStatus(Payment payment,Reservation r){
        String status=payment.getStatus()==null?"unknown":payment.getStatus().toLowerCase();
        String type=switch(status){case "succeeded"->"PAYMENT_COMPLETED";case "refunded"->"PAYMENT_REFUNDED";case "requires_payment_method","failed","payment_failed"->"PAYMENT_FAILED";default->"PAYMENT_UPDATED";};
        outbox.enqueueUserEvent("payment:"+payment.getId()+":"+status,type,r.getClientId(),"Actualización de pago","El pago de la reserva #"+r.getId()+" está en estado "+status+".","El estado de tu pago ha cambiado.");
    }
    public void reviewReceived(Review review){outbox.enqueueUserEvent("review:"+review.getReservationId()+":received","REVIEW_RECEIVED",review.getCleanerId(),"Has recibido una reseña","La reserva #"+review.getReservationId()+" ha recibido una valoración de "+review.getRating()+"/5.","Has recibido una nueva reseña verificada.");}
    public void reminder24h(Reservation r){
        outbox.enqueueUserEvent("reservation:"+r.getId()+":reminder24h:client","RESERVATION_REMINDER",r.getClientId(),"Tu servicio es mañana","Recuerda: la reserva #"+r.getId()+" está programada para "+r.getStartAt()+".","Tienes un servicio programado próximamente.");
        outbox.enqueueUserEvent("reservation:"+r.getId()+":reminder24h:cleaner","RESERVATION_REMINDER",r.getCleanerId(),"Servicio próximo","Recuerda: tienes la reserva #"+r.getId()+" programada para "+r.getStartAt()+".","Tienes un servicio asignado próximamente.");
    }
}
