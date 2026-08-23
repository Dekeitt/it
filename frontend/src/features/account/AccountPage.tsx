import { useState } from 'react';
import { useAuth } from '../../auth';
import { PageHeader } from '../../shared/ui';

export function AccountPage() {
  const auth = useAuth();
  const [draft, setDraft] = useState(auth.token);
  return <section className="page narrow"><PageHeader eyebrow="Identidad" title="Cuenta">El backend aún no incluye un proveedor de login. Para desarrollo, introduce un JWT emitido para Clean IT.</PageHeader>
    <div className="panel form-panel"><label>Access token<textarea className="input textarea token-input" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="eyJ…" autoComplete="off" spellCheck={false} /></label><div className="actions"><button className="button primary" onClick={() => auth.setToken(draft)}>Guardar para esta pestaña</button><button className="button secondary" onClick={() => { setDraft(''); auth.clear(); }}>Cerrar sesión</button></div><div className="session-info"><span><small>Subject</small>{auth.subject ?? '—'}</span><span><small>Roles</small>{auth.roles.join(', ') || '—'}</span></div><p className="security-note">El token se guarda únicamente en <code>sessionStorage</code> para mantener el flujo de desarrollo existente. La siguiente evolución recomendada es OIDC Authorization Code + PKCE y refresh token HttpOnly.</p></div>
  </section>;
}
