import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { dateTime, money } from '../../shared/format';
import { AuthGate, Empty, ErrorMessage, PageHeader, StatusTag } from '../../shared/ui';

export function PaymentsPage() {
  const api = useApi();
  const auth = useAuth();
  const payments = useQuery({ queryKey: ['payments'], queryFn: api.payments.list, enabled: !!auth.token });
  if (!auth.token) return <section className="page"><PageHeader eyebrow="Finanzas" title="Pagos" /><AuthGate /></section>;

  return <section className="page"><PageHeader eyebrow="Finanzas" title="Tus pagos">Solo se muestran pagos vinculados a reservas accesibles para tu identidad.</PageHeader>
    {payments.error && <ErrorMessage error={payments.error} />}
    {!payments.isLoading && (payments.data?.length ?? 0) === 0 && <Empty>No hay pagos todavía.</Empty>}
    <div className="table-card"><div className="table-scroll"><table><thead><tr><th>ID</th><th>Reserva</th><th>Importe</th><th>Estado</th><th>Actualizado</th></tr></thead><tbody>{(payments.data ?? []).map((payment) => <tr key={payment.id}><td>#{payment.id}</td><td>#{payment.reservationId}</td><td>{money(payment.amountCents, payment.currency)}</td><td><StatusTag status={payment.status} /></td><td>{dateTime(payment.updatedAt ?? payment.createdAt)}</td></tr>)}</tbody></table></div></div>
  </section>;
}
