package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;

import java.util.List;

public interface ReservationService {
    ReservationResponse reserve(String clientEmail, ReservationRequest req);
    List<ReservationResponse> listForUser(String userEmail);
    ReservationResponse getForUser(String userEmail, Long reservationId);
    ReservationResponse cancel(String clientEmail, Long reservationId);
    ReservationResponse reschedule(String clientEmail, Long reservationId, ReservationRescheduleRequest request);
    ReservationResponse start(String cleanerEmail, Long reservationId);
    ReservationResponse complete(String cleanerEmail, Long reservationId);
}
