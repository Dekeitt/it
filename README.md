# Clean IT

Marketplace de servicios de limpieza con backend Spring Boot y frontend React/TypeScript desacoplados.

## Arquitectura

```text
Browser
  │
  ▼
frontend/ (React 19 + Vite + Caddy)
  │  /api
  ▼
it-main/ (Spring Boot API)
  ├── PostgreSQL
  ├── Redis
  └── Stripe
```

El frontend y el backend son aplicaciones independientes. En producción se recomienda mantener un único origen público y hacer reverse proxy de `/api` al backend; así el navegador no necesita CORS y la API permanece separada del bundle web.

## Arranque del stack completo

```bash
docker compose up --build
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080/api/info`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health backend: `http://localhost:8080/actuator/health`
- Health frontend: `http://localhost:3000/healthz`

Para pagos reales configura en un `.env` local `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY` y `STRIPE_WEBHOOK_SECRET`. Los valores por defecto del compose son marcadores de desarrollo y no deben usarse contra Stripe.

## Desarrollo backend

```bash
cd it-main
./mvnw spring-boot:run
```

```bash
./mvnw clean verify -Dspring.profiles.active=test
```

## Desarrollo frontend

```bash
cd frontend
npm install
npm run dev
```

Vite escucha en `http://localhost:5173` y proxifica `/api` a `http://localhost:8080`, por lo que el desarrollo local tampoco necesita CORS.

El frontend usa:

- React 19 + TypeScript + Vite.
- TanStack Router para navegación.
- TanStack Query para estado remoto y caché.
- React Hook Form + Zod para formularios.
- Stripe Payment Element para checkout.
- Caddy como servidor de producción y reverse proxy de `/api`.

Las funcionalidades se organizan por dominio (`jobs`, `reservations`, `payments`, `reviews`) y comparten un cliente API tipado. El token de desarrollo permanece en `sessionStorage`; para producción la siguiente evolución recomendada es OIDC Authorization Code + PKCE y refresh token en cookie HttpOnly.

## Deploy en Railway

El repositorio está preparado para desplegar frontend y backend como dos servicios del mismo proyecto Railway.

Para el servicio frontend:

- conecta el mismo repositorio GitHub;
- configura `Root Directory` como `/frontend`;
- usa `/healthz` como Healthcheck Path;
- genera un dominio público para el frontend;
- añade la variable de referencia:

```text
BACKEND_URL=http://${{backend.RAILWAY_PRIVATE_DOMAIN}}:${{backend.PORT}}
```

El nombre `backend` debe coincidir con el servicio Spring Boot de Railway. El frontend escucha en el `PORT` que Railway inyecta y reenvía `/api/*` al backend por `railway.internal`, manteniendo ese tráfico dentro de la red privada.

No definas `VITE_API_BASE_URL` en producción si usas este esquema: el navegador debe consumir `/api` sobre el mismo dominio del frontend.

La guía detallada está en [`frontend/RAILWAY.md`](frontend/RAILWAY.md).

## Seguridad y dominio

- El precio y la moneda se congelan al crear la reserva; Stripe no confía en el importe del navegador.
- Cada reserva reutiliza un único PaymentIntent con clave de idempotencia.
- Los webhooks se verifican con el SDK oficial y se procesan de forma idempotente.
- PostgreSQL impide solapes de reservas incluso con varias instancias del backend.
- JWT valida emisor y audiencia y puede usar JWK asimétrico.
- `/api/jobs/open` requiere identidad y rol, coherente con `JobController`; ya no se publica accidentalmente desde `SecurityConfig`.
- El backend ya no sirve React, Babel ni páginas HTML: expone únicamente API, documentación y health checks.
