package com.clean.it.service;

import com.clean.it.dto.AppDtos.PaymentRequest;
import com.clean.it.dto.AppDtos.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPaymentIntent(String userEmail, PaymentRequest req);
}

