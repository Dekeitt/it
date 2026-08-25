import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { ErrorMessage, PageHeader } from '../../shared/ui';
import type { AvailabilitySlot } from '../../types';

const DAYS: AvailabilitySlot['dayOfWeek'][] = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];
const DAY_LABEL: Record<AvailabilitySlot['dayOfWeek'], string> = {MONDAY:'Lunes',TUESDAY:'Martes',WEDNESDAY:'Miércoles',THURSDAY:'Jueves',FRIDAY:'Viernes',SATURDAY:'Sábado',SUNDAY:'Domingo'};
type EditableSlot = Omit<AvailabilitySlot,'id'|'cleanerEmail'>;

function AvailabilityEditor({email}:{email:string}) {
  const api = useApi();
  const queryClient = useQueryClient();
  const query = useQuery({queryKey:['availability',email],queryFn:()=>api.cleaners.availability(email)});
  const [slots,setSlots] = useState<EditableSlot[]>([]);
  useEffect(()=>{if(query.data)setSlots(query.data.map(({dayOfWeek,startTime,endTime,zoneId})=>({dayOfWeek,startTime,endTime,zoneId})));},[query.data]);
  const save = useMutation({mutationFn:()=>api.cleaners.replaceMyAvailability(slots),onSuccess:(data)=>{setSlots(data.map(({dayOfWeek,startTime,endTime,zoneId})=>({dayOfWeek,startTime,endTime,zoneId})));void queryClient.invalidateQueries({queryKey:['availability',email]});void queryClient.invalidateQueries({queryKey:['cleaners','available']});}});
  function addSlot(){setSlots(c=>[...c,{dayOfWeek:'MONDAY',startTime:'09:00',endTime:'14:00',zoneId:'Europe/Madrid'}]);}
  return <div className="panel form-panel"><span className="eyebrow">Cleaner</span><h2>Disponibilidad semanal</h2><p className="muted">Los clientes solo verán tu perfil como disponible cuando la reserva quepa en uno de estos intervalos y no exista otro servicio solapado.</p><div className="form-stack">{slots.map((slot,index)=><div className="detail-grid" key={`${slot.dayOfWeek}-${index}`}><label>Día<select className="input" value={slot.dayOfWeek} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,dayOfWeek:e.target.value as AvailabilitySlot['dayOfWeek']}:item))}>{DAYS.map(day=><option key={day} value={day}>{DAY_LABEL[day]}</option>)}</select></label><label>Desde<input className="input" type="time" value={slot.startTime} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,startTime:e.target.value}:item))}/></label><label>Hasta<input className="input" type="time" value={slot.endTime} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,endTime:e.target.value}:item))}/></label><button className="button secondary compact" type="button" onClick={()=>setSlots(c=>c.filter((_,i)=>i!==index))}>Eliminar</button></div>)}<div className="actions"><button className="button secondary" type="button" onClick={addSlot}>Añadir intervalo</button><button className="button primary" type="button" disabled={save.isPending} onClick={()=>save.mutate()}>Guardar disponibilidad</button></div>{query.error&&<ErrorMessage error={query.error}/>} {save.error&&<ErrorMessage error={save.error}/>}</div></div>;
}

export function AccountPage() {
  const auth = useAuth();
  const api = useApi();
  const [draft,setDraft] = useState(auth.token);
  const me = useQuery({queryKey:['me'],queryFn:api.me.get,enabled:!!auth.token,retry:false});
  const roles = me.data?.roles ?? auth.roles;
  const email = me.data?.email ?? auth.subject;

  return <section className="page narrow">
    <PageHeader eyebrow="Identidad" title="Cuenta">La API valida el token OIDC y persiste la cuenta por issuer + subject; el email deja de ser la clave de ownership.</PageHeader>
    <div className="panel form-panel">
      {auth.oidcConfigured ? <>
        <span className="eyebrow">OIDC + PKCE</span>
        <h2>{auth.token ? 'Sesión iniciada' : 'Inicia sesión'}</h2>
        <p className="muted">El navegador usa Authorization Code + PKCE. El backend valida issuer, audiencia y firma antes de asociar la sesión al ID interno.</p>
        <div className="actions">
          {!auth.token && <button className="button primary" disabled={auth.busy} onClick={()=>void auth.login('/account').catch(()=>undefined)}>Iniciar sesión</button>}
          {auth.token && <button className="button secondary" disabled={auth.busy} onClick={()=>void auth.logout()}>Cerrar sesión</button>}
        </div>
        {auth.error && <p className="error-message">{auth.error}</p>}
      </> : <>
        <span className="eyebrow">Desarrollo local</span>
        <h2>Access token manual</h2>
        <p className="muted">Configura <code>VITE_OIDC_AUTHORITY</code> y <code>VITE_OIDC_CLIENT_ID</code> para activar el login real. La entrada manual queda solo como fallback local.</p>
        <label>Access token<textarea className="input textarea token-input" value={draft} onChange={e=>setDraft(e.target.value)} placeholder="eyJ…" autoComplete="off" spellCheck={false}/></label>
        <div className="actions"><button className="button primary" onClick={()=>auth.setToken(draft)}>Guardar para esta pestaña</button><button className="button secondary" onClick={()=>{setDraft('');auth.clear();}}>Cerrar sesión local</button></div>
      </>}
      {me.error&&<ErrorMessage error={me.error}/>} 
      <div className="session-info">
        <span><small>ID interno</small>{me.data?.id ?? '—'}</span>
        <span><small>Issuer</small>{me.data?.issuer ?? '—'}</span>
        <span><small>Subject</small>{me.data?.subject ?? auth.subject ?? '—'}</span>
        <span><small>Email</small>{email ?? '—'}</span>
        <span><small>Roles</small>{roles.join(', ') || '—'}</span>
      </div>
    </div>
    {auth.token&&roles.includes('CLEANER')&&email&&<AvailabilityEditor email={email}/>} 
  </section>;
}
