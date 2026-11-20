# Service Catalog

This catalog is the single reference for service boundaries, data ownership and
local ports. A service is only created when its implementation phase begins.

## Implemented services (Phase 1)

| Service | Responsibility | Owns data | Port | Dependencies | Status |
|---|---|---|---|---|---|
| `eureka-server` | Service registration and discovery | None | 8761 | — | Implemented |
| `gateway-service` | Public API entry point, routing, CORS, request correlation, token relay later | No business database | 8090 | Eureka | Implemented |

The Gateway runs on **8090** locally because 8080 is commonly occupied by
unrelated tools. Override with `GATEWAY_PORT`; if changed, update
`ANGULAR_API_BASE_URL` and the Angular development `apiBaseUrl`.

## Planned services

| Service | Responsibility | Owns data | Port | Phase | Depends on |
|---|---|---|---|---|---|
| `identity-service` | Registration, login, password lifecycle, refresh sessions, roles, profile basics | `identity_db` | 8081 | 2 | Gateway |
| `curriculum-service` | Curriculum versions, levels, grades, specializations, subjects, chapters, skills, prerequisites | `curriculum_db` | 8082 | 3 | Gateway, identity (admin auth) |
| `content-service` | Original lessons, translations, activities, editorial workflow, publication versions | `content_db` | 8083 | 4 | Gateway, curriculum references, identity |
| `resource-service` | External provider adapters, normalized metadata, caching, licences, attribution, review | `resource_db` | 8089 | 4A | Gateway, identity, external providers |
| `math-service` | Safe parsing/evaluation and visualization configuration validation; stateless initially | None initially | 8087 | 5 | None |
| `exercise-service` | Exercise definitions, answer specifications, attempts, validation | `exercise_db` | 8084 | 6 | Gateway, curriculum references, math rules |
| `learning-service` | Learning paths, lesson progress, mastery, revision schedule, recommendations | `learning_db` | 8085 | 7 | Events from content/exercise, identity |
| `gamification-service` | XP ledger, levels, achievements, streaks, challenges | `gamification_db` | 8086 | 7 | Events from learning/exercise |
| `analytics-service` | Read-optimized learner/admin projections and reports | `analytics_db` | 8088 | 8 | Events from all learning services |

`math-service` may remain colocated with exercise evaluation until independent
deployment provides concrete value. Do not deploy an empty service to satisfy a
diagram.

## Supporting infrastructure

| Component | Port | Phase | Notes |
|---|---|---|---|
| PostgreSQL 18 | 5432 | 1 | One logical database per service |
| RabbitMQ 4 | 5672 / 15672 | 1 | AMQP and management UI; events from Phase 7 |
| Angular dev server | 4200 | 1 | Serves the SPA in development |
| Config Server | 8888 | When needed | Not required for Phase 1 |
| Prometheus | 9090 | 9 | After metrics endpoints exist |
| Grafana | 3000 | 9 | After metrics endpoints exist |

## Data ownership rules

1. Each service has exactly one logical database and its own credentials in
   non-development environments.
2. Flyway migrations live inside the owning service; no shared migration
   directory.
3. Foreign keys never cross service databases. Curriculum IDs, lesson IDs and
   user IDs are stored as external references (UUID/code), validated through
   APIs or events.
4. A shared PostgreSQL server is a deployment convenience, not a shared schema.
5. Published lesson versions, attempts and XP transactions are append-only
   where practical.

## Local database bootstrap

`infra/docker/postgres/init/01-databases.sql` creates:

```text
identity_db
curriculum_db
content_db
resource_db
exercise_db
learning_db
gamification_db
analytics_db
```

Per-service roles and least-privilege credentials are introduced with each
service's migrations. Development uses a single administrative user; production
must not.
