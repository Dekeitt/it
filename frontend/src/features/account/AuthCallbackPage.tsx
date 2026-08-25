import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../auth';

export function AuthCallbackPage() {
  const auth = useAuth();
  const started = useRef(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    void auth.completeLogin()
      .then((returnTo) => window.location.replace(returnTo))
      .catch((cause: unknown) => {
        setError(cause instanceof Error ? cause.message : 'No se pudo completar el login');
      });
  }, [auth]);

  return <section className="page narrow">
    <div className="panel form-panel">
      <span className="eyebrow">Identidad</span>
      <h1>{error ? 'No se pudo iniciar sesión' : 'Completando inicio de sesión…'}</h1>
      {error
        ? <><p className="error-message">{error}</p><a className="button secondary" href="/account">Volver a cuenta</a></>
        : <p className="muted">Validando la respuesta del proveedor OIDC y obteniendo el access token mediante PKCE.</p>}
    </div>
  </section>;
}
