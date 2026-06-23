# Reservas

Plataforma comercial de reservas para profesionales independientes. El monorepo contiene una API modular Spring Boot y una aplicación React.

## Requisitos

- Java 21 y Maven 3.9+
- Node.js 24+
- Docker Desktop

## Inicio local

```powershell
Copy-Item .env.example .env
docker compose up -d
cd backend
mvn spring-boot:run
```

En otra terminal:

```powershell
cd frontend
npm install
npm run dev
```

- API: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Salud: `http://localhost:8080/actuator/health`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Métricas Prometheus autenticadas: `http://localhost:8080/actuator/prometheus`

## Verificación

```powershell
cd backend
mvn test
mvn verify -Pintegration # requiere PostgreSQL configurado en DB_URL

cd ../frontend
npm run lint
npm run build
```

GitHub Actions ejecuta las pruebas unitarias e integrales con PostgreSQL, valida el frontend y construye ambas imágenes. Dependabot revisa Maven, npm y Actions semanalmente.

## Producción

`compose.prod.yaml` exige secretos, HTTPS en CORS, cookies seguras y SMTP. No debe usarse `.env.example` en producción.

```powershell
docker compose -f compose.prod.yaml config
docker compose -f compose.prod.yaml up -d --build
```

Consulta [docs/roadmap.md](docs/roadmap.md), [docs/architecture.md](docs/architecture.md), [docs/etapa-07-calidad-operativa.md](docs/etapa-07-calidad-operativa.md) y [docs/runbook-operaciones.md](docs/runbook-operaciones.md).

La integración opcional con Google Calendar se configura siguiendo [docs/etapa-08-google-calendar.md](docs/etapa-08-google-calendar.md). Permanece deshabilitada hasta proporcionar credenciales OAuth y una clave AES independiente.
