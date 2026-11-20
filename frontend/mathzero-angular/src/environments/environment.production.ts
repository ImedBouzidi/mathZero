/**
 * Production environment.
 *
 * The API base URL is intentionally relative so the built application can be
 * served from the same origin as the Gateway (or a reverse proxy that fronts
 * it). Override it at build time if the API lives on another host.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
  defaultLanguage: 'fr' as const,
};
