package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReservationRequest;
import com.clean.it.dto.AppDtos.ReservationResponse;
import java.util.List;

public interface ReservationService {
    ReservationResponse reserve(String clientEmail, ReservationRequest req);
    java.util.List<ReservationResponse> listForUser(String userEmail);
}

