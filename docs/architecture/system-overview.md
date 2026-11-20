# System Overview

Mathematics Zero to Hero is a bilingual learning platform for the Tunisian
curriculum. This document describes the target architecture and the subset that
exists after Phase 1.

## Architectural principles

1. **Bounded contexts.** Each service owns its business logic, data and database
   credentials. A service never reads another service's tables.
2. **Single browser entry point.** Angular calls the API Gateway only. Internal
   service addresses and provider credentials never reach the browser.
3. **Incremental delivery.** Services are added when their phase begins. There
   are no placeholder services in this repository.
4. **Synchronous where a response is required.** REST is used for queries and
   commands whose caller needs an immediate answer.
5. **Asynchronous for side effects.** RabbitMQ events drive cross-service
   projections, gamification and analytics, with an outbox and idempotent
   consumers.
6. **Deterministic mathematics.** The restricted parser and answer validation
   never depend on an AI provider.

## Target architecture

```text
                         Learner / Admin Browser
                                  |
                                  v
                       Angular Web Application
                                  |
                           HTTPS /api/v1
                                  |
                                  v
                         Spring Cloud Gateway
                                  |
             +--------------------+--------------------+
             |                    |                    |
             v                    v                    v
      Identity Service     Curriculum Service     Content Service
             |                    |                    |
        identity_db         curriculum_db          content_db
                                  |
                                  v
                           Resource Service
                             resource_db
             |
             +--------------------+--------------------+
                                  |
                   +--------------+--------------+
                   |              |              |
                   v              v              v
             Exercise Service  Learning Service  Math Service
                   |              |              |
               exercise_db     learning_db       (stateless)
                   |              |
                   +------+-------+
                          |
                          v
                   Gamification Service
                          |
                    gamification_db

             Analytics Service (event projections)
                          |
                     analytics_db

  Resource Service -> Wikimedia / YouTube / authorized CNTE access

  Eureka: service registration        RabbitMQ: domain events
  PostgreSQL: database per service    Prometheus + Grafana: observability
```

The diagram is logical. Early development may run all logical databases on one
PostgreSQL server, but every service still owns its database, credentials and
Flyway migration history.

## Phase 1 implementation

```text
  Browser
     |  http://localhost:4200
     v
  Angular 22 SPA (dev server / static hosting)
     |  http://localhost:8090/api/v1
     v
  gateway-service (Spring Cloud Gateway, WebFlux)
     |  registers on startup
     v
  eureka-server (port 8761)

  Docker Compose:
    PostgreSQL 18  -> identity_db, curriculum_db, content_db, resource_db,
                      exercise_db, learning_db, gamification_db, analytics_db
    RabbitMQ 4     -> ready for Phase 7 events
```

Implemented:

- Angular shell with responsive layout, design tokens, French/Arabic switching
  and LTR/RTL document handling.
- Lazy-loaded landing route that calls the Gateway health endpoint and renders
  loading, success and failure states.
- `gateway-service` with Actuator health, a public `/api/v1/health` endpoint,
  narrow CORS policy, request correlation ID and Eureka registration.
- `eureka-server` with a standalone registry configuration.
- PostgreSQL and RabbitMQ with health checks and persistent development volumes.
- Centralized port and non-secret environment configuration.

Not implemented yet (by phase):

- Identity, curriculum, content, exercise, learning, gamification, analytics and
  resource services.
- Authentication and authorization.
- RabbitMQ publishers/consumers, outbox, distributed tracing.
- Prometheus/Grafana, CI/CD and production containers.

## Runtime and data flow

1. The Angular application requests `${apiBaseUrl}/health` from the Gateway.
2. CORS allows only explicit origins (`http://localhost:4200` by default).
3. The Gateway adds/accepts `X-Request-Id`, logs operations and returns the
   standard `{ data, meta }` envelope.
4. Operational tooling uses `/actuator/health` and its liveness/readiness
   groups; the browser never depends on Actuator internals.
5. Later, the Gateway routes `/api/v1/**` paths to services discovered through
   Eureka; the Angular application never changes its base URL.

## Environments

| Concern | Local | Production |
|---|---|---|
| Angular | `ng serve` on 4200 | Static hosting/CDN, relative `/api/v1` |
| Gateway | Maven/IDE on 8090 | HTTPS entry point, managed deployment |
| Services | Maven/IDE | Private network, container images |
| Data | Compose PostgreSQL/RabbitMQ | Managed PostgreSQL, RabbitMQ with backups |
| Secrets | `.env` (ignored) | Secret manager |

## Related documents

- [Service catalog](service-catalog.md)
- [Communication rules](communication.md)
- [ADR-001: Microservices](../decisions/ADR-001-microservices.md)
- [ADR-002: Database per service](../decisions/ADR-002-database-per-service.md)
- [ADR-003: Angular frontend](../decisions/ADR-003-angular-frontend.md)
- [ADR-004: Identity strategy](../decisions/ADR-004-identity-strategy.md)
