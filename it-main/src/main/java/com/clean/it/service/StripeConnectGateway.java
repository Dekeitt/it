package com.clean.it.service;

public interface StripeConnectGateway {
 boolean isConfigured();
 ConnectedAccountSnapshot createConnectedAccount(String email,String countryCode,Long cleanerId,String idempotencyKey);
 ConnectedAccountSnapshot retrieveConnectedAccount(String stripeAccountId);
 String createOnboardingLink(String stripeAccountId,String refreshUrl,String returnUrl,String idempotencyKey);

 record ConnectedAccountSnapshot(String id,String countryCode,boolean detailsSubmitted,boolean chargesEnabled,boolean payoutsEnabled,String requirementsJson){}
}
