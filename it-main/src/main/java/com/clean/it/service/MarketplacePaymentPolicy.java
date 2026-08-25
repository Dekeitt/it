package com.clean.it.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MarketplacePaymentPolicy {
 private final boolean enabled; private final int platformFeeBps; private final Set<String> allowedCountries; private final String reverseTransfer; private final String refundApplicationFee; private final boolean onBehalfOf;
 public MarketplacePaymentPolicy(@Value("${marketplace.connect.enabled:false}") boolean enabled,@Value("${marketplace.connect.platform-fee-bps:0}") int platformFeeBps,@Value("${marketplace.connect.allowed-countries:}") String allowedCountries,@Value("${marketplace.connect.refund-reverse-transfer:unset}") String reverseTransfer,@Value("${marketplace.connect.refund-application-fee:unset}") String refundApplicationFee,@Value("${marketplace.connect.on-behalf-of:false}") boolean onBehalfOf){this.enabled=enabled;this.platformFeeBps=platformFeeBps;this.allowedCountries=Arrays.stream(allowedCountries.split(",")).map(String::trim).filter(s->!s.isBlank()).map(s->s.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());this.reverseTransfer=reverseTransfer;this.refundApplicationFee=refundApplicationFee;this.onBehalfOf=onBehalfOf;if(platformFeeBps<0||platformFeeBps>10000)throw new IllegalStateException("CONNECT_PLATFORM_FEE_BPS must be between 0 and 10000");if(enabled&&this.allowedCountries.isEmpty())throw new IllegalStateException("CONNECT_ALLOWED_COUNTRIES must be configured when Connect is enabled");}
 public boolean enabled(){return enabled;} public boolean onBehalfOf(){return onBehalfOf;}
 public void requireAllowedCountry(String country){String value=country==null?"":country.trim().toUpperCase(Locale.ROOT);if(!allowedCountries.contains(value))throw new IllegalArgumentException("Country is not enabled for marketplace payouts");}
 public long platformFee(long grossCents){if(grossCents<=0)throw new IllegalArgumentException("grossCents must be positive");return BigDecimal.valueOf(grossCents).multiply(BigDecimal.valueOf(platformFeeBps)).divide(BigDecimal.valueOf(10000),0,RoundingMode.HALF_UP).longValueExact();}
 public boolean refundReverseTransfer(){return requiredBoolean(reverseTransfer,"CONNECT_REFUND_REVERSE_TRANSFER");}
 public boolean refundApplicationFee(){return requiredBoolean(refundApplicationFee,"CONNECT_REFUND_APPLICATION_FEE");}
 private boolean requiredBoolean(String raw,String name){if(!enabled)return false;if("true".equalsIgnoreCase(raw))return true;if("false".equalsIgnoreCase(raw))return false;throw new IllegalStateException(name+" must be explicitly true or false when Connect is enabled");}
}
