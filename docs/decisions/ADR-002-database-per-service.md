# ADR-002: Database per service on PostgreSQL

- **Status:** Accepted
- **Date:** 2026-11-20
- **Phase:** 1

## Context

Learning records (attempts, mastery, XP) must be trustworthy and isolated.
Cross-service joins would create hidden coupling and make schema evolution risky.
Learner data is sensitive and includes minors, so least privilege matters.

## Decision

- Use PostgreSQL for relational data.
- Each service owns one logical database and its own credentials.
- A development machine may run several logical databases on one PostgreSQL
  server; this is not permission to share tables or schemas.
- Every schema change is a Flyway migration inside the owning service.
- Cross-service references use UUIDs/codes and API or event contracts, never
  foreign keys across databases.
- Store attempts and XP transactions as append-only records; enforce idempotency
  with unique constraints/keys.
- Use `JSONB` only for genuinely flexible activity or visualization
  configuration, not as a substitute for relational modeling.

Phase 1 provisions the databases and a single administrative development user.
Per-service roles with least privilege are added with each service.

## Consequences

- Independent schema evolution and clear data ownership.
- No cross-service joins; reporting requires events/projections.
- Developers must run migrations for the service they work on.
- Production requires secret management, backups, restore testing and
  per-service credentials.
