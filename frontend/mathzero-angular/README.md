# Mathematics Zero to Hero — Angular client

Angular 22 SPA for the platform. See the repository
[README](../../README.md) for product context and full-stack setup.

## Scripts

```bash
npm ci                # install from the lockfile
npm start             # dev server on http://localhost:4200
npm run build         # production build in dist/
npm run watch         # development build in watch mode
npm run lint          # ESLint (TypeScript and templates)
npm test -- --watch=false  # Vitest unit tests
```

## Structure

```text
src/
├── app/
│   ├── core/          API base URL, health client, i18n state
│   ├── shared/        Reusable presentation primitives
│   ├── features/      Lazy-loaded feature areas (home, not-found)
│   ├── layout/        Application shell
│   ├── app.config.ts  Router, HTTP and environment providers
│   └── app.routes.ts  Top-level lazy routes
├── environments/      Development and production API base URLs
└── styles/            Design tokens and base styles
```

The browser calls the API Gateway only. Internal service URLs must never be
added to this application.

## Requirements

- Node.js 22+ and npm 11+
- Running Gateway at <http://localhost:8090> for the live platform status card
  (the UI degrades gracefully when it is offline)
