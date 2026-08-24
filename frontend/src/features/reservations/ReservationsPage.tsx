import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { dateTime, money } from '../../shared/format';
import { AuthGate, Empty, ErrorMessage, PageHeader, StatusTag } from '../../shared/ui';
import type { Reservation } from '../../types';

const schema = z.object({
  jobId: z.number().int().positive(),
  cleanerEmail: z.string().email('Email no válido'),
  startAt: z.string().min(1, 'Selecciona fecha y hora'),
  durationMinutes: z.number().int().min(30).max(1440),
});
type FormData = z.infer<typeof schema>;

function iso(value: string) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toISOString();
}

function ReservationCard({ reservation }: { reservation: Reservation }) {
  const api = useApi();
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [rescheduleAt, setRescheduleAt] = useState('');
  const [rescheduleDuration, setRescheduleDuration] = useState(reservation.durationMinutes);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [reviewSent, setReviewSent] = useState(false);

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['reservations'] });
    void queryClient.invalidateQueries({ queryKey: ['payments'] });
  };
  const cancel = useMutation({ mutationFn: () => api.reservations.cancel(reservation.id), onSuccess: refresh });
  const start = useMutation({ mutationFn: () => api.reservations.start(reservation.id), onSuccess: refresh });
  const complete = useMutation({ mutationFn: () => api.reservations.complete(reservation.id), onSuccess: refresh });
  const reschedule = useMutation({
    mutationFn: () => api.reservations.reschedule(reservation.id, {
      startAt: iso(rescheduleAt), durationMinutes: rescheduleDuration,
    }),
    onSuccess: () => { setRescheduleAt(''); refresh(); },
  });
  const review = useMutation({
    mutationFn: () => api.reservations.review(reservation.id, { rating, comment: comment || undefined }),
    onSuccess: () => { setReviewSent(true); void queryClient.invalidateQueries({ queryKey: ['reviews', reservation.cleanerEmail] }); },
  });

  const status = reservation.status.toUpperCase();
  const isClient = auth.roles.includes('CLIENT');
  const isCleaner = auth.roles.includes('CLEANER');
  const anyError = cancel.error || start.error || complete.error || reschedule.error || review.error;

  return <article className="panel">
    <div className="card-head"><div><span className="muted">Reserva #{reservation.id}</span><h3>{dateTime(reservation.startAt)}</h3></div><StatusTag status={reservation.status} /></div>
    <div className="detail-grid"><span><small>Cleaner</small>{reservation.cleanerEmail}</span><span><small>Duración</small>{reservation.durationMinutes} min</span><span><small>Total</small>{money(reservation.agreedAmountCents, reservation.currency)}</span></div>

    {isClient && status === 'SCHEDULED' && <div className="stack">
      <div className="actions"><Link to="/checkout/$reservationId" params={{ reservationId: String(reservation.id) }} className="button primary compact">Ir al pago</Link><button className="button secondary compact" disabled={cancel.isPending} onClick={() => cancel.mutate()}>Cancelar</button></div>
      <details><summary className="muted">Reprogramar</summary><div className="form-stack"><label>Nueva fecha y hora<input className="input" type="datetime-local" value={rescheduleAt} onChange={(event) => setRescheduleAt(event.target.value)} /></label><label>Duración (min)<input className="input" type="number" min="30" max="1440" step="30" value={rescheduleDuration} onChange={(event) => setRescheduleDuration(Number(event.target.value))} /></label><button className="button secondary compact" disabled={!iso(rescheduleAt) || reschedule.isPending} onClick={() => reschedule.mutate()}>Guardar nueva hora</button></div></details>
    </div>}

    {isCleaner && status === 'SCHEDULED' && <button className="button primary compact" disabled={start.isPending} onClick={() => start.mutate()}>Comenzar servicio</button>}
    {isCleaner && status === 'IN_PROGRESS' && <button className="button primary compact" disabled={complete.isPending} onClick={() => complete.mutate()}>Marcar como completado</button>}

    {isClient && status === 'COMPLETED' && !reviewSent && <details><summary className="muted">Valorar servicio</summary><div className="form-stack"><label>Puntuación<select className="input" value={rating} onChange={(event) => setRating(Number(event.target.value))}><option value="5">5 · Excelente</option><option value="4">4 · Muy bien</option><option value="3">3 · Bien</option><option value="2">2 · Mejorable</option><option value="1">1 · Mala</option></select></label><label>Comentario<textarea className="input textarea" value={comment} onChange={(event) => setComment(event.target.value)} /></label><button className="button primary compact" disabled={review.isPending} onClick={() => review.mutate()}>Publicar reseña verificada</button></div></details>}
    {reviewSent && <div className="notice">Reseña publicada para esta reserva.</div>}
    {anyError && <ErrorMessage error={anyError} />}
  </article>;
}

