import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../api';
import { ErrorMessage } from '../../shared/ui';

function applicationServerKey(value:string){const padding='='.repeat((4-value.length%4)%4);const raw=atob((value+padding).replace(/-/g,'+').replace(/_/g,'/'));return Uint8Array.from(raw,c=>c.charCodeAt(0));}

export function NotificationSettings(){
 const api=useApi();const client=useQueryClient();const preferences=useQuery({queryKey:['notifications','preferences'],queryFn:api.notifications.preferences});const config=useQuery({queryKey:['notifications','push-config'],queryFn:api.notifications.pushConfig});
 const save=useMutation({mutationFn:api.notifications.savePreferences,onSuccess:data=>client.setQueryData(['notifications','preferences'],data)});
 const enablePush=useMutation({mutationFn:async()=>{if(!config.data?.enabled||!config.data.publicKey)throw new Error('Web Push no está configurado en el servidor');if(!('serviceWorker'in navigator)||!('PushManager'in window)||!('Notification'in window))throw new Error('Este navegador no soporta Web Push');const permission=await Notification.requestPermission();if(permission!=='granted')throw new Error('No se ha concedido permiso para notificaciones');const registration=await navigator.serviceWorker.ready;let subscription=await registration.pushManager.getSubscription();subscription??=await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:applicationServerKey(config.data.publicKey)});await api.notifications.subscribe(subscription.toJSON());return api.notifications.savePreferences({emailEnabled:preferences.data?.emailEnabled??true,pushEnabled:true});},onSuccess:data=>client.setQueryData(['notifications','preferences'],data)});
 const disablePush=useMutation({mutationFn:async()=>{const registration=await navigator.serviceWorker.ready;const subscription=await registration.pushManager.getSubscription();if(subscription){await api.notifications.unsubscribe(subscription.endpoint);await subscription.unsubscribe();}return api.notifications.savePreferences({emailEnabled:preferences.data?.emailEnabled??true,pushEnabled:false});},onSuccess:data=>client.setQueryData(['notifications','preferences'],data)});
 if(preferences.isLoading)return <p className="muted">Cargando preferencias…</p>;
 return <div className="form-stack notification-settings">
   <label className="toggle-row"><span><strong>Email transaccional</strong><small>Reservas, pagos, reseñas y recordatorios.</small></span><input type="checkbox" checked={preferences.data?.emailEnabled??true} onChange={e=>save.mutate({emailEnabled:e.target.checked,pushEnabled:preferences.data?.pushEnabled??true})}/></label>
   <label className="toggle-row"><span><strong>Notificaciones push</strong><small>Mensajes breves sin información sensible en el dispositivo.</small></span><input type="checkbox" checked={preferences.data?.pushEnabled??false} disabled={!config.data?.enabled||enablePush.isPending||disablePush.isPending} onChange={e=>e.target.checked?enablePush.mutate():disablePush.mutate()}/></label>
   {!config.isLoading&&!config.data?.enabled&&<p className="security-note">Web Push está preparado pero desactivado hasta configurar las claves VAPID en producción.</p>}
   {preferences.error&&<ErrorMessage error={preferences.error}/>} {config.error&&<ErrorMessage error={config.error}/>} {save.error&&<ErrorMessage error={save.error}/>} {enablePush.error&&<ErrorMessage error={enablePush.error}/>} {disablePush.error&&<ErrorMessage error={disablePush.error}/>} 
 </div>;
}
