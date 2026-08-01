# Contenido del patch

Incluye seguridad de JWT y Stripe, autorización de pagos, detección completa de solapes, bloqueos pesimistas, perfiles por entorno, Flyway, Docker/CI y mejoras visuales y de accesibilidad.

No mueve `it-main/` a la raíz ni reorganiza todos los paquetes por funcionalidad: esas operaciones producen un diff masivo y conviene hacerlas después en un commit separado con `git mv`.

Después de aplicarlo, elimina artefactos ya versionados:

```bash
git rm -r --cached it-main/target
git add .
git commit -m "Harden backend, payments, CI and UI"
```
