import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

const STORAGE_KEY = 'clean-it-access-token';

type AuthContextValue = {
  token: string;
  subject?: string;
  roles: string[];
  setToken: (token: string) => void;
  clear: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, updateToken] = useState(() => sessionStorage.getItem(STORAGE_KEY) ?? '');
  const payload = useMemo(() => decodeJwt(token), [token]);
  const value = useMemo<AuthContextValue>(() => ({
    token,
    subject: typeof payload.sub === 'string' ? payload.sub : undefined,
    roles: normalizeRoles(payload),
    setToken: (next) => {
      const clean = next.trim();
      updateToken(clean);
      if (clean) sessionStorage.setItem(STORAGE_KEY, clean);
      else sessionStorage.removeItem(STORAGE_KEY);
    },
    clear: () => {
      updateToken('');
      sessionStorage.removeItem(STORAGE_KEY);
    },
  }), [payload, token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
