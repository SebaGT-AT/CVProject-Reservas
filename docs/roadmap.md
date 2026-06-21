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

## 4. Citas

- Reserva atomica sin solapamientos, cancelacion y transiciones de estado.
- Historial de clientes y citas.

## 5. Notificaciones

- Confirmacion y recordatorios por correo.
- Outbox, reintentos, plantillas y preferencias.

## 6. Dashboard

- Reservas de hoy, clientes nuevos y horas disponibles.
- Consultas optimizadas e indices; definiciones de metricas documentadas.

## 7. Calidad comercial

- Suite E2E, accesibilidad, responsive, observabilidad y backups probados.
- CI/CD, ambientes dev/staging/prod y revision OWASP ASVS.
- Politicas de privacidad, terminos, retencion y eliminacion de datos.

## 8. Integraciones

- Google Calendar OAuth y sincronizacion idempotente.
- Webhooks y API publica versionada si el negocio lo requiere.

## Siguiente etapa recomendada

Implementar citas con reserva atomica, estados, cancelaciones e historial de clientes.
