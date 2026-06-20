# Roadmap de producto

Cada etapa termina con migracion, pruebas, documentacion de API y una demo vertical.

## 0. Fundacion (actual)

- Monorepo, PostgreSQL local, Flyway y perfiles de configuracion.
- Registro/login JWT, roles base y pantalla inicial protegida.
- Convenciones de errores, validacion y salud de la API.

## 1. Identidad lista para produccion

- Verificacion de correo, recuperacion de contraseña y refresh tokens rotativos.
- Rate limiting, auditoria, cierre de sesiones y pruebas de seguridad.
- Registro diferenciado para cliente y profesional.

## 2. Profesionales y servicios

- Perfil publico, especialidades, zona horaria, servicios, precio y duracion.
- Horario semanal y excepciones (feriados, vacaciones, bloqueos).

## 3. Disponibilidad y citas

- Motor de slots, anticipacion minima, ventana maxima y buffers.
- Reserva atomica sin solapamientos, cancelacion y transiciones de estado.
- Historial de clientes y citas.

## 4. Notificaciones

- Confirmacion y recordatorios por correo.
- Outbox, reintentos, plantillas y preferencias.

## 5. Dashboard

- Reservas de hoy, clientes nuevos y horas disponibles.
- Consultas optimizadas e indices; definiciones de metricas documentadas.

## 6. Calidad comercial

- Suite E2E, accesibilidad, responsive, observabilidad y backups probados.
- CI/CD, ambientes dev/staging/prod y revision OWASP ASVS.
- Politicas de privacidad, terminos, retencion y eliminacion de datos.

## 7. Integraciones

- Google Calendar OAuth y sincronizacion idempotente.
- Webhooks y API publica versionada si el negocio lo requiere.

## Siguiente sesion recomendada

Completar la etapa 1 con refresh token seguro y verificacion de correo. Luego implementar el perfil profesional antes del motor de disponibilidad.

