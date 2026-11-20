# ADR-001: Microservices with incremental delivery

- **Status:** Accepted
- **Date:** 2026-11-20
- **Phase:** 1

## Context

The platform spans identity, curriculum, content, exercises, learning progress,
gamification, analytics and external resources. These are distinct bounded
contexts with different data and scaling characteristics. A single deployable
would couple curriculum changes to answer validation and gamification.

The team is small, and the product must prove one complete learning journey
before expanding. Creating every service, broker and orchestrator up front would
add operational cost without learning value.

## Decision

Adopt an Angular frontend with Spring Boot microservices behind a single Spring
Cloud Gateway. Deliver incrementally:

1. Phase 1: platform foundation (Angular shell, Gateway, Eureka, PostgreSQL,
   RabbitMQ).
2. Add identity, curriculum, content, resources, math, exercises, learning,
   gamification and analytics in dependency order.
3. Each service owns its bounded context, data and database credentials, and is
   independently testable and deployable.
4. Use Docker Compose for local and early deployments. Do not adopt Kubernetes
   until service operations are stable and the complexity is justified.

No empty services are scaffolded. A service appears only when its phase begins.

## Consequences

- Clear ownership and independent evolution of services.
- More operational surface than a monolith; mitigated by Compose, health checks
  and incremental adoption.
- Cross-service workflows need explicit contracts, timeouts and eventual
  consistency rather than shared tables or distributed transactions.
- The Gateway is a critical entry point and must remain thin and well monitored.