export function ReservationsPage() {
  const api = useApi();
  const auth = useAuth();
  const queryClient = useQueryClient();
  const reservations = useQuery({ queryKey: ['reservations'], queryFn: api.reservations.list, enabled: !!auth.token });
  const jobs = useQuery({ queryKey: ['jobs', 'open'], queryFn: api.jobs.open, enabled: !!auth.token && auth.roles.includes('CLIENT') });
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { jobId: 0, cleanerEmail: '', startAt: '', durationMinutes: 120 } });
  const startAt = form.watch('startAt');
  const durationMinutes = form.watch('durationMinutes');
  const startAtIso = iso(startAt);
  const availableCleaners = useQuery({
    queryKey: ['cleaners', 'available', startAtIso, durationMinutes],
    queryFn: () => api.cleaners.available(startAtIso, durationMinutes),
    enabled: !!startAtIso && Number.isFinite(durationMinutes) && durationMinutes >= 30,
  });
  const create = useMutation({
    mutationFn: (data: FormData) => api.reservations.create({ ...data, startAt: new Date(data.startAt).toISOString() }),
    onSuccess: () => {
      form.reset({ durationMinutes: 120, jobId: 0, cleanerEmail: '', startAt: '' });
      void queryClient.invalidateQueries({ queryKey: ['reservations'] });
    },
  });

  if (!auth.token) return <section className="page"><PageHeader eyebrow="Agenda" title="Reservas">Gestiona horarios, servicio, pago y reseña desde una única vista.</PageHeader><AuthGate /></section>;
  const isClient = auth.roles.includes('CLIENT');

  return <section className="page"><PageHeader eyebrow="Agenda" title="Tus reservas">Las reservas ahora tienen ciclo SCHEDULED → IN_PROGRESS → COMPLETED, con cancelación, reprogramación y reseña verificada.</PageHeader>
    <div className="split-layout">
      <div className="stack">
        {reservations.error && <ErrorMessage error={reservations.error} />}
        {!reservations.isLoading && (reservations.data?.length ?? 0) === 0 && <Empty>Todavía no tienes reservas.</Empty>}
        {(reservations.data ?? []).map((reservation) => <ReservationCard reservation={reservation} key={reservation.id} />)}
      </div>
      {isClient && <aside className="panel form-panel"><span className="eyebrow">Nueva</span><h2>Crear reserva</h2><p className="muted">Primero elige horario. Después solo aparecen profesionales que han declarado disponibilidad y no tienen otra reserva solapada.</p><form onSubmit={form.handleSubmit((data) => create.mutate(data))} className="form-stack"><label>Job<select className="input" {...form.register('jobId', { valueAsNumber: true })} defaultValue=""><option value="" disabled>Selecciona un job</option>{(jobs.data ?? []).map((job) => <option key={job.id} value={job.id}>#{job.id} · {job.title || job.description.slice(0, 40)}</option>)}</select></label><label>Fecha y hora<input className="input" type="datetime-local" {...form.register('startAt')} /></label><label>Duración (min)<input className="input" type="number" step="30" {...form.register('durationMinutes', { valueAsNumber: true })} /></label><label>Profesional<select className="input" {...form.register('cleanerEmail')} defaultValue="" disabled={!startAtIso}><option value="" disabled>{startAtIso ? 'Selecciona un cleaner disponible' : 'Elige primero fecha y duración'}</option>{(availableCleaners.data ?? []).map((cleaner) => <option key={cleaner.email} value={cleaner.email}>{cleaner.name} · ★ {(cleaner.rating ?? 0).toFixed(1)}</option>)}</select></label>{startAtIso && !availableCleaners.isLoading && (availableCleaners.data?.length ?? 0) === 0 && <small className="field-error">No hay cleaners con disponibilidad declarada para ese intervalo.</small>}{Object.values(form.formState.errors).map((error, i) => <small className="field-error" key={i}>{error?.message}</small>)}{availableCleaners.error && <ErrorMessage error={availableCleaners.error} />}{create.error && <ErrorMessage error={create.error} />}<button className="button primary" disabled={create.isPending || !startAtIso || (availableCleaners.data?.length ?? 0) === 0}>Reservar</button></form></aside>}
    </div>
  </section>;
}
