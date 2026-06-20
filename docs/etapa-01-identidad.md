# Etapa 1 — Identidad segura

## Alcance entregado

- Registro de clientes y profesionales; `ADMIN` no se puede autoasignar.
- Verificacion de correo con enlace de 24 horas y reenvio protegido contra enumeracion.
- Login con access token JWT de 15 minutos.
- Refresh token opaco de 30 dias, rotativo y almacenado como hash SHA-256.
- Cookie refresh `HttpOnly`, `SameSite=Strict`, path limitado y `Secure` configurable.
- Logout de una sesion y revocacion de todas las sesiones.
- Recuperacion de contraseña con enlace de un solo uso de 30 minutos.
- Cambio de contraseña revoca todas las sesiones existentes.
- Auditoria de eventos de identidad, IP y agente de usuario.
- Limite de intentos por IP para login, recuperacion y reenvio.
- Envio por SMTP en produccion y salida segura de desarrollo por log.

## Contrato HTTP

| Metodo | Ruta | Acceso | Resultado |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Publico | Crea cuenta y envia verificacion |
| GET | `/api/v1/auth/verify-email?token=` | Publico | Verifica correo |
| POST | `/api/v1/auth/verify-email/resend` | Publico | Reenvia sin revelar existencia |
| POST | `/api/v1/auth/login` | Publico | JWT + refresh cookie |
| POST | `/api/v1/auth/refresh` | Cookie | Rota refresh y emite JWT |
| POST | `/api/v1/auth/logout` | Cookie | Revoca sesion actual |
| POST | `/api/v1/auth/logout-all` | JWT | Revoca todas las sesiones |
| POST | `/api/v1/auth/password/forgot` | Publico | Solicita recuperacion |
| POST | `/api/v1/auth/password/reset` | Publico | Cambia contraseña y revoca sesiones |
| GET | `/api/v1/auth/me` | JWT | Devuelve usuario actual |

## Modelo de seguridad

El JWT se mantiene solo en memoria del navegador. Al recargar, el frontend llama a `/refresh`; JavaScript no puede leer el refresh token. Cada refresh revoca el token anterior, por lo que la reutilizacion de un token rotado revoca la familia de sesiones activas del usuario.

Los tokens de verificacion, recuperacion y refresh nunca se persisten en claro. Un volcado de la base de datos no permite utilizarlos directamente.

## Correo local

Con `MAIL_DELIVERY=log`, el backend escribe el enlace de desarrollo en su consola. Para SMTP:

```text
MAIL_DELIVERY=smtp
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_FROM=no-reply@example.com
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

## Verificacion manual

1. Iniciar Docker y ejecutar `docker compose up -d`.
2. Iniciar backend y frontend según el README.
3. Registrar una cuenta y abrir el enlace `DEV MAIL verification` del log.
4. Ingresar; recargar el navegador y comprobar que la sesion se restaura.
5. Cerrar sesion y comprobar que `/refresh` responde 401.
6. Solicitar recuperacion, abrir el enlace del log y cambiar la contraseña.

## Nota de escalamiento

El limite de intentos actual vive en memoria y es correcto para una sola instancia. Antes de desplegar multiples replicas se movera a Redis o al API gateway. La IP se toma de la conexion; las cabeceras reenviadas solo deben habilitarse cuando exista un proxy confiable.

