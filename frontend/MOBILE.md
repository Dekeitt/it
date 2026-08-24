# Clean IT mobile

La app móvil comparte el mismo frontend React/Vite que la web.

## Estado actual

Esta fase deja dos caminos preparados:

1. **PWA**: instalable directamente desde el navegador en Android/iOS.
2. **Capacitor**: base para generar contenedores Android/iOS sin reescribir la interfaz.

Los proyectos nativos `android/` e `ios/` todavía no se generan en esta fase. Se añadirán cuando el cliente API tenga resuelta la URL del backend para el bundle nativo y se incorporen capacidades nativas que aporten valor (push, biometría, deep links, cámara, etc.).

## PWA

Incluye manifest, iconos 192/512 y maskable, Apple Touch Icon, `display: standalone` y service worker.

El service worker no cachea `/api`, Actuator, Swagger ni OpenAPI. La PWA no debe considerar datos de negocio o pagos como contenido offline.

Android/Chrome: menú -> **Instalar aplicación**.

iPhone/Safari: Compartir -> **Añadir a pantalla de inicio**.

## Capacitor

```bash
npm install
npm run cap:sync
```

Crear los proyectos nativos en la siguiente fase:

```bash
npm run cap:add:android
npm run cap:add:ios
```

Abrir IDEs:

```bash
npm run cap:open:android
npm run cap:open:ios
```

### Wrapper remoto para pruebas

Para una prueba rápida del enfoque de launcher web:

```bash
CAPACITOR_SERVER_URL=https://TU-FRONTEND npx cap sync
```

No hay URL remota fijada por defecto. Para publicación en tiendas se prefiere empaquetar el bundle web y añadir capacidades nativas reales, especialmente en iOS.

## Siguiente fase

- resolver la URL de API para el bundle nativo;
- generar `android/` y `ios/`;
- deep links;
- push notifications;
- biometría;
- safe areas/notch;
- splash e iconos nativos;
- pruebas en dispositivos;
- política de privacidad y metadata de tienda.
