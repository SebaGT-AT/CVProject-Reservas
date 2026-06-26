# Etapa 9: preparación para lanzamiento

## Resultado

- Consola administrativa protegida por rol con métricas de plataforma.
- Búsqueda paginada y activación/desactivación de usuarios con auditoría.
- Bandeja de fallos de correo y Google Calendar sin exponer payloads.
- Smoke tests de autenticación pública en escritorio y móvil con Playwright.
- Imágenes versionadas en GHCR al publicar tags `v*` y composición de staging.

## Primer administrador

No existe una contraseña administrativa predeterminada. Registrar y verificar una cuenta normal, y promoverla directamente en PostgreSQL durante una ventana controlada:

```sql
BEGIN;
SELECT id, email, role, active, email_verified_at
FROM users WHERE LOWER(email) = LOWER('admin@example.com') FOR UPDATE;

UPDATE users SET role = 'ADMIN'
WHERE LOWER(email) = LOWER('admin@example.com')
  AND active = TRUE AND email_verified_at IS NOT NULL;
COMMIT;
```

Registrar esta acción en el sistema de cambios de infraestructura. Las modificaciones posteriores de estado sí quedan en `admin_audit_events`.

## Release y staging

1. Confirmar CI verde en `main`.
2. Crear un tag semántico, por ejemplo `v0.1.0`, y publicarlo.
3. Esperar que `Release images` publique API y frontend en GHCR.
4. Si los paquetes son privados, autenticar el host con un token limitado a `read:packages`: `docker login ghcr.io`.
5. Copiar `.env.staging.example` fuera del repositorio y reemplazar todos los secretos. `IMAGE_NAMESPACE` debe coincidir con el owner de GHCR en minúscula.
6. Ejecutar:

```bash
docker compose --env-file .env.staging \
  -f compose.prod.yaml -f compose.staging.yaml pull
docker compose --env-file .env.staging \
  -f compose.prod.yaml -f compose.staging.yaml up -d
```

7. Validar readiness, migraciones Flyway, registro/login, reserva/cancelación, correo y la consola administrativa.

## Criterios antes de producción

- Restauración de respaldo probada y RPO/RTO aceptados.
- Dominio, TLS, correo transaccional y políticas legales aprobados.
- Cuenta administrativa individual con MFA en Google/GitHub/infraestructura.
- Monitoreo y alertas con responsable de guardia.
- Prueba completa cliente → reserva → profesional → cancelación en staging.
- Revisión de accesibilidad, privacidad y autorización entre tenants.

Los smoke tests Playwright de esta etapa comprueban renderizado responsive, etiquetas y validación base; no sustituyen el flujo E2E autenticado completo, que requiere datos controlados y correo de pruebas en staging.
