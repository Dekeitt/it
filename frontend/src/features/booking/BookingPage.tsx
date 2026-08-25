import { useMemo, useState } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../api';
import { useAuth } from '../../auth';
import { money } from '../../shared/format';
import { Empty, ErrorMessage, PageHeader } from '../../shared/ui';
import type { AddressInput, AvailableCleaner } from '../../types';

const EMPTY_ADDRESS: AddressInput = { label:'Casa', line1:'', line2:'', postalCode:'', city:'', region:'', countryCode:'ES' };

function validIso(localValue:string) {
  if (!localValue) return undefined;
  const date = new Date(localValue);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

export function BookingPage() {
  const api = useApi();
  const auth = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serviceCode,setServiceCode] = useState('');
  const [addressId,setAddressId] = useState<number>();
  const [startLocal,setStartLocal] = useState('');
  const [durationMinutes,setDurationMinutes] = useState(120);
  const [cleaner,setCleaner] = useState<AvailableCleaner>();
  const [showAddressForm,setShowAddressForm] = useState(false);
  const [addressDraft,setAddressDraft] = useState<AddressInput>(EMPTY_ADDRESS);

  const isClient = auth.roles.includes('CLIENT');
  const catalog = useQuery({queryKey:['booking','catalog'],queryFn:api.booking.catalog,enabled:!!auth.token});
  const addresses = useQuery({queryKey:['booking','addresses'],queryFn:api.booking.addresses,enabled:!!auth.token&&isClient});
  const selectedService = catalog.data?.find(service=>service.code===serviceCode);
  const selectedAddress = addresses.data?.find(address=>address.id===addressId);
  const startAt = useMemo(()=>validIso(startLocal),[startLocal]);

  const available = useQuery({
    queryKey:['booking','available',serviceCode,addressId,startAt,durationMinutes],
    queryFn:()=>api.booking.available(serviceCode,addressId!,startAt!,durationMinutes),
    enabled:!!auth.token&&isClient&&!!serviceCode&&!!addressId&&!!startAt&&durationMinutes>0,
    retry:false,
  });

  const createAddress = useMutation({
    mutationFn:()=>api.booking.createAddress(addressDraft),
    onSuccess: async (created)=>{
      await queryClient.invalidateQueries({queryKey:['booking','addresses']});
      setAddressId(created.id);
      setAddressDraft(EMPTY_ADDRESS);
      setShowAddressForm(false);
    },
  });

  const book = useMutation({
    mutationFn:()=>api.booking.book({serviceCode,cleanerProfileId:cleaner!.cleanerProfileId,addressId:addressId!,startAt:startAt!,durationMinutes}),
    onSuccess:(reservation)=>navigate({to:'/checkout/$reservationId',params:{reservationId:String(reservation.id)}}),
  });

  function chooseService(code:string,minimum:number) {
    setServiceCode(code);
    setCleaner(undefined);
    setDurationMinutes(current=>Math.max(current,minimum));
  }

  if (!auth.token) return <section className="page"><div className="panel auth-gate"><PageHeader eyebrow="Reserva directa" title="Accede para reservar">Guarda tu dirección, consulta disponibilidad real y paga después de confirmar la reserva.</PageHeader><Link to="/account" className="button primary">Acceder</Link></div></section>;
  if (!isClient) return <section className="page"><div className="panel auth-gate"><PageHeader eyebrow="Reserva directa" title="Cuenta de cliente necesaria">Este flujo está disponible para cuentas con rol CLIENT.</PageHeader><Link to="/account" className="button secondary">Ver cuenta</Link></div></section>;

  return <section className="page booking-page">
    <PageHeader eyebrow="Reserva directa" title="Reserva sin IDs ni formularios técnicos">Elige el servicio y el profesional. Clean IT valida zona, horario y tarifa y congela el precio en servidor.</PageHeader>
    <div className="booking-layout">
      <div className="booking-steps">
        <section className="panel booking-step">
          <div className="step-heading"><span className="step-number">1</span><div><h2>Servicio</h2><p className="muted">Selecciona el tipo de limpieza.</p></div></div>
          {catalog.error&&<ErrorMessage error={catalog.error}/>} 
          <div className="choice-grid">{catalog.data?.map(service=><button type="button" key={service.code} className={`choice-card ${serviceCode===service.code?'selected':''}`} onClick={()=>chooseService(service.code,service.minimumDurationMinutes)}><strong>{service.name}</strong><span>{service.description}</span><small>Mínimo {service.minimumDurationMinutes} min</small></button>)}</div>
        </section>

        <section className="panel booking-step">
          <div className="step-heading"><span className="step-number">2</span><div><h2>Dirección</h2><p className="muted">Solo se muestran profesionales que cubren este código postal.</p></div></div>
          {addresses.error&&<ErrorMessage error={addresses.error}/>} 
          <div className="choice-grid">{addresses.data?.map(address=><button type="button" key={address.id} className={`choice-card ${addressId===address.id?'selected':''}`} onClick={()=>{setAddressId(address.id);setCleaner(undefined);}}><strong>{address.label}</strong><span>{address.line1}</span><small>{address.postalCode} · {address.city}</small></button>)}</div>
          {addresses.data?.length===0&&!showAddressForm&&<Empty>Aún no tienes ninguna dirección guardada.</Empty>}
          <div className="actions"><button type="button" className="button secondary" onClick={()=>setShowAddressForm(value=>!value)}>{showAddressForm?'Cancelar':'Añadir dirección'}</button></div>
          {showAddressForm&&<div className="address-form form-stack">
            <label>Nombre<input className="input" value={addressDraft.label} onChange={e=>setAddressDraft(d=>({...d,label:e.target.value}))}/></label>
            <label>Dirección<input className="input" value={addressDraft.line1} onChange={e=>setAddressDraft(d=>({...d,line1:e.target.value}))} placeholder="Calle y número"/></label>
            <label>Complemento<input className="input" value={addressDraft.line2??''} onChange={e=>setAddressDraft(d=>({...d,line2:e.target.value}))} placeholder="Piso, puerta…"/></label>
            <div className="detail-grid compact-grid"><label>Código postal<input className="input" value={addressDraft.postalCode} onChange={e=>setAddressDraft(d=>({...d,postalCode:e.target.value}))}/></label><label>Ciudad<input className="input" value={addressDraft.city} onChange={e=>setAddressDraft(d=>({...d,city:e.target.value}))}/></label><label>País<input className="input" maxLength={2} value={addressDraft.countryCode} onChange={e=>setAddressDraft(d=>({...d,countryCode:e.target.value.toUpperCase()}))}/></label></div>
            <button type="button" className="button primary" disabled={createAddress.isPending||!addressDraft.line1||!addressDraft.postalCode||!addressDraft.city} onClick={()=>createAddress.mutate()}>Guardar dirección</button>
            {createAddress.error&&<ErrorMessage error={createAddress.error}/>} 
          </div>}
        </section>

        <section className="panel booking-step">
          <div className="step-heading"><span className="step-number">3</span><div><h2>Fecha y duración</h2><p className="muted">La búsqueda respeta la disponibilidad semanal y excluye solapes.</p></div></div>
          <div className="detail-grid two-grid"><label>Inicio<input className="input" type="datetime-local" value={startLocal} onChange={e=>{setStartLocal(e.target.value);setCleaner(undefined);}}/></label><label>Duración<select className="input" value={durationMinutes} onChange={e=>{setDurationMinutes(Number(e.target.value));setCleaner(undefined);}}>{[60,90,120,150,180,240,300,360].filter(minutes=>minutes>=(selectedService?.minimumDurationMinutes??60)).map(minutes=><option value={minutes} key={minutes}>{minutes<60?`${minutes} min`:`${minutes/60} h`}</option>)}</select></label></div>
        </section>

        <section className="panel booking-step">
          <div className="step-heading"><span className="step-number">4</span><div><h2>Profesional</h2><p className="muted">Ordenados por precio y valoración entre quienes pueden atender la reserva.</p></div></div>
          {available.isFetching&&<div className="notice">Buscando disponibilidad…</div>}
          {available.error&&<ErrorMessage error={available.error}/>} 
          {!available.isFetching&&available.data?.length===0&&serviceCode&&addressId&&startAt&&<Empty>No hay profesionales disponibles para esta combinación. Prueba otra hora o duración.</Empty>}
          <div className="cleaner-choice-list">{available.data?.map(option=><button type="button" key={option.cleanerProfileId} className={`cleaner-choice ${cleaner?.cleanerProfileId===option.cleanerProfileId?'selected':''}`} onClick={()=>setCleaner(option)}><div className="avatar">{option.name?.[0]??'C'}</div><div className="cleaner-choice-main"><strong>{option.name}</strong><span className="rating">★ {(option.rating??0).toFixed(1)}</span><small>{money(option.hourlyRateCents,option.currency)}/h</small></div><strong className="cleaner-total">{money(option.totalCents,option.currency)}</strong></button>)}</div>
        </section>
      </div>

      <aside className="panel booking-summary">
        <span className="eyebrow">Resumen</span><h2>Tu reserva</h2>
        <div className="summary-lines"><span><small>Servicio</small><strong>{selectedService?.name??'Pendiente'}</strong></span><span><small>Dirección</small><strong>{selectedAddress?`${selectedAddress.label} · ${selectedAddress.postalCode}`:'Pendiente'}</strong></span><span><small>Horario</small><strong>{startAt?new Intl.DateTimeFormat('es-ES',{dateStyle:'medium',timeStyle:'short'}).format(new Date(startAt)):'Pendiente'}</strong></span><span><small>Duración</small><strong>{durationMinutes} min</strong></span><span><small>Profesional</small><strong>{cleaner?.name??'Pendiente'}</strong></span></div>
        <div className="booking-total"><small>Total congelado al reservar</small><strong>{cleaner?money(cleaner.totalCents,cleaner.currency):'—'}</strong></div>
        <button className="button primary booking-submit" disabled={!serviceCode||!addressId||!startAt||!cleaner||book.isPending} onClick={()=>book.mutate()}>{book.isPending?'Creando reserva…':'Confirmar y pagar'}</button>
        {book.error&&<ErrorMessage error={book.error}/>} 
        <p className="security-note">El total visible procede de la API. Al confirmar, el backend vuelve a validar servicio, zona, disponibilidad y tarifa antes de persistir la reserva.</p>
      </aside>
    </div>
  </section>;
}
