import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

const STORAGE_KEY = 'clean-it-access-token';
const OIDC_STATE_KEY = 'clean-it-oidc-state';
const OIDC_VERIFIER_KEY = 'clean-it-oidc-code-verifier';
const OIDC_RETURN_TO_KEY = 'clean-it-oidc-return-to';

const authority = (import.meta.env.VITE_OIDC_AUTHORITY ?? '').replace(/\/$/, '');
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID ?? '';
const scope = import.meta.env.VITE_OIDC_SCOPE ?? 'openid profile email';
const audience = import.meta.env.VITE_OIDC_AUDIENCE ?? '';

type OidcDiscovery = {
  authorization_endpoint: string;
  token_endpoint: string;
  end_session_endpoint?: string;
};

type AuthContextValue = {
  token: string;
  subject?: string;
  roles: string[];
  oidcConfigured: boolean;
  busy: boolean;
  error?: string;
  setToken: (token: string) => void;
  clear: () => void;
  login: (returnTo?: string) => Promise<void>;
  completeLogin: () => Promise<string>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
let discoveryPromise: Promise<OidcDiscovery> | undefined;

function decodeJwt(token: string): Record<string, unknown> {
  try {
    const part = token.split('.')[1];
    if (!part) return {};
    const padded = part.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(part.length / 4) * 4, '=');
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return {};
  }
}

function normalizeRoles(payload: Record<string, unknown>) {
  const raw = [payload.role, payload.roles].flatMap((value) => {
    if (Array.isArray(value)) return value;
    if (typeof value === 'string') return value.split(/[\s,]+/);
    return [];
  });
  return [...new Set(raw.filter((value): value is string => typeof value === 'string')
    .map((role) => role.toUpperCase().replace(/^ROLE_/, ''))
    .filter(Boolean))];
}

function base64Url(bytes: Uint8Array) {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function randomValue(size = 32) {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function challenge(verifier: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64Url(new Uint8Array(digest));
}

function redirectUri() {
  return `${window.location.origin}/auth/callback`;
}

async function discovery() {
  if (!authority) throw new Error('VITE_OIDC_AUTHORITY no está configurado');
  discoveryPromise ??= fetch(`${authority}/.well-known/openid-configuration`)
    .then(async (response) => {
      if (!response.ok) throw new Error(`No se pudo cargar OIDC discovery (${response.status})`);
      return response.json() as Promise<OidcDiscovery>;
    });
  return discoveryPromise;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, updateToken] = useState(() => sessionStorage.getItem(STORAGE_KEY) ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const payload = useMemo(() => decodeJwt(token), [token]);

  const setToken = (next: string) => {
    const clean = next.trim();
    updateToken(clean);
    if (clean) sessionStorage.setItem(STORAGE_KEY, clean);
    else sessionStorage.removeItem(STORAGE_KEY);
  };

  const clear = () => {
    updateToken('');
    sessionStorage.removeItem(STORAGE_KEY);
  };

  const value = useMemo<AuthContextValue>(() => ({
    token,
    subject: typeof payload.sub === 'string' ? payload.sub : undefined,
    roles: normalizeRoles(payload),
    oidcConfigured: Boolean(authority && clientId),
    busy,
    error,
    setToken,
    clear,
    login: async (returnTo = '/account') => {
      if (!authority || !clientId) throw new Error('OIDC no está configurado');
      setBusy(true);
      setError(undefined);
      try {
        const config = await discovery();
        const state = randomValue();
        const verifier = randomValue(64);
        const codeChallenge = await challenge(verifier);
        sessionStorage.setItem(OIDC_STATE_KEY, state);
        sessionStorage.setItem(OIDC_VERIFIER_KEY, verifier);
        sessionStorage.setItem(OIDC_RETURN_TO_KEY, returnTo.startsWith('/') ? returnTo : '/account');

        const params = new URLSearchParams({
          response_type: 'code',
          client_id: clientId,
          redirect_uri: redirectUri(),
          scope,
          state,
          code_challenge: codeChallenge,
          code_challenge_method: 'S256',
        });
        if (audience) params.set('audience', audience);
        window.location.assign(`${config.authorization_endpoint}?${params}`);
      } catch (cause) {
        const message = cause instanceof Error ? cause.message : 'No se pudo iniciar sesión';
        setError(message);
        setBusy(false);
        throw cause;
      }
    },
    completeLogin: async () => {
      if (!authority || !clientId) throw new Error('OIDC no está configurado');
      setBusy(true);
      setError(undefined);
      try {
        const params = new URLSearchParams(window.location.search);
        const providerError = params.get('error_description') ?? params.get('error');
        if (providerError) throw new Error(providerError);
        const code = params.get('code');
        const returnedState = params.get('state');
        const expectedState = sessionStorage.getItem(OIDC_STATE_KEY);
        const verifier = sessionStorage.getItem(OIDC_VERIFIER_KEY);
        if (!code || !returnedState || !expectedState || returnedState !== expectedState || !verifier) {
          throw new Error('Respuesta OIDC inválida o expirada');
        }

        const config = await discovery();
        const response = await fetch(config.token_endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: clientId,
            code,
            redirect_uri: redirectUri(),
            code_verifier: verifier,
          }),
        });
        const body = await response.json() as { access_token?: string; error_description?: string; error?: string };
        if (!response.ok || !body.access_token) {
          throw new Error(body.error_description ?? body.error ?? 'El proveedor OIDC no devolvió access_token');
        }
        setToken(body.access_token);
        const returnTo = sessionStorage.getItem(OIDC_RETURN_TO_KEY) ?? '/account';
        sessionStorage.removeItem(OIDC_STATE_KEY);
        sessionStorage.removeItem(OIDC_VERIFIER_KEY);
        sessionStorage.removeItem(OIDC_RETURN_TO_KEY);
        setBusy(false);
        return returnTo;
      } catch (cause) {
        const message = cause instanceof Error ? cause.message : 'No se pudo completar el login';
        setError(message);
        setBusy(false);
        throw cause;
      }
    },
    logout: async () => {
      clear();
      sessionStorage.removeItem(OIDC_STATE_KEY);
      sessionStorage.removeItem(OIDC_VERIFIER_KEY);
      sessionStorage.removeItem(OIDC_RETURN_TO_KEY);
      if (!authority || !clientId) return;
      try {
        const config = await discovery();
        if (config.end_session_endpoint) {
          const params = new URLSearchParams({
            client_id: clientId,
            post_logout_redirect_uri: `${window.location.origin}/account`,
          });
          window.location.assign(`${config.end_session_endpoint}?${params}`);
        }
      } catch {
        // Local logout already completed; provider logout is best effort.
      }
    },
  }), [busy, error, payload, token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
