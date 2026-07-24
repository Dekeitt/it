# it

Artefacto Maven: `com.clean:it:0.0.1-SNAPSHOT`

Aplicación Spring Boot para gestión de:

- jobs
- reservas
- cleaners
- reviews
- pagos y webhooks de Stripe

## Requisitos

- Java 17
- Maven
- PostgreSQL y Redis para entorno real

## Ejecución

```bash
./mvnw spring-boot:run
```

## Documentación de la API

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Root info endpoint: `http://localhost:8080/`

## Variables relevantes

- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `SPRING_REDIS_PASSWORD`
- `JWT_SECRET`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`

## Endpoints destacados

- `POST /api/jobs`
- `GET /api/jobs/open`
- `POST /api/reviews`
- `GET /api/reviews/{email}`
