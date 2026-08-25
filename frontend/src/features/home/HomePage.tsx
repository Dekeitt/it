import { Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api';
import { useAuth } from '../../auth';

export function HomePage() {
  const api = useApi();
  const auth = useAuth();
  const cleaners = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const top = [...(cleaners.data ?? [])].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0)).slice(0, 3);
  const canBook = auth.roles.includes('CLIENT');

  return <>
    <section className="hero">
      <div>
        <span className="eyebrow">Marketplace de limpieza</span>
        <h1>Tu casa impecable, sin complicaciones.</h1>
        <p>Elige servicio, dirección y horario. Clean IT encuentra profesionales que realmente cubren tu zona y están disponibles, calcula el total y te lleva al pago.</p>
        <div className="hero-actions">{canBook?<Link to="/book" className="button primary">Reservar limpieza</Link>:<Link to="/account" className="button primary">Acceder para reservar</Link>}<Link to="/marketplace" className="button secondary">Explorar profesionales</Link></div>
      </div>
      <div className="hero-card">
        <span className="kicker">Reserva directa</span><strong>1. Servicio y dirección</strong><strong>2. Horario y profesional</strong><strong>3. Precio final y pago</strong>
      </div>
    </section>

    <section className="section"><div className="section-heading"><div><span className="eyebrow">Confianza</span><h2>Profesionales destacados</h2></div><Link to="/marketplace">Ver todos →</Link></div>
      <div className="card-grid">{top.map((cleaner) => <article className="profile-card" key={cleaner.email}><div className="avatar">{cleaner.name?.[0] ?? 'C'}</div><div><h3>{cleaner.name}</h3><p>{cleaner.email}</p><span className="rating">★ {(cleaner.rating ?? 0).toFixed(1)}</span></div></article>)}</div>
    </section>

    <section className="feature-grid"><article><span>01</span><h3>Precio de servidor</h3><p>La tarifa se calcula desde la oferta del profesional y el importe se congela al reservar.</p></article><article><span>02</span><h3>Cobertura real</h3><p>Un profesional fuera de tu zona o con otro servicio solapado no aparece como reservable.</p></article><article><span>03</span><h3>Pago seguro</h3><p>Stripe Payment Element gestiona los datos sensibles del pago después de crear la reserva.</p></article></section>
  </>;
}
