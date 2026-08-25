package com.clean.it.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cleaner_service_areas")
public class CleanerServiceArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cleaner_id", nullable = false)
    private Long cleanerId;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "postal_code_prefix", nullable = false, length = 16)
    private String postalCodePrefix;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCleanerId() { return cleanerId; }
    public void setCleanerId(Long cleanerId) { this.cleanerId = cleanerId; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getPostalCodePrefix() { return postalCodePrefix; }
    public void setPostalCodePrefix(String postalCodePrefix) { this.postalCodePrefix = postalCodePrefix; }
}
