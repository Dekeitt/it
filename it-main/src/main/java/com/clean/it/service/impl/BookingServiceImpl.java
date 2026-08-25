package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.CleanerServiceArea;
import com.clean.it.domain.CleanerServiceOffering;
import com.clean.it.domain.Job;
import com.clean.it.domain.JobStatus;
import com.clean.it.domain.Reservation;
import com.clean.it.domain.ServiceType;
import com.clean.it.domain.UserAddress;
import com.clean.it.dto.AppDtos.ReservationRequest;
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
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.CleanerServiceAreaRepository;
import com.clean.it.repository.CleanerServiceOfferingRepository;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.repository.ServiceTypeRepository;
import com.clean.it.repository.UserAddressRepository;
import com.clean.it.service.BookingService;
import com.clean.it.service.CleanerAvailabilityService;
import com.clean.it.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BookingServiceImpl implements BookingService {
    private static final String CURRENCY = "eur";

    private final ServiceTypeRepository serviceTypeRepository;
    private final CleanerServiceOfferingRepository offeringRepository;
    private final CleanerServiceAreaRepository serviceAreaRepository;
    private final UserAddressRepository addressRepository;
    private final CleanerRepository cleanerRepository;
    private final CleanerAvailabilityService availabilityService;
    private final JobRepository jobRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    public BookingServiceImpl(ServiceTypeRepository serviceTypeRepository,
                              CleanerServiceOfferingRepository offeringRepository,
                              CleanerServiceAreaRepository serviceAreaRepository,
                              UserAddressRepository addressRepository,
                              CleanerRepository cleanerRepository,
                              CleanerAvailabilityService availabilityService,
                              JobRepository jobRepository,
                              ReservationRepository reservationRepository,
                              ReservationService reservationService) {
        this.serviceTypeRepository = serviceTypeRepository;
        this.offeringRepository = offeringRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.addressRepository = addressRepository;
        this.cleanerRepository = cleanerRepository;
        this.availabilityService = availabilityService;
        this.jobRepository = jobRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> catalog() {
        return serviceTypeRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toServiceDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> addresses(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toAddressDto).toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setLabel(clean(request.getLabel()));
        address.setLine1(clean(request.getLine1()));
        address.setLine2(cleanNullable(request.getLine2()));
        address.setPostalCode(clean(request.getPostalCode()));
        address.setCity(clean(request.getCity()));
        address.setRegion(cleanNullable(request.getRegion()));
        address.setCountryCode(country(request.getCountryCode()));
        return toAddressDto(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableCleanerResponse> available(Long userId, String serviceCode, Long addressId,
                                                     Instant startAt, int durationMinutes) {
        ServiceType serviceType = requireActiveService(serviceCode);
        UserAddress address = requireOwnedAddress(userId, addressId);
        validateSchedule(serviceType, startAt, durationMinutes);

        List<AvailableCleanerResponse> result = new ArrayList<>();
        for (CleanerServiceOffering offering : offeringRepository.findByServiceTypeIdAndActiveTrue(serviceType.getId())) {
            Cleaner cleaner = cleanerRepository.findFirstByUserId(offering.getCleanerId()).orElse(null);
            if (cleaner == null) continue;
            if (!covers(offering.getCleanerId(), address)) continue;
            if (!availabilityService.isAvailable(offering.getCleanerId(), startAt, durationMinutes)) continue;
            result.add(toAvailableCleaner(cleaner, serviceType, offering, durationMinutes));
        }
        result.sort(Comparator.comparing(AvailableCleanerResponse::getTotalCents)
                .thenComparing(AvailableCleanerResponse::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    @Transactional
    public ReservationResponse book(Long userId, String userEmail, DirectBookingRequest request) {
        ServiceType serviceType = requireActiveService(request.getServiceCode());
        UserAddress address = requireOwnedAddress(userId, request.getAddressId());
        validateSchedule(serviceType, request.getStartAt(), request.getDurationMinutes());

        Cleaner cleaner = cleanerRepository.findById(request.getCleanerProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
        CleanerServiceOffering offering = offeringRepository
                .findByCleanerIdAndServiceTypeId(cleaner.getUserId(), serviceType.getId())
                .filter(CleanerServiceOffering::isActive)
                .orElseThrow(() -> new IllegalStateException("Cleaner does not offer the selected service"));
        if (!covers(cleaner.getUserId(), address)) {
            throw new IllegalStateException("Cleaner does not serve the selected address");
        }
        if (!availabilityService.isAvailable(cleaner.getUserId(), request.getStartAt(), request.getDurationMinutes())) {
            throw new IllegalStateException("Cleaner not available at requested time");
        }

        long totalCents = price(offering.getHourlyRateCents(), request.getDurationMinutes());
        Job job = new Job();
        job.setClientId(userId);
        job.setClientEmail(userEmail);
        job.setCleanerId(cleaner.getUserId());
        job.setCleanerEmail(cleaner.getEmail());
        job.setTitle(serviceType.getName());
        job.setDescription("Reserva directa: " + serviceType.getName());
        job.setPriceCents(totalCents);
        job.setStatus(JobStatus.ACCEPTED);
        job.setSource("DIRECT_BOOKING");
        job = jobRepository.save(job);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setJobId(job.getId());
        reservationRequest.setCleanerEmail(cleaner.getEmail());
        reservationRequest.setStartAt(request.getStartAt());
        reservationRequest.setDurationMinutes(request.getDurationMinutes());
        ReservationResponse response = reservationService.reserve(userId, userEmail, reservationRequest);

        Reservation reservation = reservationRepository.findById(response.getId())
                .orElseThrow(() -> new IllegalStateException("Direct reservation was not persisted"));
        reservation.setServiceTypeId(serviceType.getId());
        reservation.setAddressId(address.getId());
        reservationRepository.save(reservation);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CleanerOfferingResponse> offeringsForCleanerEmail(String cleanerEmail) {
        Cleaner cleaner = requireCleanerByEmail(cleanerEmail);
        return offeringRepository.findByCleanerIdOrderByServiceTypeIdAsc(cleaner.getUserId()).stream()
                .filter(CleanerServiceOffering::isActive)
                .map(this::toOfferingDto)
                .toList();
    }

    @Override
    @Transactional
    public List<CleanerOfferingResponse> replaceOfferings(Long cleanerId, List<CleanerOfferingRequest> requests) {
        requireCleanerByUserId(cleanerId);
        Set<Long> services = new HashSet<>();
        List<CleanerServiceOffering> entities = new ArrayList<>();
        for (CleanerOfferingRequest request : requests) {
            ServiceType serviceType = requireActiveService(request.getServiceCode());
            if (!services.add(serviceType.getId())) {
                throw new IllegalArgumentException("A service can only be configured once");
            }
            CleanerServiceOffering offering = new CleanerServiceOffering();
            offering.setCleanerId(cleanerId);
            offering.setServiceTypeId(serviceType.getId());
            offering.setHourlyRateCents(request.getHourlyRateCents());
            offering.setActive(true);
            entities.add(offering);
        }
        offeringRepository.deleteByCleanerId(cleanerId);
        offeringRepository.flush();
        return offeringRepository.saveAll(entities).stream().map(this::toOfferingDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceAreaResponse> serviceAreasForCleanerEmail(String cleanerEmail) {
        Cleaner cleaner = requireCleanerByEmail(cleanerEmail);
        return serviceAreaRepository.findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(cleaner.getUserId())
                .stream().map(this::toAreaDto).toList();
    }

    @Override
    @Transactional
    public List<ServiceAreaResponse> replaceServiceAreas(Long cleanerId, List<ServiceAreaRequest> requests) {
        requireCleanerByUserId(cleanerId);
        Set<String> unique = new HashSet<>();
        List<CleanerServiceArea> entities = new ArrayList<>();
        for (ServiceAreaRequest request : requests) {
            String countryCode = country(request.getCountryCode());
            String prefix = postal(request.getPostalCodePrefix());
            if (prefix.isBlank()) throw new IllegalArgumentException("postalCodePrefix is required");
            if (!unique.add(countryCode + ":" + prefix)) {
                throw new IllegalArgumentException("A service area can only be configured once");
            }
            CleanerServiceArea area = new CleanerServiceArea();
            area.setCleanerId(cleanerId);
            area.setCountryCode(countryCode);
            area.setPostalCodePrefix(prefix);
            entities.add(area);
        }
        serviceAreaRepository.deleteByCleanerId(cleanerId);
        serviceAreaRepository.flush();
        return serviceAreaRepository.saveAll(entities).stream().map(this::toAreaDto).toList();
    }

    private ServiceType requireActiveService(String code) {
        return serviceTypeRepository.findByCodeIgnoreCase(clean(code))
                .filter(ServiceType::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found"));
    }

    private UserAddress requireOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
    }

    private Cleaner requireCleanerByEmail(String email) {
        return cleanerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
    }

    private Cleaner requireCleanerByUserId(Long cleanerId) {
        return cleanerRepository.findFirstByUserId(cleanerId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaner profile not found"));
    }

    private void validateSchedule(ServiceType serviceType, Instant startAt, int durationMinutes) {
        if (durationMinutes < serviceType.getMinimumDurationMinutes()) {
            throw new IllegalArgumentException("durationMinutes is below the service minimum");
        }
        if (durationMinutes > 1440 || durationMinutes % 30 != 0) {
            throw new IllegalArgumentException("durationMinutes must be a 30-minute increment up to 1440");
        }
        if (!startAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("startAt must be in the future");
        }
    }

    private boolean covers(Long cleanerId, UserAddress address) {
        String addressCountry = country(address.getCountryCode());
        String addressPostal = postal(address.getPostalCode());
        return serviceAreaRepository.findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(cleanerId).stream()
                .anyMatch(area -> country(area.getCountryCode()).equals(addressCountry)
                        && addressPostal.startsWith(postal(area.getPostalCodePrefix())));
    }

    private long price(long hourlyRateCents, int durationMinutes) {
        return BigDecimal.valueOf(hourlyRateCents)
                .multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private AvailableCleanerResponse toAvailableCleaner(Cleaner cleaner, ServiceType serviceType,
                                                         CleanerServiceOffering offering, int durationMinutes) {
        AvailableCleanerResponse response = new AvailableCleanerResponse();
        response.setCleanerProfileId(cleaner.getId());
        response.setEmail(cleaner.getEmail());
        response.setName(cleaner.getName());
        response.setRating(cleaner.getRating());
        response.setServiceCode(serviceType.getCode());
        response.setServiceName(serviceType.getName());
        response.setHourlyRateCents(offering.getHourlyRateCents());
        response.setTotalCents(price(offering.getHourlyRateCents(), durationMinutes));
        response.setCurrency(CURRENCY);
        return response;
    }

    private ServiceTypeResponse toServiceDto(ServiceType serviceType) {
        ServiceTypeResponse response = new ServiceTypeResponse();
        response.setCode(serviceType.getCode());
        response.setName(serviceType.getName());
        response.setDescription(serviceType.getDescription());
        response.setMinimumDurationMinutes(serviceType.getMinimumDurationMinutes());
        return response;
    }

    private AddressResponse toAddressDto(UserAddress address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setLabel(address.getLabel());
        response.setLine1(address.getLine1());
        response.setLine2(address.getLine2());
        response.setPostalCode(address.getPostalCode());
        response.setCity(address.getCity());
        response.setRegion(address.getRegion());
        response.setCountryCode(address.getCountryCode());
        return response;
    }

    private CleanerOfferingResponse toOfferingDto(CleanerServiceOffering offering) {
        ServiceType serviceType = serviceTypeRepository.findById(offering.getServiceTypeId())
                .orElseThrow(() -> new IllegalStateException("Service type linked to offering not found"));
        CleanerOfferingResponse response = new CleanerOfferingResponse();
        response.setServiceCode(serviceType.getCode());
        response.setServiceName(serviceType.getName());
        response.setMinimumDurationMinutes(serviceType.getMinimumDurationMinutes());
        response.setHourlyRateCents(offering.getHourlyRateCents());
        return response;
    }

    private ServiceAreaResponse toAreaDto(CleanerServiceArea area) {
        ServiceAreaResponse response = new ServiceAreaResponse();
        response.setId(area.getId());
        response.setCountryCode(area.getCountryCode());
        response.setPostalCodePrefix(area.getPostalCodePrefix());
        return response;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String country(String value) {
        String result = clean(value).toUpperCase(Locale.ROOT);
        if (result.length() != 2) throw new IllegalArgumentException("countryCode must contain two letters");
        return result;
    }

    private String postal(String value) {
        return clean(value).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
