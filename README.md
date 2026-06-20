# Reservas

Plataforma comercial de reservas para profesionales independientes. El repositorio usa un monorepo con un backend modular y un frontend React.

## Requisitos

- Java 21
- Maven 3.9+
- Node.js 22+
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

La guía incremental está en [docs/roadmap.md](docs/roadmap.md) y las decisiones técnicas en [docs/architecture.md](docs/architecture.md).

