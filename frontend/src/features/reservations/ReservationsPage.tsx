import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { dateTime, money } from '../../shared/format';
import { AuthGate, Empty, ErrorMessage, PageHeader, StatusTag } from '../../shared/ui';

const schema = z.object({
  jobId: z.number().int().positive(),
  cleanerEmail: z.string().email('Email no válido'),
  startAt: z.string().min(1, 'Selecciona fecha y hora'),
  durationMinutes: z.number().int().min(30).max(1440),
});
type FormData = z.infer<typeof schema>;

export function ReservationsPage() {
  const api = useApi();
  const auth = useAuth();
  const queryClient = useQueryClient();
  const reservations = useQuery({ queryKey: ['reservations'], queryFn: api.reservations.list, enabled: !!auth.token });
  const jobs = useQuery({ queryKey: ['jobs', 'open'], queryFn: api.jobs.open, enabled: !!auth.token });
  const cleaners = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { jobId: 0, cleanerEmail: '', startAt: '', durationMinutes: 120 } });
  const create = useMutation({ mutationFn: (data: FormData) => api.reservations.create({ ...data, startAt: new Date(data.startAt).toISOString() }), onSuccess: () => { form.reset({ durationMinutes: 120, jobId: 0, cleanerEmail: '', startAt: '' }); void queryClient.invalidateQueries({ queryKey: ['reservations'] }); } });

  if (!auth.token) return <section className="page"><PageHeader eyebrow="Agenda" title="Reservas">Gestiona horarios y pagos desde una única vista.</PageHeader><AuthGate /></section>;

  return <section className="page"><PageHeader eyebrow="Agenda" title="Tus reservas">El precio mostrado es el importe congelado en el momento de reservar.</PageHeader>
    <div className="split-layout">
      <div className="stack">
        {reservations.error && <ErrorMessage error={reservations.error} />}
        {!reservations.isLoading && (reservations.data?.length ?? 0) === 0 && <Empty>Todavía no tienes reservas.</Empty>}
        {(reservations.data ?? []).map((reservation) => <article className="panel" key={reservation.id}><div className="card-head"><div><span className="muted">Reserva #{reservation.id}</span><h3>{dateTime(reservation.startAt)}</h3></div><StatusTag status={reservation.status} /></div><div className="detail-grid"><span><small>Cleaner</small>{reservation.cleanerEmail}</span><span><small>Duración</small>{reservation.durationMinutes} min</span><span><small>Total</small>{money(reservation.agreedAmountCents, reservation.currency)}</span></div><Link to="/checkout/$reservationId" params={{ reservationId: String(reservation.id) }} className="button primary compact">Ir al pago</Link></article>)}
      </div>
      <aside className="panel form-panel"><span className="eyebrow">Nueva</span><h2>Crear reserva</h2><form onSubmit={form.handleSubmit((data) => create.mutate(data))} className="form-stack"><label>Job<select className="input" {...form.register('jobId', { valueAsNumber: true })} defaultValue=""><option value="" disabled>Selecciona un job</option>{(jobs.data ?? []).map((job) => <option key={job.id} value={job.id}>#{job.id} · {job.title || job.description.slice(0, 40)}</option>)}</select></label><label>Profesional<select className="input" {...form.register('cleanerEmail')} defaultValue=""><option value="" disabled>Selecciona un cleaner</option>{(cleaners.data ?? []).map((cleaner) => <option key={cleaner.email} value={cleaner.email}>{cleaner.name} · {cleaner.email}</option>)}</select></label><label>Fecha y hora<input className="input" type="datetime-local" {...form.register('startAt')} /></label><label>Duración (min)<input className="input" type="number" step="30" {...form.register('durationMinutes', { valueAsNumber: true })} /></label>{Object.values(form.formState.errors).map((error, i) => <small className="field-error" key={i}>{error?.message}</small>)}{create.error && <ErrorMessage error={create.error} />}<button className="button primary" disabled={create.isPending}>Reservar</button></form></aside>
    </div>
  </section>;
}
