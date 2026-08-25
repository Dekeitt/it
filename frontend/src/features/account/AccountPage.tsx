import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { ErrorMessage, PageHeader } from '../../shared/ui';
import type { AvailabilitySlot } from '../../types';

const DAYS: AvailabilitySlot['dayOfWeek'][] = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];
const DAY_LABEL: Record<AvailabilitySlot['dayOfWeek'], string> = {MONDAY:'Lunes',TUESDAY:'Martes',WEDNESDAY:'Miércoles',THURSDAY:'Jueves',FRIDAY:'Viernes',SATURDAY:'Sábado',SUNDAY:'Domingo'};
type EditableSlot = Omit<AvailabilitySlot,'id'|'cleanerEmail'>;
type EditableService={serviceCode:string;hourlyRateEuros:string};
type EditableArea={countryCode:string;postalCodePrefix:string};

function CleanerProfileEditor({email,currentName,onSaved}:{email:string;currentName?:string;onSaved:()=>void}){
  const api=useApi();const[name,setName]=useState(currentName??'');
  useEffect(()=>{if(currentName)setName(currentName);},[currentName]);
  const save=useMutation({mutationFn:()=>api.cleaners.upsertProfile(name),onSuccess:onSaved});
  return <div className="panel account-section"><span className="eyebrow">Perfil profesional</span><h2>Nombre público</h2><p className="muted">El email se sincroniza desde tu identidad autenticada; no puede establecerlo el frontend.</p><div className="form-stack"><label>Nombre<input className="input" value={name} onChange={e=>setName(e.target.value)} placeholder="Nombre que verán los clientes"/></label><button className="button primary" disabled={!name.trim()||save.isPending} onClick={()=>save.mutate()}>Guardar perfil</button>{save.error&&<ErrorMessage error={save.error}/>}<small className="muted">Cuenta: {email}</small></div></div>;
}

function AvailabilityEditor({email}:{email:string}) {
  const api = useApi();
  const queryClient = useQueryClient();
  const query = useQuery({queryKey:['availability',email],queryFn:()=>api.cleaners.availability(email)});
  const [slots,setSlots] = useState<EditableSlot[]>([]);
  useEffect(()=>{if(query.data)setSlots(query.data.map(({dayOfWeek,startTime,endTime,zoneId})=>({dayOfWeek,startTime,endTime,zoneId})));},[query.data]);
  const save = useMutation({mutationFn:()=>api.cleaners.replaceMyAvailability(slots),onSuccess:(data)=>{setSlots(data.map(({dayOfWeek,startTime,endTime,zoneId})=>({dayOfWeek,startTime,endTime,zoneId})));void queryClient.invalidateQueries({queryKey:['availability',email]});void queryClient.invalidateQueries({queryKey:['cleaners','available']});void queryClient.invalidateQueries({queryKey:['booking','available']});}});
  function addSlot(){setSlots(c=>[...c,{dayOfWeek:'MONDAY',startTime:'09:00',endTime:'14:00',zoneId:'Europe/Madrid'}]);}
  return <div className="panel account-section"><span className="eyebrow">Cleaner</span><h2>Disponibilidad semanal</h2><p className="muted">Los clientes solo te verán como disponible cuando la reserva quepa en uno de estos intervalos y no exista otro servicio solapado.</p><div className="form-stack">{slots.map((slot,index)=><div className="detail-grid" key={`${slot.dayOfWeek}-${index}`}><label>Día<select className="input" value={slot.dayOfWeek} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,dayOfWeek:e.target.value as AvailabilitySlot['dayOfWeek']}:item))}>{DAYS.map(day=><option key={day} value={day}>{DAY_LABEL[day]}</option>)}</select></label><label>Desde<input className="input" type="time" value={slot.startTime} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,startTime:e.target.value}:item))}/></label><label>Hasta<input className="input" type="time" value={slot.endTime} onChange={e=>setSlots(c=>c.map((item,i)=>i===index?{...item,endTime:e.target.value}:item))}/></label><button className="button secondary compact" type="button" onClick={()=>setSlots(c=>c.filter((_,i)=>i!==index))}>Eliminar</button></div>)}<div className="actions"><button className="button secondary" type="button" onClick={addSlot}>Añadir intervalo</button><button className="button primary" type="button" disabled={save.isPending} onClick={()=>save.mutate()}>Guardar disponibilidad</button></div>{query.error&&<ErrorMessage error={query.error}/>} {save.error&&<ErrorMessage error={save.error}/>}</div></div>;
}

