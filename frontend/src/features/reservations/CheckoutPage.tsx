import { useEffect, useMemo, useRef, useState } from 'react';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { useMutation } from '@tanstack/react-query';
import { useParams } from '@tanstack/react-router';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { money } from '../../shared/format';
import { AuthGate, ErrorMessage, PageHeader } from '../../shared/ui';
import type { PaymentIntentResponse } from '../../types';

function PaymentForm({ intent }: { intent: PaymentIntentResponse }) {
  const stripe = useStripe();
  const elements = useElements();
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const returnedSecret = new URLSearchParams(window.location.search).get('payment_intent_client_secret');
    if (!stripe || !returnedSecret) return;
    if (returnedSecret !== intent.clientSecret) {
      setMessage('La respuesta de Stripe no corresponde a esta reserva.');
      return;
    }
    void stripe.retrievePaymentIntent(returnedSecret).then(({ paymentIntent }) => {
      if (paymentIntent) setMessage(`Stripe informa: ${paymentIntent.status}. El webhook del servidor confirmará el estado definitivo.`);
    });
  }, [intent.clientSecret, stripe]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!stripe || !elements) return;
    setSubmitting(true);
    setMessage('');
    const result = await stripe.confirmPayment({ elements, confirmParams: { return_url: window.location.href.split('?')[0] } });
    if (result.error) setMessage(result.error.message ?? 'No se pudo confirmar el pago.');
    setSubmitting(false);
  }

  return <form onSubmit={submit} className="payment-form"><PaymentElement /><button className="button primary" disabled={!stripe || submitting}>{submitting ? 'Confirmando…' : `Pagar ${money(intent.amountCents, intent.currency)}`}</button>{message && <div className="notice" role="status">{message}</div>}</form>;
}

export function CheckoutPage() {
  const { reservationId } = useParams({ from: '/checkout/$reservationId' });
  const auth = useAuth();
  const api = useApi();
  const started = useRef(false);
  const mutation = useMutation({ mutationFn: () => api.payments.createIntent(Number(reservationId)) });
  useEffect(() => { if (auth.token && !started.current) { started.current = true; mutation.mutate(); } }, [auth.token, mutation]);
  const stripePromise = useMemo(() => mutation.data?.publishableKey ? loadStripe(mutation.data.publishableKey) : null, [mutation.data?.publishableKey]);

  if (!auth.token) return <section className="page narrow"><PageHeader eyebrow="Checkout" title="Pago seguro" /><AuthGate /></section>;
  return <section className="page narrow"><PageHeader eyebrow="Checkout" title={`Reserva #${reservationId}`}>Los datos de tarjeta se introducen directamente en Stripe.</PageHeader>{mutation.error && <ErrorMessage error={mutation.error} />}{mutation.isPending && <div className="panel">Preparando PaymentIntent…</div>}{mutation.data && stripePromise && <div className="panel checkout-card"><div className="checkout-summary"><span>Total</span><strong>{money(mutation.data.amountCents, mutation.data.currency)}</strong></div><Elements stripe={stripePromise} options={{ clientSecret: mutation.data.clientSecret, appearance: { theme: 'night' } }}><PaymentForm intent={mutation.data} /></Elements></div>}</section>;
}
