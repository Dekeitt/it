package com.clean.it.service;

import com.clean.it.domain.CleanerConnectAccount;
import com.clean.it.dto.ConnectDtos.*;
import com.clean.it.repository.CleanerConnectAccountRepository;
import com.clean.it.repository.CleanerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class ConnectAccountService {
 private final CleanerConnectAccountRepository accounts; private final CleanerRepository cleaners; private final StripeConnectGateway stripe; private final MarketplacePaymentPolicy policy; private final String refreshUrl; private final String returnUrl;
 public ConnectAccountService(CleanerConnectAccountRepository accounts,CleanerRepository cleaners,StripeConnectGateway stripe,MarketplacePaymentPolicy policy,@Value("${marketplace.connect.refresh-url:}") String refreshUrl,@Value("${marketplace.connect.return-url:}") String returnUrl){this.accounts=accounts;this.cleaners=cleaners;this.stripe=stripe;this.policy=policy;this.refreshUrl=refreshUrl;this.returnUrl=returnUrl;}

 @Transactional
 public OnboardingResponse onboarding(Long cleanerId,String email,OnboardingRequest request){requireEnabled();cleaners.findFirstByUserId(cleanerId).orElseThrow(()->new IllegalStateException("Create the cleaner profile before onboarding payouts"));String country=request.countryCode().trim().toUpperCase(Locale.ROOT);policy.requireAllowedCountry(country);CleanerConnectAccount account=accounts.findByCleanerId(cleanerId).orElseGet(()->create(cleanerId,email,country));sync(account,stripe.retrieveConnectedAccount(account.getStripeAccountId()));if(refreshUrl.isBlank()||returnUrl.isBlank())throw new IllegalStateException("Connect onboarding return URLs are not configured");String key="cleaner:"+cleanerId+":connect-onboarding:"+(Instant.now().getEpochSecond()/60);String url=stripe.createOnboardingLink(account.getStripeAccountId(),refreshUrl,returnUrl,key);return new OnboardingResponse(status(account),url);}

 @Transactional
 public ConnectStatusResponse status(Long cleanerId){if(!policy.enabled())return new ConnectStatusResponse(false,false,"DISABLED",false,false,false,null,false);CleanerConnectAccount account=accounts.findByCleanerId(cleanerId).orElse(null);if(account==null)return new ConnectStatusResponse(true,false,"NOT_STARTED",false,false,false,null,false);if(stripe.isConfigured())sync(account,stripe.retrieveConnectedAccount(account.getStripeAccountId()));return status(account);}

 @Transactional(readOnly=true)
 public CleanerConnectAccount requireReady(Long cleanerId){if(!policy.enabled())throw new IllegalStateException("Marketplace payouts are not enabled");CleanerConnectAccount account=accounts.findByCleanerId(cleanerId).orElseThrow(()->new IllegalStateException("Cleaner has not completed payout onboarding"));if(!ready(account))throw new IllegalStateException("Cleaner payout account is not ready");return account;}

 public void sync(CleanerConnectAccount account,StripeConnectGateway.ConnectedAccountSnapshot snapshot){account.setCountryCode(snapshot.countryCode());account.setDetailsSubmitted(snapshot.detailsSubmitted());account.setChargesEnabled(snapshot.chargesEnabled());account.setPayoutsEnabled(snapshot.payoutsEnabled());account.setRequirementsJson(snapshot.requirementsJson());account.setOnboardingStatus(ready(account)?"READY":snapshot.detailsSubmitted()?"RESTRICTED":"PENDING");accounts.save(account);}
 private CleanerConnectAccount create(Long cleanerId,String email,String country){if(!stripe.isConfigured())throw new IllegalStateException("Stripe Connect is not configured");var snapshot=stripe.createConnectedAccount(email,country,cleanerId,"cleaner:"+cleanerId+":connect-account:v1");CleanerConnectAccount account=new CleanerConnectAccount();account.setCleanerId(cleanerId);account.setStripeAccountId(snapshot.id());sync(account,snapshot);return account;}
 private ConnectStatusResponse status(CleanerConnectAccount a){return new ConnectStatusResponse(true,true,a.getOnboardingStatus(),a.isDetailsSubmitted(),a.isChargesEnabled(),a.isPayoutsEnabled(),a.getCountryCode(),ready(a));}
 private boolean ready(CleanerConnectAccount a){return a.isDetailsSubmitted()&&a.isPayoutsEnabled()&&(!policy.onBehalfOf()||a.isChargesEnabled());}
 private void requireEnabled(){if(!policy.enabled())throw new IllegalStateException("Marketplace payouts are disabled");if(!stripe.isConfigured())throw new IllegalStateException("Stripe Connect is not configured");}
}
