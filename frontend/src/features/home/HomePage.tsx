import { Link } from '@tanstack/react-router';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api';

export function HomePage() {
  const api = useApi();
  const cleaners = useQuery({ queryKey: ['cleaners'], queryFn: api.cleaners.list });
  const top = [...(cleaners.data ?? [])].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0)).slice(0, 3);

  return <>
    <section className="hero">
      <div>
        <span className="eyebrow">Marketplace de limpieza</span>
        <h1>Tu casa impecable, sin complicaciones.</h1>
        <p>Encuentra profesionales, reserva un horario y paga de forma segura desde una experiencia pensada para cliente y cleaner.</p>
        <div className="hero-actions"><Link to="/marketplace" className="button primary">Encontrar profesional</Link><Link to="/jobs" className="button secondary">Ver trabajos</Link></div>
      </div>
      <div className="hero-card">
        <span className="kicker">Próximo paso</span><strong>1. Elige profesional</strong><strong>2. Reserva horario</strong><strong>3. Paga con Stripe</strong>
      </div>
    </section>

    <section className="section"><div className="section-heading"><div><span className="eyebrow">Confianza</span><h2>Profesionales destacados</h2></div><Link to="/marketplace">Ver todos →</Link></div>
      <div className="card-grid">{top.map((cleaner) => <article className="profile-card" key={cleaner.email}><div className="avatar">{cleaner.name?.[0] ?? 'C'}</div><div><h3>{cleaner.name}</h3><p>{cleaner.email}</p><span className="rating">★ {(cleaner.rating ?? 0).toFixed(1)}</span></div></article>)}</div>
    </section>

    <section className="feature-grid"><article><span>01</span><h3>Precio protegido</h3><p>El importe se congela al reservar y se calcula en el servidor.</p></article><article><span>02</span><h3>Pago seguro</h3><p>Stripe Payment Element gestiona los datos sensibles del pago.</p></article><article><span>03</span><h3>Reserva consistente</h3><p>PostgreSQL impide solapes aunque existan varias instancias del backend.</p></article></section>
  </>;
}
