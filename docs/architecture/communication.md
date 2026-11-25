# Service Communication

## Browser boundary

- Angular calls **only** the API Gateway at `${apiBaseUrl}/api/v1`.
- Internal service URLs, Eureka addresses and provider credentials are never
  shipped to the browser.
- Local CORS is restricted to explicitly configured origins
  (`APP_CORS_ALLOWED_ORIGINS`), not a wildcard. Credentialed requests require an
  exact origin.

## Synchronous REST

Use REST when the caller needs an immediate result.

- Versioned base path: `/api/v1`.
- JSON request and response bodies.
- Explicit DTOs; never expose JPA entities.
- Connection and read timeouts on every client.
- Bounded retries only for safe/idempotent operations.
- Non-idempotent commands require an idempotency key before retries are allowed.
- OpenAPI contract in `contracts/openapi/` and in service documentation.

### Success envelope

```json
{
  "data": { "id": "resource-uuid" },
  "meta": { "requestId": "request-id" }
}
```

### Error envelope

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The submitted data is invalid.",
    "details": [{ "field": "email", "message": "Enter a valid email address." }]
  },
  "meta": { "requestId": "request-id" }
}
```

The Gateway accepts an incoming `X-Request-Id` or generates one, so a single
identifier can correlate logs across services. Never log passwords, tokens or
unnecessary personal data.

## Asynchronous events (RabbitMQ)

Use events for cross-service side effects and long-running workflows:

- `UserRegistered`
- `LessonCompleted`
- `ExerciseAttemptRecorded`
- `SkillMasteryUpdated`
- `XpAwarded`

Rules:

- Events carry identifiers and facts, not secrets or unnecessary personal data.
- Schemas are versioned in `contracts/events/` when introduced.
- Publish through a transactional outbox; never a database transaction spanning
  services.
- Consumers are idempotent because delivery may happen more than once.
- Retry with bounded backoff and a dead-letter path; do not block the producer.
- Gamification and analytics consume events instead of querying other services'
  databases.

## External provider boundary

Only `resource-service` may call Wikimedia, OpenStax, GeoGebra, PhET, YouTube,
CNTE/Edusoutien or other external educational providers.

- Provider responses are normalized into internal DTOs; provider-specific fields
  do not leak into other services or Angular.
- Metadata and canonical links are stored by default. Copying or transforming
  content requires explicit licence permission.
- API keys stay in server-side secrets and are never logged or returned.
- Provider outage, quota exhaustion and rate limits must degrade gracefully;
  the manual resource catalogue keeps working.

## Resilience defaults

| Concern | Default |
|---|---|
| Gateway connect timeout | 2 s |
| Gateway response timeout | 10 s |
| Safe GET retries | Bounded, idempotent only |
| Circuit breakers | Only where failure isolation is justified (Phase 9) |
| Distributed transactions | Not used |

## Phase status

Phase 1 implements the Gateway boundary, CORS, request correlation and the REST
envelope for the health endpoint. RabbitMQ and event contracts are provisioned
but unused until Phase 7.
