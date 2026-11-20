# Mathematics Zero to Hero

Bilingual (French / Arabic) gamified mathematics learning platform aligned with the
Tunisian school curriculum, from primary school to the Baccalaureate.

> **Status: Phase 1 — Platform foundation.** The Angular shell, API Gateway,
> Eureka registry, PostgreSQL and RabbitMQ are running. Business services,
> authentication and learner features arrive in later phases.

## Product vision

Learners progress through a guided, game-like path of original lessons,
interactive mathematical visualizations and exercises with immediate,
server-validated feedback. Mathematical correctness, curriculum alignment,
accessibility and privacy take priority over decorative gamification.

The full product blueprint lives in
[`mathematics-zero-to-hero-project (2).md`](./mathematics-zero-to-hero-project%20(2).md).

## Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 22, TypeScript 6, standalone components, lazy routes, Vitest |
| Styling | Tailwind CSS 4 with a token-based design system |
| Backend | Java 21, Spring Boot 4.2, Spring Cloud 2026.0 |
| Gateway | Spring Cloud Gateway (WebFlux) |
| Discovery | Netflix Eureka |
| Data | PostgreSQL 18 (database per service, Flyway later) |
| Messaging | RabbitMQ 4 (domain events from Phase 7) |
| Build | Maven 3.9, npm 11 |
| Local infra | Docker Compose |

Versions are pinned in `backend/pom.xml` and
`frontend/mathzero-angular/package.json`. Spring Cloud 2026.0.0 is the release
train matched to Spring Boot 4.2.0.

## Repository layout

```text
.
├── backend/
│   ├── pom.xml                  Maven aggregator and dependency management
│   ├── eureka-server/           Service registry (port 8761)
│   └── gateway-service/         Public API entry point (port 8090)
├── frontend/
│   └── mathzero-angular/        Angular application
├── infra/
│   ├── compose/                 Docker Compose for PostgreSQL and RabbitMQ
│   ├── docker/postgres/init/    Development database bootstrap
│   └── scripts/                 Smoke checks
├── contracts/
│   └── openapi/                 API contracts (Gateway health for now)
├── docs/
│   ├── architecture/            System overview, service catalog, communication
│   ├── decisions/               Architecture decision records
│   └── implementation-phases/   Phase checklists and evidence
├── .env.example
└── README.md
```

## Prerequisites

- JDK 21 or newer (`java -version`)
- Maven 3.9+
- Node.js 22+ and npm 11+
- Docker Engine with Docker Compose v2

`JAVA_HOME` must point to a full JDK, not a JRE-only install:
`javac -version` should report 21 or newer.

Confirm the toolchain:

```bash
java -version && mvn -version
node --version && npm --version
docker --version && docker compose version
```

## Local development

### 1. Configure and start infrastructure

```bash
cp .env.example .env
docker compose -f infra/compose/docker-compose.yml up -d
docker compose -f infra/compose/docker-compose.yml ps
```

PostgreSQL and RabbitMQ must both report `healthy`. The PostgreSQL bootstrap
creates one logical database per service (`identity_db`, `curriculum_db`, …).

### 2. Start the service registry

```bash
cd backend
mvn -pl eureka-server spring-boot:run
```

Eureka is available at <http://localhost:8761>.

### 3. Start the API Gateway

In a second terminal:

```bash
cd backend
mvn -pl gateway-service spring-boot:run
```

The Gateway listens on <http://localhost:8090> and registers itself with Eureka.
Port 8090 is used because 8080 is frequently occupied by local tools; change
`GATEWAY_PORT` and the Angular `apiBaseUrl` together if needed.

### 4. Start the Angular application

In a third terminal:

```bash
cd frontend/mathzero-angular
npm ci
npm start
```

Open <http://localhost:4200>. The landing page calls the Gateway health endpoint
through the public API and shows its live status. The browser never calls
internal services directly.

### Environment variables

`.env.example` documents every local, non-secret variable. Docker Compose reads
`.env` from the repository root. Java services use the defaults in their
`application.yml` and can be overridden with exported environment variables; no
secrets belong in Git.

## Ports

| Component | Port | Phase |
|---|---|---|
| Angular dev server | 4200 | 1 |
| API Gateway | 8090 | 1 |
| Eureka | 8761 | 1 |
| PostgreSQL | 5432 | 1 |
| RabbitMQ AMQP | 5672 | 1 |
| RabbitMQ management | 15672 | 1 |
| Identity service | 8081 | 2 |
| Curriculum service | 8082 | 3 |
| Content service | 8083 | 4 |
| Exercise service | 8084 | 6 |
| Learning service | 8085 | 7 |
| Gamification service | 8086 | 7 |
| Math service | 8087 | 5 (only if independently deployed) |
| Analytics service | 8088 | 8 |
| Resource service | 8089 | 4A |
| Config Server | 8888 | only when needed |
| Prometheus / Grafana | 9090 / 3000 | 9 |

## Verification

```bash
# Whole-repository smoke check (infra + Eureka + Gateway)
./infra/scripts/smoke-test.sh

# Backend unit and context tests
cd backend && mvn test

# Frontend build, lint and unit tests
cd frontend/mathzero-angular
npm run build
npm run lint
npm test -- --watch=false
```

Manual checks:

```bash
curl http://localhost:8090/actuator/health
curl http://localhost:8090/api/v1/health
curl -H 'Accept: application/json' http://localhost:8761/eureka/apps/gateway-service
```

Expected Gateway payload:

```json
{
  "data": {
    "status": "UP",
    "service": "gateway-service",
    "version": "0.1.0",
    "timestamp": "2026-11-20T16:27:16Z"
  },
  "meta": { "requestId": "..." }
}
```

## Documentation

- [System overview](docs/architecture/system-overview.md)
- [Service catalog](docs/architecture/service-catalog.md)
- [Communication rules](docs/architecture/communication.md)
- [Architecture decisions](docs/decisions/)
- [Phase 1 record](docs/implementation-phases/phase-1.md)

## Next phase

Phase 2 introduces `identity-service` with registration, login, short-lived JWT
access tokens, rotating refresh tokens, roles and protected Angular routes.
