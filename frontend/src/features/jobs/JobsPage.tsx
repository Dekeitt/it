import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { money } from '../../shared/format';
import { AuthGate, Empty, ErrorMessage, PageHeader, StatusTag } from '../../shared/ui';

const schema = z.object({
  title: z.string().max(120).optional(),
  description: z.string().min(1, 'Describe el trabajo').max(1000),
  priceEuros: z.coerce.number().min(0, 'El precio no puede ser negativo').max(100000),
});
type FormData = z.infer<typeof schema>;

export function JobsPage() {
  const api = useApi();
  const auth = useAuth();
  const client = useQueryClient();
  const jobs = useQuery({ queryKey: ['jobs', 'open'], queryFn: api.jobs.open, enabled: !!auth.token });
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { title: '', description: '', priceEuros: 45 } });
  const create = useMutation({
    mutationFn: (data: FormData) => api.jobs.create({ title: data.title || undefined, description: data.description, priceCents: Math.round(data.priceEuros * 100) }),
    onSuccess: () => { form.reset(); void client.invalidateQueries({ queryKey: ['jobs'] }); },
  });
  const accept = useMutation({ mutationFn: api.jobs.accept, onSuccess: () => void client.invalidateQueries({ queryKey: ['jobs'] }) });

  if (!auth.token) return <section className="page"><PageHeader eyebrow="Trabajo" title="Jobs">Publica o acepta trabajos según tu rol.</PageHeader><AuthGate /></section>;
  const isClient = auth.roles.includes('CLIENT');
  const isCleaner = auth.roles.includes('CLEANER');

  return <section className="page"><PageHeader eyebrow="Trabajo" title="Jobs abiertos">La API sigue siendo la fuente de verdad para permisos y estados.</PageHeader>
    <div className="split-layout">
      <div className="stack">
        {jobs.error && <ErrorMessage error={jobs.error} />}
        {accept.error && <ErrorMessage error={accept.error} />}
        {!jobs.isLoading && (jobs.data?.length ?? 0) === 0 && <Empty>No hay jobs abiertos.</Empty>}
        {(jobs.data ?? []).map((job) => <article className="panel job-card" key={job.id}><div className="card-head"><div><span className="muted">Job #{job.id}</span><h3>{job.title || 'Servicio de limpieza'}</h3></div><StatusTag status={job.status} /></div><p>{job.description}</p><div className="meta-row"><strong>{money(job.priceCents)}</strong>{job.cleanerEmail && <span>{job.cleanerEmail}</span>}</div>{isCleaner && <button className="button primary compact" disabled={accept.isPending} onClick={() => accept.mutate(job.id)}>Aceptar trabajo</button>}</article>)}
      </div>
      <aside className="panel form-panel"><span className="eyebrow">Cliente</span><h2>Publicar trabajo</h2>{!isClient ? <p className="muted">Tu JWT no contiene el rol CLIENT.</p> : <form onSubmit={form.handleSubmit((data) => create.mutate(data))} className="form-stack"><label>Título<input className="input" {...form.register('title')} /></label><label>Descripción<textarea className="input textarea" {...form.register('description')} /></label><label>Precio estimado (€)<input className="input" type="number" step="0.01" {...form.register('priceEuros')} /></label>{Object.values(form.formState.errors).map((error, i) => <small className="field-error" key={i}>{error?.message}</small>)}{create.error && <ErrorMessage error={create.error} />}<button className="button primary" disabled={create.isPending}>Publicar</button></form>}</aside>
    </div>
  </section>;
}
