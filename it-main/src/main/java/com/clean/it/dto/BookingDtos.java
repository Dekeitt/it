package com.clean.it.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class BookingDtos {
    private BookingDtos() {}

    public static class ServiceTypeResponse {
        private String code;
        private String name;
        private String description;
        private Integer minimumDurationMinutes;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getMinimumDurationMinutes() { return minimumDurationMinutes; }
        public void setMinimumDurationMinutes(Integer minimumDurationMinutes) { this.minimumDurationMinutes = minimumDurationMinutes; }
    }

    public static class AddressRequest {
        @NotBlank @Size(max = 80) private String label;
        @NotBlank @Size(max = 255) private String line1;
        @Size(max = 255) private String line2;
        @NotBlank @Size(max = 32) private String postalCode;
        @NotBlank @Size(max = 160) private String city;
        @Size(max = 160) private String region;
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") private String countryCode;
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }
        public String getLine2() { return line2; }
        public void setLine2(String line2) { this.line2 = line2; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }

    public static class AddressResponse extends AddressRequest {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class CleanerOfferingRequest {
        @NotBlank private String serviceCode;
        @NotNull @Positive private Long hourlyRateCents;
        public String getServiceCode() { return serviceCode; }
        public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
        public Long getHourlyRateCents() { return hourlyRateCents; }
        public void setHourlyRateCents(Long hourlyRateCents) { this.hourlyRateCents = hourlyRateCents; }
    }

    public static class CleanerOfferingResponse {
        private String serviceCode;
        private String serviceName;
        private Integer minimumDurationMinutes;
        private Long hourlyRateCents;
        public String getServiceCode() { return serviceCode; }
        public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Integer getMinimumDurationMinutes() { return minimumDurationMinutes; }
        public void setMinimumDurationMinutes(Integer minimumDurationMinutes) { this.minimumDurationMinutes = minimumDurationMinutes; }
        public Long getHourlyRateCents() { return hourlyRateCents; }
        public void setHourlyRateCents(Long hourlyRateCents) { this.hourlyRateCents = hourlyRateCents; }
    }

    public static class ServiceAreaRequest {
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") private String countryCode;
        @NotBlank @Size(max = 16) private String postalCodePrefix;
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getPostalCodePrefix() { return postalCodePrefix; }
        public void setPostalCodePrefix(String postalCodePrefix) { this.postalCodePrefix = postalCodePrefix; }
    }

    public static class ServiceAreaResponse extends ServiceAreaRequest {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class AvailableCleanerResponse {
        private Long cleanerProfileId;
        private String email;
        private String name;
        private Double rating;
        private String serviceCode;
        private String serviceName;
        private Long hourlyRateCents;
        private Long totalCents;
        private String currency;
        public Long getCleanerProfileId() { return cleanerProfileId; }
        public void setCleanerProfileId(Long cleanerProfileId) { this.cleanerProfileId = cleanerProfileId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public String getServiceCode() { return serviceCode; }
        public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Long getHourlyRateCents() { return hourlyRateCents; }
        public void setHourlyRateCents(Long hourlyRateCents) { this.hourlyRateCents = hourlyRateCents; }
        public Long getTotalCents() { return totalCents; }
        public void setTotalCents(Long totalCents) { this.totalCents = totalCents; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class DirectBookingRequest {
        @NotBlank private String serviceCode;
        @NotNull private Long cleanerProfileId;
        @NotNull private Long addressId;
        @NotNull private Instant startAt;
        @NotNull @Min(30) @Max(1440) private Integer durationMinutes;
        public String getServiceCode() { return serviceCode; }
        public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
        public Long getCleanerProfileId() { return cleanerProfileId; }
        public void setCleanerProfileId(Long cleanerProfileId) { this.cleanerProfileId = cleanerProfileId; }
        public Long getAddressId() { return addressId; }
        public void setAddressId(Long addressId) { this.addressId = addressId; }
        public Instant getStartAt() { return startAt; }
        public void setStartAt(Instant startAt) { this.startAt = startAt; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    }
}
