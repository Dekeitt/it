import { Link, Outlet } from '@tanstack/react-router';
import { useAuth } from '../auth';

const nav = [
  ['/', 'Inicio'],
  ['/marketplace', 'Profesionales'],
  ['/jobs', 'Jobs'],
  ['/reservations', 'Reservas'],
  ['/payments', 'Pagos'],
  ['/reviews', 'Reseñas'],
] as const;

export function Shell() {
  const auth = useAuth();
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="brand" aria-label="Clean IT, inicio">
          <span className="brand-mark">C</span>
          <span><strong>Clean IT</strong><small>Servicios de confianza</small></span>
        </Link>
        <nav className="desktop-nav" aria-label="Navegación principal">
          {nav.map(([to, label]) => <Link key={to} to={to} activeProps={{ className: 'active' }}>{label}</Link>)}
        </nav>
        <Link to="/account" className={`account-chip ${auth.token ? 'online' : ''}`}>
          <span className="status-dot" />{auth.subject ?? (auth.token ? 'Sesión' : 'Acceder')}
        </Link>
      </header>

      <main><Outlet /></main>

      <nav className="mobile-nav" aria-label="Navegación móvil">
        {nav.slice(0, 5).map(([to, label]) => <Link key={to} to={to} activeProps={{ className: 'active' }}>{label}</Link>)}
      </nav>
      <footer className="footer">Clean IT · Frontend independiente · API Spring Boot</footer>
    </div>
  );
}