function CommercialEditor({email}:{email:string}){
  const api=useApi();const queryClient=useQueryClient();
  const catalog=useQuery({queryKey:['booking','catalog'],queryFn:api.booking.catalog});
  const services=useQuery({queryKey:['cleaner-services',email],queryFn:()=>api.cleaners.services(email)});
  const areas=useQuery({queryKey:['service-areas',email],queryFn:()=>api.cleaners.serviceAreas(email)});
  const[serviceDrafts,setServiceDrafts]=useState<EditableService[]>([]);const[areaDrafts,setAreaDrafts]=useState<EditableArea[]>([]);
  useEffect(()=>{if(services.data)setServiceDrafts(services.data.map(item=>({serviceCode:item.serviceCode,hourlyRateEuros:(item.hourlyRateCents/100).toFixed(2)})));},[services.data]);
  useEffect(()=>{if(areas.data)setAreaDrafts(areas.data.map(item=>({countryCode:item.countryCode,postalCodePrefix:item.postalCodePrefix})));},[areas.data]);
  const saveServices=useMutation({mutationFn:()=>api.cleaners.replaceMyServices(serviceDrafts.map(item=>({serviceCode:item.serviceCode,hourlyRateCents:Math.round(Number(item.hourlyRateEuros)*100)}))),onSuccess:()=>{void queryClient.invalidateQueries({queryKey:['cleaner-services',email]});void queryClient.invalidateQueries({queryKey:['booking','available']});}});
  const saveAreas=useMutation({mutationFn:()=>api.cleaners.replaceMyServiceAreas(areaDrafts.map(item=>({countryCode:item.countryCode.toUpperCase(),postalCodePrefix:item.postalCodePrefix}))),onSuccess:()=>{void queryClient.invalidateQueries({queryKey:['service-areas',email]});void queryClient.invalidateQueries({queryKey:['booking','available']});}});
  function addService(){const next=catalog.data?.find(service=>!serviceDrafts.some(item=>item.serviceCode===service.code));if(next)setServiceDrafts(c=>[...c,{serviceCode:next.code,hourlyRateEuros:'20.00'}]);}
  return <>
    <div className="panel account-section"><span className="eyebrow">Oferta</span><h2>Servicios y tarifas</h2><p className="muted">El precio final se calcula en servidor con esta tarifa y la duración elegida.</p><div className="form-stack">{serviceDrafts.map((item,index)=><div className="detail-grid two-grid" key={`${item.serviceCode}-${index}`}><label>Servicio<select className="input" value={item.serviceCode} onChange={e=>setServiceDrafts(c=>c.map((row,i)=>i===index?{...row,serviceCode:e.target.value}:row))}>{catalog.data?.map(service=><option key={service.code} value={service.code}>{service.name}</option>)}</select></label><label>€/hora<input className="input" type="number" min="1" step="0.50" value={item.hourlyRateEuros} onChange={e=>setServiceDrafts(c=>c.map((row,i)=>i===index?{...row,hourlyRateEuros:e.target.value}:row))}/></label><button className="button secondary compact" onClick={()=>setServiceDrafts(c=>c.filter((_,i)=>i!==index))}>Eliminar</button></div>)}<div className="actions"><button className="button secondary" disabled={!catalog.data?.some(service=>!serviceDrafts.some(item=>item.serviceCode===service.code))} onClick={addService}>Añadir servicio</button><button className="button primary" disabled={saveServices.isPending||serviceDrafts.some(item=>Number(item.hourlyRateEuros)<=0)} onClick={()=>saveServices.mutate()}>Guardar tarifas</button></div>{catalog.error&&<ErrorMessage error={catalog.error}/>} {services.error&&<ErrorMessage error={services.error}/>} {saveServices.error&&<ErrorMessage error={saveServices.error}/>}</div></div>
    <div className="panel account-section"><span className="eyebrow">Cobertura</span><h2>Zonas de trabajo</h2><p className="muted">MVP determinista por país y prefijo postal. Por ejemplo, ES + 28 cubre códigos postales que empiecen por 28.</p><div className="form-stack">{areaDrafts.map((item,index)=><div className="detail-grid two-grid" key={`${item.countryCode}-${item.postalCodePrefix}-${index}`}><label>País<input className="input" maxLength={2} value={item.countryCode} onChange={e=>setAreaDrafts(c=>c.map((row,i)=>i===index?{...row,countryCode:e.target.value.toUpperCase()}:row))}/></label><label>Prefijo postal<input className="input" maxLength={16} value={item.postalCodePrefix} onChange={e=>setAreaDrafts(c=>c.map((row,i)=>i===index?{...row,postalCodePrefix:e.target.value}:row))}/></label><button className="button secondary compact" onClick={()=>setAreaDrafts(c=>c.filter((_,i)=>i!==index))}>Eliminar</button></div>)}<div className="actions"><button className="button secondary" onClick={()=>setAreaDrafts(c=>[...c,{countryCode:'ES',postalCodePrefix:''}])}>Añadir zona</button><button className="button primary" disabled={saveAreas.isPending||areaDrafts.some(item=>item.countryCode.length!==2||!item.postalCodePrefix.trim())} onClick={()=>saveAreas.mutate()}>Guardar cobertura</button></div>{areas.error&&<ErrorMessage error={areas.error}/>} {saveAreas.error&&<ErrorMessage error={saveAreas.error}/>}</div></div>
  </>;
}

