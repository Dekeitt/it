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

Copia `.env.example`, configura secretos reales y activa `SPRING_PROFILES_ACTIVE=prod`. Nunca publiques el archivo `.env`.

- Aplicación: `http://localhost:8080/`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Decisiones de seguridad incluidas

- El importe de Stripe se deriva del job almacenado, no del navegador.
- Los secretos de pago y JSON crudo no se serializan en la API.
- Las reservas se comprueban bajo bloqueo de base de datos y con detección completa de solapes.
- Producción usa variables obligatorias y `ddl-auto=validate`.
