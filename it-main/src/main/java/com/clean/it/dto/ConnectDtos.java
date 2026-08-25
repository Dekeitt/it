package com.clean.it.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class ConnectDtos {
 private ConnectDtos(){}
 public record OnboardingRequest(@NotBlank @Pattern(regexp="[A-Za-z]{2}") String countryCode){}
 public record ConnectStatusResponse(boolean enabled,boolean accountCreated,String onboardingStatus,boolean detailsSubmitted,boolean chargesEnabled,boolean payoutsEnabled,String countryCode,boolean readyForMarketplace){}
 public record OnboardingResponse(ConnectStatusResponse status,String onboardingUrl){}
}