export function AccountPage() {
  const auth = useAuth();const api = useApi();const queryClient=useQueryClient();const [draft,setDraft] = useState(auth.token);
  const me = useQuery({queryKey:['me'],queryFn:api.me.get,enabled:!!auth.token,retry:false});
  const roles = me.data?.roles ?? auth.roles;const email = me.data?.email ?? auth.subject;
  const cleaners=useQuery({queryKey:['cleaners'],queryFn:api.cleaners.list,enabled:!!auth.token&&roles.includes('CLEANER')});
  const cleanerProfile=email?cleaners.data?.find(cleaner=>cleaner.email.toLowerCase()===email.toLowerCase()):undefined;

  return <section className="page narrow account-page">
    <PageHeader eyebrow="Identidad" title="Cuenta">La API valida el token OIDC y persiste la cuenta por issuer + subject; el email deja de ser la clave de ownership.</PageHeader>
    <div className="panel account-section">
      {auth.oidcConfigured ? <><span className="eyebrow">OIDC + PKCE</span><h2>{auth.token ? 'Sesión iniciada' : 'Inicia sesión'}</h2><p className="muted">El navegador usa Authorization Code + PKCE. El backend valida issuer, audiencia y firma antes de asociar la sesión al ID interno.</p><div className="actions">{!auth.token && <button className="button primary" disabled={auth.busy} onClick={()=>void auth.login('/account').catch(()=>undefined)}>Iniciar sesión</button>}{auth.token && <button className="button secondary" disabled={auth.busy} onClick={()=>void auth.logout()}>Cerrar sesión</button>}</div>{auth.error && <p className="error-message">{auth.error}</p>}</> : <><span className="eyebrow">Desarrollo local</span><h2>Access token manual</h2><p className="muted">Configura <code>VITE_OIDC_AUTHORITY</code> y <code>VITE_OIDC_CLIENT_ID</code> para activar el login real.</p><label>Access token<textarea className="input textarea token-input" value={draft} onChange={e=>setDraft(e.target.value)} placeholder="eyJ…" autoComplete="off" spellCheck={false}/></label><div className="actions"><button className="button primary" onClick={()=>auth.setToken(draft)}>Guardar para esta pestaña</button><button className="button secondary" onClick={()=>{setDraft('');auth.clear();}}>Cerrar sesión local</button></div></>}
      {me.error&&<ErrorMessage error={me.error}/>}<div className="session-info"><span><small>ID interno</small>{me.data?.id ?? '—'}</span><span><small>Issuer</small>{me.data?.issuer ?? '—'}</span><span><small>Subject</small>{me.data?.subject ?? auth.subject ?? '—'}</span><span><small>Email</small>{email ?? '—'}</span><span><small>Roles</small>{roles.join(', ') || '—'}</span></div>
    </div>
    {auth.token&&roles.includes('CLEANER')&&email&&<><CleanerProfileEditor email={email} currentName={cleanerProfile?.name} onSaved={()=>void queryClient.invalidateQueries({queryKey:['cleaners']})}/>{cleanerProfile&&<><CommercialEditor email={email}/><AvailabilityEditor email={email}/></>}</>}
  </section>;
}
