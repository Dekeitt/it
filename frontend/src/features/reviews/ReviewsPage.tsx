import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { dateTime } from '../../shared/format';
import { Empty, ErrorMessage, PageHeader } from '../../shared/ui';

const schema = z.object({ rating: z.number().int().min(1).max(5), comment: z.string().max(2000).optional() });
type FormData = z.infer<typeof schema>;

export function ReviewsPage() {
  const api = useApi();
  const auth = useAuth();
  const queryClient = useQueryClient();
  const cleaners = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const [email, setEmail] = useState('');
  useEffect(() => { if (!email && cleaners.data?.[0]) setEmail(cleaners.data[0].email); }, [cleaners.data, email]);
  const reviews = useQuery({ queryKey: ['reviews', email], queryFn: () => api.reviews.list(email), enabled: !!email });
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { rating: 5, comment: '' } });
  const create = useMutation({ mutationFn: (data: FormData) => api.reviews.create(email, data), onSuccess: () => { form.reset({ rating: 5, comment: '' }); void queryClient.invalidateQueries({ queryKey: ['reviews', email] }); } });

  return <section className="page"><PageHeader eyebrow="Reputación" title="Reseñas">Consulta opiniones públicas o añade una nueva con una sesión de cliente.</PageHeader>
    <div className="split-layout"><div className="stack"><label className="select-label">Profesional<select className="input" value={email} onChange={(e) => setEmail(e.target.value)}>{(cleaners.data ?? []).map((cleaner) => <option key={cleaner.email} value={cleaner.email}>{cleaner.name}</option>)}</select></label>{reviews.error && <ErrorMessage error={reviews.error} />}{!reviews.isLoading && (reviews.data?.length ?? 0) === 0 && <Empty>Este profesional todavía no tiene reseñas.</Empty>}{(reviews.data ?? []).map((review) => <article className="panel review-card" key={review.id}><div className="card-head"><strong>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</strong><span className="muted">{dateTime(review.createdAt)}</span></div><p>{review.comment || 'Sin comentario.'}</p><small className="muted">{review.clientEmail}</small></article>)}</div>
      <aside className="panel form-panel"><span className="eyebrow">Tu experiencia</span><h2>Escribir reseña</h2>{!auth.token ? <p className="muted">Inicia una sesión para publicar una reseña.</p> : <form className="form-stack" onSubmit={form.handleSubmit((data) => create.mutate(data))}><label>Puntuación<select className="input" {...form.register('rating', { valueAsNumber: true })}><option value="5">5 · Excelente</option><option value="4">4 · Muy bien</option><option value="3">3 · Bien</option><option value="2">2 · Mejorable</option><option value="1">1 · Mala</option></select></label><label>Comentario<textarea className="input textarea" {...form.register('comment')} /></label>{create.error && <ErrorMessage error={create.error} />}<button className="button primary" disabled={!email || create.isPending}>Publicar reseña</button></form>}</aside></div>
  </section>;
}
