package com.clean.it.service;

import com.clean.it.domain.CleanerConnectAccount;
import com.clean.it.domain.StripePayout;
import com.clean.it.repository.CleanerConnectAccountRepository;
import com.clean.it.repository.ConnectEventRepository;
import com.clean.it.repository.StripePayoutRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class StripeConnectWebhookService {
 private static final Duration LEASE=Duration.ofMinutes(15); private static final int MAX_REASON=1000; private static final Set<String> PAYOUT_EVENTS=Set.of("payout.created","payout.updated","payout.paid","payout.failed","payout.canceled");
 private final ConnectEventRepository events; private final CleanerConnectAccountRepository accounts; private final StripePayoutRepository payouts; private final ConnectAccountService accountService; private final TransactionTemplate tx;
 public StripeConnectWebhookService(ConnectEventRepository events,CleanerConnectAccountRepository accounts,StripePayoutRepository payouts,ConnectAccountService accountService,PlatformTransactionManager manager){this.events=events;this.accounts=accounts;this.payouts=payouts;this.accountService=accountService;this.tx=new TransactionTemplate(manager);}
 public boolean process(Event event){Instant now=Instant.now(),created=event.getCreated()==null?now:Instant.ofEpochSecond(event.getCreated());Boolean claimed=tx.execute(s->events.claim(event.getId(),event.getType(),event.getAccount(),created,now,now.minus(LEASE))>0);if(!Boolean.TRUE.equals(claimed))return false;try{tx.executeWithoutResult(s->processClaimed(event));return true;}catch(RuntimeException e){tx.executeWithoutResult(s->events.markFailed(event.getId(),safe(e)));throw e;}}
 private void processClaimed(Event event){if("account.updated".equals(event.getType()))syncAccount(object(event,Account.class));else if(PAYOUT_EVENTS.contains(event.getType()))syncPayout(event.getAccount(),object(event,Payout.class));if(events.markProcessed(event.getId(),Instant.now())!=1)throw new IllegalStateException("Connect event processing lease was lost");}
 private void syncAccount(Account stripeAccount){accounts.findByStripeAccountId(stripeAccount.getId()).ifPresent(account->accountService.sync(account,new StripeConnectGateway.ConnectedAccountSnapshot(stripeAccount.getId(),stripeAccount.getCountry(),Boolean.TRUE.equals(stripeAccount.getDetailsSubmitted()),Boolean.TRUE.equals(stripeAccount.getChargesEnabled()),Boolean.TRUE.equals(stripeAccount.getPayoutsEnabled()),null)));}
 private void syncPayout(String stripeAccountId,Payout payout){if(stripeAccountId==null)return;CleanerConnectAccount account=accounts.findByStripeAccountId(stripeAccountId).orElse(null);if(account==null)return;StripePayout stored=payouts.findByStripePayoutId(payout.getId()).orElseGet(StripePayout::new);stored.setConnectAccountId(account.getId());stored.setStripePayoutId(payout.getId());stored.setAmountCents(payout.getAmount());stored.setCurrency(payout.getCurrency()==null?"eur":payout.getCurrency().toLowerCase(Locale.ROOT));stored.setStatus(payout.getStatus());stored.setArrivalAt(payout.getArrivalDate()==null?null:Instant.ofEpochSecond(payout.getArrivalDate()));stored.setFailureCode(payout.getFailureCode());stored.setRawJson(payout.toJson());payouts.save(stored);}
 private <T extends StripeObject> T object(Event event,Class<T> type){EventDataObjectDeserializer d=event.getDataObjectDeserializer();StripeObject object=d.getObject().orElseGet(()->unsafe(d));if(!type.isInstance(object))throw new IllegalArgumentException("Stripe Connect event payload type does not match "+event.getType());return type.cast(object);}
 private StripeObject unsafe(EventDataObjectDeserializer d){try{return d.deserializeUnsafe();}catch(EventDataObjectDeserializationException e){throw new IllegalArgumentException("Stripe Connect event payload could not be deserialized",e);}}
 private String safe(RuntimeException e){String m=e.getMessage();if(m==null||m.isBlank())m=e.getClass().getSimpleName();return m.length()<=MAX_REASON?m:m.substring(0,MAX_REASON);}
}
