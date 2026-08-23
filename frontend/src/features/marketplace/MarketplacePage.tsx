import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api';
import { Empty, ErrorMessage, PageHeader } from '../../shared/ui';

export function MarketplacePage() {
  const api = useApi();
  const [term, setTerm] = useState('');
  const query = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const visible = useMemo(() => (query.data ?? []).filter((cleaner) => `${cleaner.name} ${cleaner.email}`.toLowerCase().includes(term.toLowerCase())), [query.data, term]);

  return <section className="page"><PageHeader eyebrow="Explorar" title="Profesionales">Compara perfiles y valoraciones antes de crear tu reserva.</PageHeader>
    <div className="toolbar"><input className="input" value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Buscar por nombre o email" aria-label="Buscar profesionales" /></div>
    {query.error && <ErrorMessage error={query.error} />}
    {!query.isLoading && visible.length === 0 ? <Empty>No hay profesionales que coincidan con la búsqueda.</Empty> : <div className="profile-grid">{visible.map((cleaner) => <article className="profile-card large" key={cleaner.email}><div className="avatar large">{cleaner.name?.[0] ?? 'C'}</div><div><h3>{cleaner.name}</h3><p>{cleaner.email}</p><span className="rating">★ {(cleaner.rating ?? 0).toFixed(1)}</span></div></article>)}</div>}
  </section>;
}
