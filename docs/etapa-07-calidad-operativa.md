# Etapa 7: calidad comercial y operación

## Resultado

La aplicación cuenta con una primera línea base operable para entregar cambios con seguridad:

- CI en cada push y pull request: Java 21, Node 24 y PostgreSQL 17.
- Prueba integral de la restricción real que impide reservas solapadas.
- Imágenes multi-stage y composición productiva sin exponer PostgreSQL.
- Health/readiness, métricas Prometheus, logs JSON y `X-Request-ID`.
- CORS configurable, cabeceras defensivas y validación fail-fast del perfil `prod`.
- Error boundary, salto al contenido, foco visible y movimiento reducido en React.
- Actualizaciones semanales de dependencias mediante Dependabot.

## Puertas de entrega

Una revisión solo debe integrarse cuando pasan `mvn verify -Pintegration`, `npm run lint`, `npm run build` y la construcción de imágenes. Los endpoints de métricas requieren autenticación; únicamente salud y readiness son públicos.

## Límites conscientes

Esta etapa no sustituye una auditoría externa, pruebas de carga ni revisión legal local. Antes del lanzamiento deben probarse restauraciones sobre staging, ejecutar un recorrido E2E de los flujos críticos y aprobar las políticas legales del país donde opere el producto.
