# Deploy del frontend en Railway

El frontend está preparado para desplegarse como un servicio independiente dentro del mismo proyecto y environment de Railway que el backend.

## Servicio frontend

Crea un nuevo servicio desde el mismo repositorio GitHub y configura:

- Root Directory: `/frontend`
- Builder: Dockerfile (Railway detectará `frontend/Dockerfile` al usar ese root)
- Healthcheck Path: `/healthz`
- Public Networking: genera un dominio Railway o conecta tu dominio personalizado
- Watch Paths: `/frontend/**` y, si quieres redeploy al cambiar infraestructura compartida, `/docker-compose.yml`

Railway inyecta `PORT`; Caddy escucha automáticamente en ese puerto.

## Comunicación privada con el backend

En Variables del servicio frontend crea:

```text
BACKEND_URL=http://${{backend.RAILWAY_PRIVATE_DOMAIN}}:${{backend.PORT}}
```

`backend` debe coincidir con el nombre de tu servicio Spring Boot en Railway. Si se llama de otra forma, selecciona ese servicio al crear la reference variable.

No configures `VITE_API_BASE_URL` en producción: el navegador debe llamar a `/api` sobre el mismo dominio del frontend. Caddy reenvía esas peticiones al backend por la red privada de Railway.

## Resultado

```text
Browser
  |
  | https://tu-frontend.up.railway.app
  v
Frontend (Caddy + React)
  |
  | /api -> BACKEND_URL por railway.internal
  v
Backend Spring Boot
```

Esto evita CORS para la aplicación web y mantiene el tráfico frontend -> backend dentro de la red privada de Railway.

## Stripe

Puedes mantener el dominio público actual del backend para el webhook de Stripe. Si más adelante quieres exponer solo el frontend, puedes registrar el webhook contra:

```text
https://TU_DOMINIO/api/payments/webhook
```

Caddy conservará el path y lo reenviará al backend.

## Comprobación

Después del deploy:

```text
GET /healthz       -> 200 ok
GET /              -> SPA React
GET /api/info      -> respuesta del backend
```
