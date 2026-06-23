# Etapa 8: Google Calendar

## Resultado

Cada profesional puede conectar su calendario mediante OAuth 2.0. Las reservas confirmadas crean eventos y las cancelaciones los eliminan. Al conectar una cuenta se encolan también las citas futuras ya confirmadas.

La integración está deshabilitada por defecto. La API conserva únicamente el refresh token cifrado con AES-256-GCM; los access tokens son efímeros y nunca se persisten. El estado OAuth es aleatorio, de un solo uso y expira en diez minutos.

## Configuración en Google Cloud

1. Crear o seleccionar un proyecto y habilitar Google Calendar API.
2. Configurar la pantalla de consentimiento y solicitar el scope `https://www.googleapis.com/auth/calendar.events`.
3. Crear credenciales OAuth 2.0 de tipo aplicación web.
4. Registrar exactamente el callback del ambiente, por ejemplo `https://api.example.com/api/v1/integrations/google-calendar/callback`.
5. Completar las variables del ambiente y habilitar `GOOGLE_CALENDAR_ENABLED=true`.

Referencias: [OAuth 2.0 para aplicaciones web](https://developers.google.com/identity/protocols/oauth2/web-server) y [Google Calendar Events](https://developers.google.com/calendar/api/v3/reference/events).

## Variables

```text
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=https://api.example.com/api/v1/integrations/google-calendar/callback
GOOGLE_FRONTEND_RESULT_URL=https://app.example.com/perfil-profesional
GOOGLE_TOKEN_ENCRYPTION_KEY=<base64 de 32 bytes>
```

Generar una clave independiente de JWT:

```bash
openssl rand -base64 32
```

La clave de cifrado debe permanecer disponible durante toda la vida de los tokens. Rotarla requiere descifrar y volver a cifrar las conexiones existentes mediante un procedimiento explícito.

## Confiabilidad

- La escritura del outbox comparte transacción con la cita.
- El worker usa `FOR UPDATE SKIP LOCKED`, reintentos exponenciales y estado `DEAD`.
- El ID del evento se deriva del UUID de la cita y cumple el alfabeto hexadecimal admitido por Calendar; un reintento actualiza el mismo evento.
- Una nueva autorización genera una nueva generación de sincronización y vuelve a encolar las citas futuras.
- Si Google revoca el refresh token, la conexión pasa a `REAUTH_REQUIRED` y el frontend solicita autorización nuevamente.

## Decisiones de privacidad

Los eventos incluyen servicio, nombre y correo del cliente para que el profesional pueda identificarlos. No se agregan asistentes ni se envían invitaciones automáticas desde Google. Esta información y el alcance OAuth deben declararse en la política de privacidad antes del lanzamiento.

## Límites

Esta entrega sincroniza Reservas hacia el calendario principal de Google. Aún no bloquea disponibilidad usando eventos externos ni procesa webhooks de Google; ambas capacidades requieren sincronización bidireccional, canales renovables y una política clara de conflictos.
