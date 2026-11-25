# Phase 1 — Platform Foundation

- **Status:** Complete
- **Date:** 2026-11-20
- **Branch:** `feature/phase-1-platform-foundation`
- **Scope:** Angular shell, API Gateway, Eureka, PostgreSQL, RabbitMQ, Compose,
  design tokens, health checks, local documentation.

## Delivered

### Frontend

- Angular 22 application at `frontend/mathzero-angular` with standalone
  components, strict TypeScript, lazy feature routes and Vitest.
- Responsive application shell: skip link, header, primary navigation, language
  switch, main landmark and footer.
- Design tokens for the navy/violet/emerald identity and accessible shared
  primitives (button directive, card component).
- French/Arabic language switch driving `document.lang` and `document.dir`
  (LTR/RTL), with the preference persisted locally. Formulas remain explicitly
  LTR. Inter and Noto Sans Arabic are self-hosted through Fontsource, so builds
  are offline-safe and no third-party font request is made at runtime.
- `API_BASE_URL` injection token; the browser calls the Gateway only.
- Lazy landing page that calls `/api/v1/health` and renders loading, success and
  failure states with a retry action.

### Backend and infrastructure

- Maven aggregator pinned to Spring Boot 4.2.0 and Spring Cloud 2026.0.0
  (matched release train, verified against the Spring Cloud BOM).
- `eureka-server` (8761), standalone registry with Actuator health.
- `gateway-service` (8090) using Spring Cloud Gateway WebFlux, Eureka client,
  Actuator liveness/readiness, narrow CORS and `X-Request-Id` correlation.
- Public `GET /api/v1/health` returning the standard `{data, meta}` envelope.
- Docker Compose for PostgreSQL 18 and RabbitMQ 4 with health checks, named
  volumes and the database-per-service bootstrap.
- `.env.example` centralizes non-secret local configuration; `.env` is ignored.
- OpenAPI contract for the Gateway health endpoint.
- Smoke script covering infrastructure, registry, Gateway, public health and
  CORS.

## Acceptance criteria

| Criterion | Evidence | Result |
|---|---|---|
| Angular runs locally | `ng serve` on 4200 returns HTTP 200 | Met |
| Eureka starts and shows the Gateway registered | `/eureka/apps/gateway-service` reports `GATEWAY-SERVICE`, status `UP`, port 8090 | Met |
| Gateway health endpoint responds | `/api/v1/health` returns `status: UP`, `service: gateway-service`; `/actuator/health` is `UP` | Met |
| PostgreSQL and RabbitMQ healthy in Compose | both containers report `healthy`; eight `*_db` databases exist | Met |
| No frontend bypass of the Gateway | single `API_BASE_URL`; no internal service URLs in Angular | Met |
| Automated tests | backend 7 tests, frontend 10 tests | Met |
| Build and lint | `ng build` succeeds; `ng lint` passes | Met |

## Verification commands and results

> Historical environment note: this phase was verified with Java 21. Maven
> compiled with `--release 21` using a full JDK toolchain, and the packaged
> services were executed on Java 21. The current project baseline is Java 17;
> developers need `JAVA_HOME` pointing at a full JDK 17+.

```text
$ cd backend && mvn test
eureka-server .......... Tests run: 1, Failures: 0, Errors: 0
gateway-service ........ Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS

$ cd frontend/mathzero-angular && npm run build
Application bundle generation complete.
Initial total: 289.42 kB (estimated transfer 77.95 kB)
Lazy: home-page, not-found-page, home-routes

$ npm run lint
All files pass linting.

$ npm test -- --watch=false
Test Files  4 passed (4)
Tests       10 passed (10)

$ docker compose -f infra/compose/docker-compose.yml ps
mathzero-postgres   Up (healthy)
mathzero-rabbitmq   Up (healthy)

$ ./infra/scripts/smoke-test.sh
All checks passed.
```

Observed public health response:

```json
{
  "data": {
    "status": "UP",
    "service": "gateway-service",
    "version": "0.1.0",
    "timestamp": "2026-11-20T16:27:16.502760496Z"
  },
  "meta": { "requestId": "smoke-1" }
}
```

## Decisions and deviations

- **Gateway port 8090.** Port 8080 was occupied by an unrelated local service.
  The spec permits changing reserved ports when they conflict; the mapping is
  documented in `.env.example`, the README and the service catalog. Change
  `GATEWAY_PORT`, `ANGULAR_API_BASE_URL` and the Angular `apiBaseUrl` together.
- **No UI component library yet.** Tailwind tokens and two accessible primitives
  cover the shell. Angular Material/PrimeNG is revisited when identity forms
  arrive (ADR-003). This avoids unused theming weight in Phase 1.
- **No CI workflow yet.** CI, Prometheus/Grafana and production containers are
  Phase 9 deliverables.
- **Single development database user.** Per-service least-privilege credentials
  are added with each service's migrations; Phase 1 only provisions databases.

## Known limitations

- RabbitMQ and PostgreSQL are provisioned but no service consumes them yet.
- The Gateway has no downstream routes until Phase 2.
- The language switch uses a small shell dictionary; the full i18n library lands
  with localized lesson content.
- `home.status.error` documents the Gateway port; if the port changes, update
  the translation text.

## Phase exit checklist

- [x] Working acceptance criteria
- [x] Automated tests for critical behavior
- [x] API documentation updated (health contract)
- [x] Database bootstrap committed (Flyway starts in Phase 2)
- [x] Local run instructions verified
- [x] No unresolved high-severity security issue
