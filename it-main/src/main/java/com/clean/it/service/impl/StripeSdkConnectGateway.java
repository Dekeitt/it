package com.clean.it.service.impl;

import com.clean.it.service.MarketplacePaymentPolicy;
import com.clean.it.service.StripeConnectGateway;
import com.clean.it.service.StripeGatewayException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeSdkConnectGateway implements StripeConnectGateway {
 private static final int CONNECT_TIMEOUT_MS=10_000,READ_TIMEOUT_MS=30_000,MAX_NETWORK_RETRIES=2;
 private final String secretKey; private final MarketplacePaymentPolicy policy;
 public StripeSdkConnectGateway(@Value("${stripe.secret-key:}") String secretKey,MarketplacePaymentPolicy policy){this.secretKey=secretKey;this.policy=policy;}
 @Override public boolean isConfigured(){return !secretKey.isBlank();}
 @Override public ConnectedAccountSnapshot createConnectedAccount(String email,String countryCode,Long cleanerId,String idempotencyKey){try{Map<String,Object> capabilities=new HashMap<>();capabilities.put("transfers",Map.of("requested",true));if(policy.onBehalfOf())capabilities.put("card_payments",Map.of("requested",true));Map<String,Object> params=new HashMap<>();params.put("type","express");params.put("country",countryCode);params.put("email",email);params.put("capabilities",capabilities);params.put("metadata",Map.of("cleanItCleanerId",String.valueOf(cleanerId)));return snapshot(Account.create(params,options(idempotencyKey)));}catch(StripeException e){throw new StripeGatewayException("Stripe could not create the connected account",e);}}
 @Override public ConnectedAccountSnapshot retrieveConnectedAccount(String accountId){try{return snapshot(Account.retrieve(accountId,(Map<String,Object>)null,options(null)));}catch(StripeException e){throw new StripeGatewayException("Stripe could not retrieve the connected account",e);}}
 @Override public String createOnboardingLink(String accountId,String refreshUrl,String returnUrl,String idempotencyKey){try{AccountLink link=AccountLink.create(Map.of("account",accountId,"refresh_url",refreshUrl,"return_url",returnUrl,"type","account_onboarding"),options(idempotencyKey));return link.getUrl();}catch(StripeException e){throw new StripeGatewayException("Stripe could not create the onboarding link",e);}}
 private ConnectedAccountSnapshot snapshot(Account a){return new ConnectedAccountSnapshot(a.getId(),a.getCountry(),Boolean.TRUE.equals(a.getDetailsSubmitted()),Boolean.TRUE.equals(a.getChargesEnabled()),Boolean.TRUE.equals(a.getPayoutsEnabled()),null);}
 private RequestOptions options(String idempotencyKey){RequestOptions.RequestOptionsBuilder b=RequestOptions.builder().setApiKey(secretKey).setConnectTimeout(CONNECT_TIMEOUT_MS).setReadTimeout(READ_TIMEOUT_MS).setMaxNetworkRetries(MAX_NETWORK_RETRIES);if(idempotencyKey!=null&&!idempotencyKey.isBlank())b.setIdempotencyKey(idempotencyKey);return b.build();}
}
