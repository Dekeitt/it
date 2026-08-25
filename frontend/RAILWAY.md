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

## OIDC + PKCE

El login web usa Authorization Code + PKCE y discovery OIDC estándar. Configura en el servicio frontend las variables de build:

```text
VITE_OIDC_AUTHORITY=https://TU_ISSUER_OIDC
VITE_OIDC_CLIENT_ID=TU_CLIENT_ID_PUBLICO
VITE_OIDC_SCOPE=openid profile email
VITE_OIDC_AUDIENCE=clean-it-api
```

El Dockerfile declara estas variables como `ARG` para que Vite las incluya durante el build. No pongas ningún client secret en el frontend: una SPA con PKCE es un cliente público.

En el proveedor OIDC registra:

```text
Allowed callback / redirect URI: https://TU_DOMINIO/auth/callback
Allowed logout URI:             https://TU_DOMINIO/account
Allowed web origin:              https://TU_DOMINIO
```

El access token que emita el proveedor debe llevar la audiencia configurada por el backend y claims `role` o `roles` con `CLIENT` y/o `CLEANER` según corresponda.

En el backend configura el mismo issuer/audience y el JWK Set del proveedor:

```text
JWT_ISSUER=https://TU_ISSUER_OIDC
JWT_AUDIENCE=clean-it-api
JWT_JWK_SET_URI=https://TU_ISSUER_OIDC/RUTA_JWKS
JWT_SECRET=
```

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
