# Etapa 3 — Horarios y disponibilidad

## Alcance entregado

- Múltiples tramos de trabajo por día de la semana.
- Validación de horas y rechazo de tramos superpuestos.
- Bloqueos completos o parciales para vacaciones, feriados y compromisos.
- Horarios especiales que reemplazan la semana habitual para una fecha.
- Política configurable de anticipación mínima, ventana máxima, intervalo entre inicios y buffer posterior.
- Cálculo público de slots para un servicio y un rango máximo de 31 días.
- Conversión correcta desde la zona horaria IANA del profesional a instantes UTC.
- Vista privada para administrar agenda y vista pública de próximos horarios.

## Prioridad de reglas

Para cada fecha el motor aplica estas reglas en orden:

1. toma los tramos del día de la semana;
2. si hay excepciones `AVAILABLE`, reemplaza la semana por esos tramos;
3. resta excepciones `BLOCKED` parciales o elimina el día si el bloqueo es completo;
4. genera inicios según `slotIntervalMinutes`;
5. comprueba que duración del servicio más buffer quepa en el tramo;
6. descarta inicios fuera de la anticipación y ventana de reserva;
7. convierte el resultado a UTC conservando horas locales para presentación.

En la siguiente etapa se restarán además las citas ya confirmadas. La disponibilidad es una consulta; solo la creación transaccional de una cita podrá garantizar el horario.

## Endpoints privados

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/professional/schedule/weekly` | Semana habitual |
| PUT | `/api/v1/professional/schedule/weekly` | Reemplazar semana completa |
| GET | `/api/v1/professional/schedule/policy` | Política efectiva |
| PUT | `/api/v1/professional/schedule/policy` | Actualizar política |
| GET | `/api/v1/professional/schedule/exceptions?from=&to=` | Excepciones del rango |
| POST | `/api/v1/professional/schedule/exceptions` | Crear bloqueo u horario especial |
| DELETE | `/api/v1/professional/schedule/exceptions/{id}` | Eliminar excepción propia |

## Endpoint público

```http
GET /api/v1/professionals/{slug}/availability?serviceId={uuid}&from=2026-06-22&to=2026-06-28
```

Cada slot devuelve `startAt` y `endAt` en UTC, además de las horas locales del profesional. El rango máximo de 31 días limita costo y abuso de la consulta.

## Reglas y límites

- Los horarios no pueden cruzar medianoche; se representan como dos tramos en días consecutivos.
- `BLOCKED` sin horas bloquea el día completo.
- `AVAILABLE` siempre requiere inicio y fin.
- La semana acepta hasta 35 tramos.
- La gestión de excepciones permite consultar hasta 366 días.
- La política usa bloqueo optimista para evitar sobrescrituras concurrentes.

## Verificación automatizada

- Generación por duración e intervalo.
- Conversión `America/Santiago` a UTC.
- Bloqueo parcial y completo.
- Horario especial en un día normalmente cerrado.
- Anticipación mínima.
- Rechazo de superposiciones y excepciones inválidas.
