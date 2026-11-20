# ADR-003: Angular standalone frontend

- **Status:** Accepted
- **Date:** 2026-11-20
- **Phase:** 1

## Context

The learner experience is a long-lived SPA with bilingual content, RTL support,
interactive math visualizations and protected admin areas. The team needs
predictable structure, lazy loading and strict typing.

## Decision

- Angular 22 with TypeScript strict mode, standalone components and lazy-loaded
  feature routes.
- Feature-first structure under `src/app/features`, with `core` for
  cross-cutting concerns and `shared` for reusable presentation only.
- Angular Signals for local and derived state; RxJS for HTTP and asynchronous
  flows. Add NgRx SignalStore only when cross-feature complexity justifies it.
- Tailwind CSS 4 with design tokens as CSS custom properties and `@theme`.
- Environment-based API base URL; the browser calls the Gateway only.
- French and Arabic are first-class: a single language signal drives
  `document.lang`, `document.dir` and localized shell text. Mathematical
  notation stays `dir="ltr"` even on Arabic pages.
- Fonts are self-hosted through Fontsource (Inter Variable and Noto Sans Arabic)
  so production builds do not fetch third-party CSS and the browser makes no
  Google Fonts request — a privacy requirement for a platform used by minors.
- Vitest for unit tests, Angular's build for production bundles, ESLint +
  Prettier for quality.

Component library note: no heavy UI kit is introduced in Phase 1. Accessible
primitives (button directive, card) are token-driven. Angular Material or
PrimeNG will be selected when complex forms, tables and overlays arrive in
Phase 2+; adopting one now would add theming weight without usage. This
decision is revisited before the identity forms are built.

## Consequences

- Small initial bundle, clear domain boundaries and testable state.
- The team owns primitive accessibility instead of inheriting a kit.
- A UI kit migration may be needed later; the token layer limits the cost.
