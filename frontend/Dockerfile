FROM node:22-alpine AS build
WORKDIR /app

ARG VITE_API_BASE_URL=
ARG VITE_OIDC_AUTHORITY=
ARG VITE_OIDC_CLIENT_ID=
ARG VITE_OIDC_SCOPE="openid profile email"
ARG VITE_OIDC_AUDIENCE=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_OIDC_AUTHORITY=$VITE_OIDC_AUTHORITY \
    VITE_OIDC_CLIENT_ID=$VITE_OIDC_CLIENT_ID \
    VITE_OIDC_SCOPE=$VITE_OIDC_SCOPE \
    VITE_OIDC_AUDIENCE=$VITE_OIDC_AUDIENCE

COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM caddy:2-alpine
ENV PORT=8080 \
    BACKEND_URL=http://backend:8080
COPY Caddyfile /etc/caddy/Caddyfile
RUN caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
COPY --from=build /app/dist /srv
EXPOSE 8080
