import type { ReactNode } from 'react';
import { ApiError } from '../api';

export function PageHeader({ eyebrow, title, children }: { eyebrow?: string; title: string; children?: ReactNode }) {
  return <header className="page-header">{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h1>{title}</h1>{children && <p>{children}</p>}</header>;
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}

export function ErrorMessage({ error }: { error: unknown }) {
  const requestId = error instanceof ApiError ? error.requestId : undefined;
  return <div className="notice error" role="alert">{error instanceof Error ? error.message : 'Ha ocurrido un error.'}{requestId && <small> · Ref. {requestId}</small>}</div>;
}

export function AuthGate() {
  return <div className="panel auth-gate"><span className="eyebrow">Área privada</span><h2>Necesitas una sesión</h2><p>Añade un JWT válido desde la sección Cuenta para acceder a esta funcionalidad.</p></div>;
}

export function StatusTag({ status }: { status?: string }) {
  const value = (status ?? 'UNKNOWN').toUpperCase();
  const positive = ['PAID', 'SUCCEEDED', 'CONFIRMED', 'COMPLETED', 'OPEN'].includes(value);
  const warning = ['PENDING', 'PROCESSING', 'REQUIRES_PAYMENT_METHOD'].includes(value);
  return <span className={`tag ${positive ? 'good' : warning ? 'warn' : ''}`}>{value}</span>;
}
