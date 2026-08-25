package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationRescheduleRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;

import java.util.List;

public interface ReservationService {
    ReservationResponse reserve(Long clientId, String clientEmail, ReservationRequest req);
    List<ReservationResponse> listForUser(Long userId);
    ReservationResponse getForUser(Long userId, Long reservationId);
    ReservationResponse cancel(Long clientId, Long reservationId);
    ReservationResponse reschedule(Long clientId, Long reservationId, ReservationRescheduleRequest request);
    ReservationResponse start(Long cleanerId, Long reservationId);
    ReservationResponse complete(Long cleanerId, Long reservationId);
}
