import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useApi } from '../../api';
import { ErrorMessage } from '../../shared/ui';

export function ConnectPayoutsPanel(){
 const api=useApi();const queryClient=useQueryClient();const[country,setCountry]=useState('ES');
 const status=useQuery({queryKey:['connect','status'],queryFn:api.connect.status,retry:false});
 const onboarding=useMutation({mutationFn:()=>api.connect.onboarding(country),onSuccess:data=>{void queryClient.invalidateQueries({queryKey:['connect','status']});window.location.assign(data.onboardingUrl);}});
 const data=status.data;
 return <div className="panel account-section"><span className="eyebrow">Cobros</span><h2>Stripe Connect</h2>
  {!data?.enabled?<p className="muted">Los payouts del marketplace todavía no están activados por operaciones. Tu perfil y reservas siguen funcionando normalmente.</p>:<>
   <div className="detail-grid"><span><small>Onboarding</small>{data.onboardingStatus}</span><span><small>KYC enviado</small>{data.detailsSubmitted?'Sí':'Pendiente'}</span><span><small>Payouts</small>{data.payoutsEnabled?'Activos':'Pendientes'}</span></div>
   <p className="muted">Stripe recopila y valida los datos de identidad y cuenta bancaria. Clean IT solo conserva el estado necesario para saber si puedes recibir fondos.</p>
   {!data.readyForMarketplace&&<div className="form-stack"><label>País de la cuenta<input className="input" maxLength={2} value={country} onChange={e=>setCountry(e.target.value.toUpperCase())}/></label><button className="button primary" disabled={country.length!==2||onboarding.isPending} onClick={()=>onboarding.mutate()}>{data.accountCreated?'Reanudar configuración de cobros':'Configurar cobros'}</button></div>}
   {data.readyForMarketplace&&<div className="notice">Cuenta lista para recibir el neto de las reservas mediante Stripe Connect.</div>}
  </>}
  {status.error&&<ErrorMessage error={status.error}/>} {onboarding.error&&<ErrorMessage error={onboarding.error}/>} 
 </div>;
}
