# Runbook de operaciones

## Despliegue

1. Configurar secretos fuera del repositorio y ejecutar `docker compose -f compose.prod.yaml config`.
2. Construir y desplegar con `docker compose -f compose.prod.yaml up -d --build`.
3. Confirmar `/actuator/health/readiness`, registro de Flyway y envío SMTP.
4. Ejecutar una reserva sintética y comprobar confirmación, outbox y correo.

## Observación

- Correlacionar soporte y logs mediante `X-Request-ID`.
- Recolectar `/actuator/prometheus` desde una red privada y con credenciales técnicas.
- Alertar por readiness fallida, errores HTTP 5xx, latencia, conexiones de base de datos y eventos de outbox en estado `DEAD`.
- Nunca registrar JWT, cookies, contraseñas ni contenido sensible del cliente.

## Respaldo y restauración

Respaldo lógico diario de referencia:

```bash
pg_dump --format=custom --no-owner --file=reservas.dump "$DATABASE_URL"
```

Restaurar primero en una base vacía de staging:

```bash
pg_restore --clean --if-exists --no-owner --dbname="$RESTORE_DATABASE_URL" reservas.dump
```

Validar migraciones, conteos de usuarios/citas, una consulta de disponibilidad y una reserva de prueba. Registrar duración y resultado. Para un producto con reservas activas se recomienda además respaldo administrado con point-in-time recovery, cifrado y retención definida.

## Incidente y rollback

1. Declarar impacto, hora y responsable; conservar un `X-Request-ID` de ejemplo.
2. Detener despliegues y aislar el componente afectado sin borrar datos.
3. Si el binario es la causa, volver a la imagen anterior. No revertir migraciones destructivamente.
4. Confirmar integridad de citas y reanudar el worker de notificaciones.
5. Documentar causa, recuperación, clientes afectados y acciones preventivas.

## Google Calendar

- Alertar por crecimiento de `calendar_sync_outbox` en `FAILED` o `DEAD`.
- Un error de autorización cambia la conexión a `REAUTH_REQUIRED`; el profesional debe volver a autorizarla.
- No editar ni borrar manualmente filas del outbox. Corregir la causa y reencolar mediante una operación administrativa auditada.
- Antes de rotar `GOOGLE_TOKEN_ENCRYPTION_KEY`, re-cifrar todos los refresh tokens. Perder la clave obliga a cada profesional a conectar su cuenta nuevamente.
- Al desactivar la integración global, los eventos existentes permanecen en Google y dejan de recibir cambios.
