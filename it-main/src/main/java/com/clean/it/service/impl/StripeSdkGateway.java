package com.clean.it.service.impl;

import com.clean.it.service.StripeGateway;
import com.clean.it.service.StripeGatewayException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeSdkGateway implements StripeGateway {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_NETWORK_RETRIES = 2;
    private final String secretKey;
    private final String publicKey;

    public StripeSdkGateway(@Value("${stripe.secret-key:}") String secretKey,@Value("${stripe.publishable-key:}") String publicKey) {this.secretKey = secretKey;this.publicKey = publicKey;}
    @Override public boolean isConfigured() {return !secretKey.isBlank() && !publicKey.isBlank();}
    @Override public String publishableKey() {return publicKey;}

    @Override public IntentSnapshot createPaymentIntent(long amountCents,String currency,long reservationId,String idempotencyKey){return createIntent(amountCents,currency,reservationId,null,0,false,idempotencyKey);}
    @Override public IntentSnapshot createDestinationPaymentIntent(long amountCents,String currency,long reservationId,String destinationAccount,long platformFeeCents,boolean onBehalfOf,String idempotencyKey){if(destinationAccount==null||destinationAccount.isBlank())throw new IllegalArgumentException("destinationAccount is required");return createIntent(amountCents,currency,reservationId,destinationAccount,platformFeeCents,onBehalfOf,idempotencyKey);}

    private IntentSnapshot createIntent(long amountCents,String currency,long reservationId,String destinationAccount,long platformFeeCents,boolean onBehalfOf,String idempotencyKey){try{Map<String,Object> params=new HashMap<>();params.put("amount",amountCents);params.put("currency",currency);params.put("automatic_payment_methods",Map.of("enabled",true));params.put("metadata",Map.of("reservationId",String.valueOf(reservationId)));params.put("description","Clean IT reservation "+reservationId);if(destinationAccount!=null){params.put("transfer_data",Map.of("destination",destinationAccount));params.put("application_fee_amount",platformFeeCents);if(onBehalfOf)params.put("on_behalf_of",destinationAccount);}RequestOptions options=requestOptions().setIdempotencyKey(idempotencyKey).build();return snapshot(PaymentIntent.create(params,options));}catch(StripeException e){throw new StripeGatewayException("Stripe could not initialize the payment",e);}}

    @Override public IntentSnapshot retrievePaymentIntent(String paymentIntentId){try{return snapshot(PaymentIntent.retrieve(paymentIntentId,(Map<String,Object>)null,requestOptions().build()));}catch(StripeException e){throw new StripeGatewayException("Stripe could not retrieve the existing payment",e);}}
    @Override public IntentSnapshot cancelPaymentIntent(String paymentIntentId){try{PaymentIntent intent=PaymentIntent.retrieve(paymentIntentId,(Map<String,Object>)null,requestOptions().build());return snapshot(intent.cancel(requestOptions().build()));}catch(StripeException e){throw new StripeGatewayException("Stripe could not cancel the payment",e);}}
    @Override public RefundSnapshot refundPaymentIntent(String paymentIntentId,String idempotencyKey){return refund(paymentIntentId,idempotencyKey,false,false);}
    @Override public RefundSnapshot refundDestinationPaymentIntent(String paymentIntentId,String idempotencyKey,boolean reverseTransfer,boolean refundApplicationFee){return refund(paymentIntentId,idempotencyKey,reverseTransfer,refundApplicationFee);}
    private RefundSnapshot refund(String paymentIntentId,String idempotencyKey,boolean reverseTransfer,boolean refundApplicationFee){try{Map<String,Object> params=new HashMap<>();params.put("payment_intent",paymentIntentId);if(reverseTransfer)params.put("reverse_transfer",true);if(refundApplicationFee)params.put("refund_application_fee",true);Refund refund=Refund.create(params,requestOptions().setIdempotencyKey(idempotencyKey).build());return new RefundSnapshot(refund.getId(),refund.getStatus(),refund.toJson());}catch(StripeException e){throw new StripeGatewayException("Stripe could not refund the payment",e);}}

    @Override public ReconciliationSnapshot reconcileDestinationPaymentIntent(String paymentIntentId){try{PaymentIntent intent=PaymentIntent.retrieve(paymentIntentId,(Map<String,Object>)null,requestOptions().build());String chargeId=intent.getLatestCharge();if(chargeId==null||chargeId.isBlank())return new ReconciliationSnapshot(paymentIntentId,null,null,null);Charge charge=Charge.retrieve(chargeId,(Map<String,Object>)null,requestOptions().build());return new ReconciliationSnapshot(paymentIntentId,chargeId,charge.getTransfer(),charge.getApplicationFee());}catch(StripeException e){throw new StripeGatewayException("Stripe could not reconcile the destination charge",e);}}

    private RequestOptions.RequestOptionsBuilder requestOptions(){return RequestOptions.builder().setApiKey(secretKey).setConnectTimeout(CONNECT_TIMEOUT_MS).setReadTimeout(READ_TIMEOUT_MS).setMaxNetworkRetries(MAX_NETWORK_RETRIES);}
    private IntentSnapshot snapshot(PaymentIntent intent){return new IntentSnapshot(intent.getId(),intent.getClientSecret(),intent.getStatus(),intent.toJson());}
}
