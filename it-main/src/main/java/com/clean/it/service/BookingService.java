package com.clean.it.service;

import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.dto.BookingDtos.AddressRequest;
import com.clean.it.dto.BookingDtos.AddressResponse;
import com.clean.it.dto.BookingDtos.AvailableCleanerResponse;
import com.clean.it.dto.BookingDtos.CleanerOfferingRequest;
import com.clean.it.dto.BookingDtos.CleanerOfferingResponse;
import com.clean.it.dto.BookingDtos.DirectBookingRequest;
import com.clean.it.dto.BookingDtos.ServiceAreaRequest;
import com.clean.it.dto.BookingDtos.ServiceAreaResponse;
import com.clean.it.dto.BookingDtos.ServiceTypeResponse;

import java.time.Instant;
import java.util.List;

public interface BookingService {
    List<ServiceTypeResponse> catalog();
    List<AddressResponse> addresses(Long userId);
    AddressResponse createAddress(Long userId, AddressRequest request);
    List<AvailableCleanerResponse> available(Long userId, String serviceCode, Long addressId,
                                             Instant startAt, int durationMinutes);
    ReservationResponse book(Long userId, String userEmail, DirectBookingRequest request);

    List<CleanerOfferingResponse> offeringsForCleanerEmail(String cleanerEmail);
    List<CleanerOfferingResponse> replaceOfferings(Long cleanerId, List<CleanerOfferingRequest> requests);
    List<ServiceAreaResponse> serviceAreasForCleanerEmail(String cleanerEmail);
    List<ServiceAreaResponse> replaceServiceAreas(Long cleanerId, List<ServiceAreaRequest> requests);
}
