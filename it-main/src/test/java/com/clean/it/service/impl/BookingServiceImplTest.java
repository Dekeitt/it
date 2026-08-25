package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.domain.CleanerServiceArea;
import com.clean.it.domain.CleanerServiceOffering;
import com.clean.it.domain.Job;
import com.clean.it.domain.Reservation;
import com.clean.it.domain.ServiceType;
import com.clean.it.domain.UserAddress;
import com.clean.it.dto.AppDtos.ReservationResponse;
import com.clean.it.dto.BookingDtos.DirectBookingRequest;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.repository.CleanerServiceAreaRepository;
import com.clean.it.repository.CleanerServiceOfferingRepository;
import com.clean.it.repository.JobRepository;
import com.clean.it.repository.ReservationRepository;
import com.clean.it.repository.ServiceTypeRepository;
import com.clean.it.repository.UserAddressRepository;
import com.clean.it.service.CleanerAvailabilityService;
import com.clean.it.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    @Test
    void availableOnlyReturnsCleanersCoveringTheAddressAndCalculatesPriceOnServer() {
        Fixture f = new Fixture();
        Instant start = Instant.now().plusSeconds(3600);
        f.stubCatalogAndAddress();
        when(f.offerings.findByServiceTypeIdAndActiveTrue(1L)).thenReturn(List.of(f.offering(20L, 1_800L)));
        when(f.cleaners.findFirstByUserId(20L)).thenReturn(Optional.of(f.cleaner()));
        when(f.areas.findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(20L)).thenReturn(List.of(f.area("ES", "28")));
        when(f.availability.isAvailable(20L, start, 120)).thenReturn(true);

        var result = f.service().available(10L, "STANDARD", 7L, start, 120);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCleanerProfileId()).isEqualTo(3L);
        assertThat(result.get(0).getHourlyRateCents()).isEqualTo(1_800L);
        assertThat(result.get(0).getTotalCents()).isEqualTo(3_600L);
        assertThat(result.get(0).getCurrency()).isEqualTo("eur");
    }

    @Test
    void availableExcludesCleanerOutsideTheSavedAddressArea() {
        Fixture f = new Fixture();
        Instant start = Instant.now().plusSeconds(3600);
        f.stubCatalogAndAddress();
        when(f.offerings.findByServiceTypeIdAndActiveTrue(1L)).thenReturn(List.of(f.offering(20L, 2_000L)));
        when(f.cleaners.findFirstByUserId(20L)).thenReturn(Optional.of(f.cleaner()));
        when(f.areas.findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(20L)).thenReturn(List.of(f.area("ES", "08")));

        assertThat(f.service().available(10L, "STANDARD", 7L, start, 120)).isEmpty();
    }

    @Test
    void directBookingFreezesPersistedCleanerRateIntoInternalJob() {
        Fixture f = new Fixture();
        Instant start = Instant.now().plusSeconds(7200);
        f.stubCatalogAndAddress();
        Cleaner cleaner = f.cleaner();
        CleanerServiceOffering offering = f.offering(20L, 1_800L);
        when(f.cleaners.findById(3L)).thenReturn(Optional.of(cleaner));
        when(f.offerings.findByCleanerIdAndServiceTypeId(20L, 1L)).thenReturn(Optional.of(offering));
        when(f.areas.findByCleanerIdOrderByCountryCodeAscPostalCodePrefixAsc(20L)).thenReturn(List.of(f.area("ES", "28")));
        when(f.availability.isAvailable(20L, start, 90)).thenReturn(true);
        when(f.jobs.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(99L);
            return job;
        });
        ReservationResponse reservationResponse = new ReservationResponse();
        reservationResponse.setId(88L);
        when(f.reservationService.reserve(any(), any(), any())).thenReturn(reservationResponse);
        Reservation reservation = new Reservation();
        reservation.setId(88L);
        when(f.reservations.findById(88L)).thenReturn(Optional.of(reservation));
        when(f.reservations.save(reservation)).thenReturn(reservation);

        DirectBookingRequest request = new DirectBookingRequest();
        request.setServiceCode("STANDARD");
        request.setCleanerProfileId(3L);
        request.setAddressId(7L);
        request.setStartAt(start);
        request.setDurationMinutes(90);
        f.service().book(10L, "client@example.com", request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(f.jobs).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getPriceCents()).isEqualTo(2_700L);
        assertThat(jobCaptor.getValue().getSource()).isEqualTo("DIRECT_BOOKING");
        assertThat(jobCaptor.getValue().getCleanerId()).isEqualTo(20L);
        assertThat(reservation.getServiceTypeId()).isEqualTo(1L);
        assertThat(reservation.getAddressId()).isEqualTo(7L);
    }

    private static class Fixture {
        final ServiceTypeRepository serviceTypes = mock(ServiceTypeRepository.class);
        final CleanerServiceOfferingRepository offerings = mock(CleanerServiceOfferingRepository.class);
        final CleanerServiceAreaRepository areas = mock(CleanerServiceAreaRepository.class);
        final UserAddressRepository addresses = mock(UserAddressRepository.class);
        final CleanerRepository cleaners = mock(CleanerRepository.class);
        final CleanerAvailabilityService availability = mock(CleanerAvailabilityService.class);
        final JobRepository jobs = mock(JobRepository.class);
        final ReservationRepository reservations = mock(ReservationRepository.class);
        final ReservationService reservationService = mock(ReservationService.class);

        BookingServiceImpl service() {
            return new BookingServiceImpl(serviceTypes, offerings, areas, addresses, cleaners,
                    availability, jobs, reservations, reservationService);
        }

        void stubCatalogAndAddress() {
            when(serviceTypes.findByCodeIgnoreCase("STANDARD")).thenReturn(Optional.of(serviceType()));
            when(addresses.findByIdAndUserId(7L, 10L)).thenReturn(Optional.of(address()));
        }

        ServiceType serviceType() {
            ServiceType service = new ServiceType();
            service.setId(1L);
            service.setCode("STANDARD");
            service.setName("Limpieza estándar");
            service.setMinimumDurationMinutes(60);
            service.setActive(true);
            return service;
        }

        UserAddress address() {
            UserAddress address = new UserAddress();
            address.setId(7L);
            address.setUserId(10L);
            address.setLabel("Casa");
            address.setLine1("Calle Mayor 1");
            address.setPostalCode("28001");
            address.setCity("Madrid");
            address.setCountryCode("ES");
            return address;
        }

        Cleaner cleaner() {
            Cleaner cleaner = new Cleaner();
            cleaner.setId(3L);
            cleaner.setUserId(20L);
            cleaner.setEmail("cleaner@example.com");
            cleaner.setName("Cleaner");
            cleaner.setRating(4.7);
            return cleaner;
        }

        CleanerServiceOffering offering(Long cleanerId, Long hourlyRateCents) {
            CleanerServiceOffering offering = new CleanerServiceOffering();
            offering.setCleanerId(cleanerId);
            offering.setServiceTypeId(1L);
            offering.setHourlyRateCents(hourlyRateCents);
            offering.setActive(true);
            return offering;
        }

        CleanerServiceArea area(String country, String prefix) {
            CleanerServiceArea area = new CleanerServiceArea();
            area.setCleanerId(20L);
            area.setCountryCode(country);
            area.setPostalCodePrefix(prefix);
            return area;
        }
    }
}
