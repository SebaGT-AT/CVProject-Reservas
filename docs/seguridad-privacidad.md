# Línea base de seguridad y privacidad

## Datos y acceso

El sistema trata identidad, contacto, historial de citas y datos operativos. El acceso debe seguir mínimo privilegio: clientes ven su historial, profesionales su agenda y los procesos técnicos solo lo necesario. Los secretos viven en un gestor externo y se rotan ante exposición.

## Retención

Antes del lanzamiento, negocio y asesoría legal deben definir por jurisdicción:

- plazo del historial y auditoría;
- exportación, rectificación y eliminación solicitada por el titular;
- anonimización de métricas y respaldos expirados;
- encargado de tratamiento y proveedores de correo/infraestructura.

La eliminación debe preservar únicamente lo exigido legalmente y no romper trazabilidad financiera u operativa.

## Revisión previa al lanzamiento

- Modelo de amenazas y checklist OWASP ASVS para autenticación, sesiones, control de acceso y validación.
- TLS extremo a extremo, cookies `Secure`, CORS exacto y cabeceras verificadas.
- Escaneo de dependencias e imágenes, rotación de secretos y prueba de restauración.
- Pruebas de autorización entre dos clientes y dos profesionales distintos.
- Política de privacidad, términos, consentimiento y canal para derechos del titular aprobados legalmente.
