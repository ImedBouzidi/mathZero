# Mathematics Zero to Hero

Bilingual (French / Arabic) gamified mathematics learning platform aligned with the
Tunisian school curriculum, from primary school to the Baccalaureate.

> **Status: Phase 3 complete; Phase 4A foundation underway.** The Angular shell,
> API Gateway, Eureka registry, PostgreSQL, RabbitMQ, JWT identity service,
> versioned curriculum API and French/Arabic explorer are implemented. The
> resource service supports manual curation and review with opt-in Wikimedia,
> OpenStax, GeoGebra and PhET adapters feeding the admin suggestion board;
> every imported resource still waits for administrator approval. No unverified
> curriculum records are seeded.

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
| Backend | Java 17, Spring Boot 4.2, Spring Cloud 2026.0 |
| Gateway | Spring Cloud Gateway (WebFlux) |
| Discovery | Netflix Eureka |
| Data | PostgreSQL 18 (database per service, Flyway migrations) |
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
│   ├── gateway-service/         Public API entry point (port 8090)
│   ├── identity-service/        JWT identity and onboarding (port 8091)
│   ├── curriculum-service/      Versioned curriculum API (port 8082)
│   └── resource-service/        Curated resource catalogue (port 8089)
├── frontend/
│   └── mathzero-angular/        Angular application
├── infra/
│   ├── compose/                 Docker Compose for services and infrastructure
│   ├── docker/postgres/init/    Development database bootstrap
│   └── scripts/                 Import and smoke-test scripts
├── contracts/
│   └── openapi/                 Gateway, curriculum and resource API contracts
├── docs/
│   ├── architecture/            System overview, service catalog, communication
│   ├── decisions/               Architecture decision records
│   └── implementation-phases/   Phase checklists and evidence
├── .env.example
└── README.md
```

## Prerequisites

- JDK 17 or newer (`java -version`)
- Maven 3.9+
- Node.js 22+ and npm 11+
- Docker Engine with Docker Compose v2

`JAVA_HOME` must point to a full JDK, not a JRE-only install. Ensure
`JAVA_HOME/bin` comes before other Java installations in `PATH`; both
`java -version` and `javac -version` should report 17 or newer.

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
docker compose -f infra/compose/docker-compose.yml up -d postgres rabbitmq
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

### 3. Configure and start the identity service

The local administrator bootstrap reads its private password from
`backend/identity-service/.env`. Copy the example and set a private password
before starting the service:

```bash
cp -n backend/identity-service/.env.example backend/identity-service/.env
```

The `.env` file is ignored by Git. `manage-services.sh` and Docker Compose load
it automatically; when starting with Maven directly, export its values in the
shell first.

Start the service after PostgreSQL and Eureka are ready:

```bash
set -a
source backend/identity-service/.env
set +a
cd backend
mvn -pl identity-service spring-boot:run
```

```bash
cd backend
mvn -pl curriculum-service spring-boot:run
```

```bash
cd backend
mvn -pl resource-service spring-boot:run
```

The curriculum service runs Flyway migrations against `curriculum_db`. It does
not seed curriculum records; the public explorer remains empty until verified
data has been entered and published.

### 4. Start the API Gateway

In a second terminal:

```bash
cd backend
mvn -pl gateway-service spring-boot:run
```

The Gateway listens on <http://localhost:8090> and registers itself with Eureka.
Port 8090 is used because 8080 is frequently occupied by local tools; change
`GATEWAY_PORT` and the Angular `apiBaseUrl` together if needed.

### 5. Start the Angular application

In a third terminal:

```bash
cd frontend/mathzero-angular
npm ci
npm start
```

Open <http://localhost:4200>. The landing page calls the Gateway health endpoint
through the public API and shows its live status. The browser never calls
internal services directly.

Open <http://localhost:4200/curriculum> to browse published curriculum records.
The explorer intentionally shows an empty state until verified curriculum
content is entered and published by an administrator.

### Environment variables

`.env.example` documents every local, non-secret variable. Docker Compose reads
`.env` from the repository root. The identity bootstrap has its own ignored
`backend/identity-service/.env`; no password is stored in application
configuration or Git. Production must use a secret store and disable bootstrap
after the administrator is provisioned.

### Administration and resource curation

Sign in with an ADMIN account and open <http://localhost:4200/admin> to validate
and import curriculum JSON, optionally publish a verified version, curate
licensed resources, and review the publication queue. Every curated resource is
assigned to a `PRIMARY`, `BASIC` or `SECONDARY` cycle: approved resources are
grouped by cycle on <http://localhost:4200/resources>, and each experience
dashboard shows only the resources for its own cycle. The import CLI defaults
to validation-only:

```bash
./infra/scripts/import-curriculum.sh /path/to/verified-curriculum.json
DRY_RUN=false ./infra/scripts/import-curriculum.sh /path/to/verified-curriculum.json
```

See [Admin and resource workflows](docs/admin-and-resources.md) for bootstrap,
JSON format, review, and provider configuration details.

## Ports

| Component | Port | Phase |
|---|---|---|
| Angular dev server | 4200 | 1 |
| API Gateway | 8090 | 1 |
| Eureka | 8761 | 1 |
| PostgreSQL | 5432 | 1 |
| RabbitMQ AMQP | 5672 | 1 |
| RabbitMQ management | 15672 | 1 |
| Identity service | 8091 | 2 |
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

## Service manager

An interactive service manager is available at the repository root:

```bash
./manage-services.sh
```

It can also be used non-interactively:

```bash
./manage-services.sh start all
./manage-services.sh start gateway
./manage-services.sh status
./manage-services.sh view-logs curriculum
./manage-services.sh logs gateway
./manage-services.sh restart frontend
./manage-services.sh stop rabbitmq
./manage-services.sh stop all
```

From the interactive menu, choose **Start one service**, **Stop one service**,
or **Restart one service** to select PostgreSQL, RabbitMQ, Eureka, Identity,
Curriculum, Gateway, or Angular individually.

Choose **View service logs** from the menu for recent application or
infrastructure output. Use `logs <service>` to follow a live log stream; press
Ctrl+C to stop following. After start, stop, and restart actions, the manager
prints each service's status and configured port.

Manual checks:

```bash
curl http://localhost:8090/actuator/health
curl http://localhost:8090/api/v1/health
curl 'http://localhost:8090/api/v1/curricula?page=0&size=20'
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
- [Gateway API contract](contracts/openapi/gateway-service.yaml)
- [Curriculum API contract](contracts/openapi/curriculum-service.yaml)
- [Resource API contract](contracts/openapi/resource-service.yaml)
- [Admin and resource workflows](docs/admin-and-resources.md)
- [Communication rules](docs/architecture/communication.md)
- [Architecture decisions](docs/decisions/)
- [Phase 1 record](docs/implementation-phases/phase-1.md)
- [Phase 3 record](docs/implementation-phases/phase-3.md)

## Next phase

Phase 4 introduces editorial content authoring, bilingual lessons, and
publication workflows backed by the curriculum catalog.
