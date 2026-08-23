# Clean IT mobile

La estrategia móvil reutiliza el frontend React existente en dos fases.

## Fase 1: PWA

El build de Vite genera automáticamente:

- Web App Manifest.
- Service worker con actualización automática.
- Cache de assets del build para una carga más resiliente.
- Experiencia `standalone` al instalar desde Android/iOS.
- Iconos PWA normales y maskable.

Los endpoints `/api`, `/actuator`, `/swagger-ui`, `/v3/api-docs` y `/healthz` están excluidos del fallback de navegación del service worker. Los datos del backend no se cachean como contenido offline.

Para comprobar la PWA:

```bash
npm install
npm run build
npm run preview
```

En producción la aplicación debe servirse por HTTPS. Railway ya proporciona HTTPS en el dominio público.

### Instalación

- Android/Chrome: usar `Instalar aplicación` / `Añadir a pantalla de inicio`.
- iPhone/iPad/Safari: Compartir → `Añadir a pantalla de inicio`.

## Fase 2: Capacitor

Capacitor está inicializado con:

```text
appId: com.cleanit.app
appName: Clean IT
webDir: dist
```

Antes de generar los proyectos de las tiendas hay que confirmar el `appId`, porque cambiarlo después de publicar es costoso.

Cuando estemos listos para Android:

```bash
npm install @capacitor/android@8.5.0
npx cap add android
npm run mobile:sync
npx cap open android
```

Para iOS (requiere macOS + Xcode):

```bash
npm install @capacitor/ios@8.5.0
npx cap add ios
npm run mobile:sync
npx cap open ios
```

## API en aplicaciones nativas

La PWA usa `/api` bajo el mismo origen y Caddy lo reenvía al backend de Railway. Una app Capacitor empaquetada ejecutará los assets desde un origen nativo local, por lo que antes de publicar Android/iOS debemos definir el transporte API nativo (URL pública + política CORS segura o HTTP nativo de Capacitor). No se debe apuntar una build de tienda a una URL remota mediante `server.url` como sustituto permanente de la app empaquetada.

## Próximas capacidades nativas

Después de generar las plataformas, las primeras integraciones recomendadas son:

1. Push notifications para reservas/pagos.
2. Deep links a reservas y checkout.
3. Biometría para proteger la reentrada a la sesión.
4. Cámara para perfil/incidencias.
5. Calendario para añadir reservas.
