# Roadmap de producto

Cada etapa termina con migracion, pruebas, documentacion de API y una demo vertical.

## 0. Fundacion (completada)

- Monorepo, PostgreSQL local, Flyway y perfiles de configuracion.
- Registro/login JWT, roles base y pantalla inicial protegida.
- Convenciones de errores, validacion y salud de la API.

## 1. Identidad lista para produccion (completada)

- Verificacion de correo, recuperacion de contraseña y refresh tokens rotativos. ✓
- Rate limiting, auditoria, cierre de sesiones y pruebas de seguridad. ✓
- Registro diferenciado para cliente y profesional. ✓

## 2. Profesionales y servicios (completada)

- Perfil publico, especialidades, zona horaria, servicios, precio y duracion. ✓

## 3. Horarios y disponibilidad (completada)

- Horario semanal y excepciones (feriados, vacaciones, bloqueos). ✓
- Motor de slots, anticipacion minima, ventana maxima y buffers. ✓

## 4. Citas (completada)

- Reserva atomica sin solapamientos, cancelacion y transiciones de estado. ✓
- Historial de clientes y citas. ✓

## 5. Notificaciones (completada)

- Confirmacion y recordatorios por correo. ✓
- Outbox y reintentos confiables. ✓
- Plantillas HTML y preferencias por usuario quedan como mejora comercial.

## 6. Dashboard (completada)

- Reservas de hoy, clientes nuevos y horas disponibles. ✓
- Consultas optimizadas e indices; definiciones de metricas documentadas. ✓

## 7. Calidad comercial (línea base completada)

- CI con PostgreSQL, prueba integral crítica, imágenes y configuración productiva. ✓
- Observabilidad, correlación, hardening y runbook de backup/restauración. ✓
- Accesibilidad base y lineamientos de seguridad/privacidad. ✓
- E2E completo, prueba de restauración en staging y aprobación legal quedan como puertas de lanzamiento.

## 8. Integraciones (Google Calendar completado)

- Google Calendar OAuth, cifrado de credenciales y sincronización idempotente. ✓
- Recuperación ante revocación y reintentos mediante outbox. ✓
- Bloqueo por eventos externos, webhooks y API pública quedan como evolución.

## Siguiente etapa recomendada

Preparar la etapa de lanzamiento comercial: administración, métricas de negocio, soporte, pruebas E2E y despliegue de staging.
