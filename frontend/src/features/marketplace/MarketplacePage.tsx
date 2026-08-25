import { useMemo, useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { money } from '../../shared/format';
import { Empty, ErrorMessage, PageHeader } from '../../shared/ui';
import type { Cleaner } from '../../types';

const DAY:Record<string,string>={MONDAY:'Lun',TUESDAY:'Mar',WEDNESDAY:'Mié',THURSDAY:'Jue',FRIDAY:'Vie',SATURDAY:'Sáb',SUNDAY:'Dom'};

function CleanerProfileCard({cleaner,canBook}:{cleaner:Cleaner;canBook:boolean}) {
  const api=useApi();
  const services=useQuery({queryKey:['cleaner-services',cleaner.email],queryFn:()=>api.cleaners.services(cleaner.email)});
  const availability=useQuery({queryKey:['availability',cleaner.email],queryFn:()=>api.cleaners.availability(cleaner.email)});
  const reviews=useQuery({queryKey:['reviews',cleaner.email],queryFn:()=>api.reviews.list(cleaner.email)});
  return <article className="profile-card profile-detail-card" key={cleaner.email}>
    <div className="profile-primary"><div className="avatar large">{cleaner.name?.[0]??'C'}</div><div><h3>{cleaner.name}</h3><p>{cleaner.email}</p><span className="rating">★ {(cleaner.rating??0).toFixed(1)}</span></div></div>
    <div className="profile-block"><small>Servicios</small><div className="tag-row">{services.data?.map(service=><span className="tag good" key={service.serviceCode}>{service.serviceName} · {money(service.hourlyRateCents)}/h</span>)}{!services.isLoading&&services.data?.length===0&&<span className="muted">Sin tarifas publicadas</span>}</div></div>
    <div className="profile-block"><small>Disponibilidad semanal</small><div className="tag-row">{availability.data?.slice(0,5).map(slot=><span className="tag" key={`${slot.dayOfWeek}-${slot.startTime}`}>{DAY[slot.dayOfWeek]} {slot.startTime.slice(0,5)}–{slot.endTime.slice(0,5)}</span>)}{!availability.isLoading&&availability.data?.length===0&&<span className="muted">Sin horario publicado</span>}</div></div>
    <div className="profile-block"><small>Reseñas verificadas</small>{reviews.data?.slice(0,2).map(review=><p className="profile-review" key={review.id}><span className="rating">★ {review.rating}</span> {review.comment||'Servicio completado y verificado'}</p>)}{!reviews.isLoading&&reviews.data?.length===0&&<p className="muted">Todavía no tiene reseñas verificadas.</p>}</div>
    {canBook&&<Link to="/book" className="button primary">Reservar</Link>}
  </article>;
}

export function MarketplacePage() {
  const api = useApi();
  const auth=useAuth();
  const [term, setTerm] = useState('');
  const query = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const visible = useMemo(() => (query.data ?? []).filter((cleaner) => `${cleaner.name} ${cleaner.email}`.toLowerCase().includes(term.toLowerCase())), [query.data, term]);

  return <section className="page"><PageHeader eyebrow="Explorar" title="Profesionales">Compara valoración, servicios, tarifas, disponibilidad semanal y reseñas verificadas antes de reservar.</PageHeader>
    <div className="toolbar"><input className="input" value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Buscar por nombre o email" aria-label="Buscar profesionales" /></div>
    {query.error && <ErrorMessage error={query.error} />}
    {!query.isLoading && visible.length === 0 ? <Empty>No hay profesionales que coincidan con la búsqueda.</Empty> : <div className="profile-grid profile-detail-grid">{visible.map((cleaner) => <CleanerProfileCard key={cleaner.email} cleaner={cleaner} canBook={auth.roles.includes('CLIENT')}/>)}</div>}
  </section>;
}
