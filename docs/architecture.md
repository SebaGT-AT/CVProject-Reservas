# Arquitectura

## Enfoque

Se parte con un **monolito modular**. Mantiene despliegue, transacciones y observabilidad simples, pero separa el dominio para permitir extraer servicios en el futuro si el volumen lo justifica.

```text
React SPA
   |
REST / JSON + JWT
   |
Spring Boot
   |-- identity       usuarios, autenticacion y roles
   |-- professionals perfiles, especialidades y horarios
   |-- customers      ficha e historial
   |-- scheduling     disponibilidad y reglas de agenda
   |-- appointments   reservas, estados y cancelaciones
   |-- notifications  correo y eventos salientes
   `-- dashboard      consultas agregadas
   |
PostgreSQL
```

## Reglas estructurales

- Paquetes por funcionalidad, no por capa global.
- Cada modulo expone casos de uso mediante servicios y DTO; sus entidades no cruzan fronteras libremente.
- Controladores delgados, validacion en el borde y reglas de negocio en servicios de aplicacion/dominio.
- Flyway es la unica fuente de cambios de esquema; Hibernate solo valida.
- Fechas persistidas en UTC (`Instant`); la zona horaria del profesional se conserva como identificador IANA.
- IDs UUID para no filtrar volumen ni acoplar futuras integraciones.
- Errores HTTP con `ProblemDetail` y un formato consistente.
- Operaciones de reserva usan transacciones y restricciones de base de datos para evitar doble reserva.

## Modelo de dominio inicial

```text
User 1---0..1 ProfessionalProfile
User 1---0..1 CustomerProfile
ProfessionalProfile 1---* ServiceOffering
ProfessionalProfile 1---* WeeklySchedule
ProfessionalProfile 1---* ScheduleException
ProfessionalProfile 1---* Appointment *---1 CustomerProfile
Appointment 1---* AppointmentStatusHistory
Appointment 1---* NotificationDelivery
```

Una `Appointment` guarda inicio/fin, servicio, precio pactado y estado. El precio y duracion se copian al reservar para que cambios futuros en el servicio no alteren el historial.

## Seguridad

- Contraseñas con BCrypt; nunca se registran ni se devuelven.
- Access token JWT corto en memoria y refresh token opaco rotativo en cookie `HttpOnly`; se almacenan hashes, hay revocacion y limite de intentos.
- Autorizacion por rol y por propiedad del recurso. `PROFESSIONAL` solo administra su agenda; `CUSTOMER` solo sus citas; `ADMIN` opera soporte.
- Secretos por variables de entorno o secret manager, nunca en Git.
- CORS restringido por ambiente y cabeceras seguras habilitadas.

## Reserva sin colisiones

La disponibilidad publicada no es una reserva. Al confirmar:

1. validar servicio, horario, excepciones y anticipacion;
2. abrir transaccion;
3. insertar la cita con clave idempotente y `busy_until` que incluye buffer;
4. una restriccion de exclusion de PostgreSQL impide solapamientos activos;
5. confirmar y publicar un evento para correo/calendario.

Los correos se procesan con **transactional outbox**, bloqueo `SKIP LOCKED`, deduplicacion y reintentos, evitando perder mensajes si el proveedor externo falla. El mismo patron alojara futuras integraciones.

## Integracion con Google Calendar

La cita interna continúa siendo la fuente de verdad. El módulo `integration.googlecalendar` contiene OAuth, cifrado y el adaptador HTTP; el dominio solo publica operaciones `UPSERT` o `DELETE` en un outbox transaccional.

Los refresh tokens se cifran con AES-256-GCM. Los eventos usan un ID determinista derivado de la cita, por lo que un reintento actualiza el mismo recurso. La conexión tiene una generación propia para volver a sincronizar citas futuras cuando el profesional autoriza otra cuenta. No se almacenan access tokens ni se aceptan instrucciones entrantes desde Google en esta etapa.

## Despliegue objetivo

- Frontend estatico en CDN.
- API en contenedor sin estado.
- PostgreSQL administrado con backups y point-in-time recovery.
- Correo transaccional administrado.
- Logs JSON, metricas Micrometer, trazas y alertas por tasa de error/latencia.
