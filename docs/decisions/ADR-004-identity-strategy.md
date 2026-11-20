# ADR-004: Dedicated identity service with standards-based tokens

- **Status:** Accepted
- **Date:** 2026-11-20
- **Phase:** 2 (design fixed in Phase 1)

## Context

Authentication is required for protected learning records, but the MVP must stay
operable without an external identity platform. A future migration to Keycloak
or another OIDC provider should remain possible. Learners may be minors, so data
minimization and session control are important.

## Decision

- Implement a dedicated `identity-service` with Spring Security and its own
  `identity_db`.
- Issue short-lived signed JWT access tokens and rotate opaque refresh tokens
  stored only as hashes server-side.
- Deliver refresh tokens in secure, `HttpOnly`, appropriately `SameSite` cookies;
  implement CSRF protection matched to the cookie strategy.
- Detect refresh-token reuse and revoke the affected token family/session.
- Logout and logout-all revoke sessions. Email verification and password reset
  tokens are single-use and expiring.
- Other services validate JWTs as OAuth2 resource servers using the published
  JWK/public key. They do not call `identity_db`.
- Roles: `STUDENT`, `CONTENT_EDITOR`, `REVIEWER`, `ADMIN`. Authorization is
  enforced on the server; hidden UI is never a control.
- Keep token validation standards-based and avoid custom identity assumptions so
  a Keycloak/OIDC migration remains possible.

No Keycloak, social login or paid identity provider is introduced in the MVP.

## Consequences

- Identity is isolated and testable; other services stay stateless for auth.
- The team owns password, refresh-token and CSRF security correctly.
- Cookie-based refresh requires deliberate CORS, CSRF, `Secure` and `SameSite`
  configuration in production.
- A later OIDC migration can replace the issuer without rewriting business
  services, provided claims and validation remain standards-based.
