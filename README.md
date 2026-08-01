# Clean IT

Marketplace de servicios de limpieza construido con Spring Boot, PostgreSQL, Redis y una SPA React ligera.

## Arranque local

```bash
cd it-main
./mvnw spring-boot:run
```

La configuración local usa H2. Para levantar PostgreSQL y Redis:

```bash
docker compose up -d
```

## Comprobaciones

```bash
./mvnw clean verify -Dspring.profiles.active=test
docker build -t clean-it .
```

## Producción

Copia `.env.example`, configura secretos reales y activa `SPRING_PROFILES_ACTIVE=prod`. Nunca publiques el archivo `.env`. Para JWT configura `JWT_JWK_SET_URI` (recomendado) o un `JWT_SECRET` de al menos 32 bytes, además de `JWT_ISSUER` y `JWT_AUDIENCE`.

- Aplicación: `http://localhost:8080/`
- Checkout seguro: `http://localhost:8080/checkout.html?reservationId=<id>`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Decisiones de seguridad incluidas

- El precio y la moneda se congelan al crear la reserva; Stripe nunca confía en el importe del navegador.
- Cada reserva reutiliza un único PaymentIntent con clave de idempotencia.
- Los webhooks se verifican con el SDK oficial y se reclaman de forma atómica y reintentable.
- Los secretos de pago y JSON crudo no se serializan en la API.
- PostgreSQL impide solapes de reservas incluso con varias instancias de la aplicación.
- JWT valida emisor y audiencia y puede usar claves asimétricas mediante JWK.
- Producción usa variables obligatorias y `ddl-auto=validate`.
