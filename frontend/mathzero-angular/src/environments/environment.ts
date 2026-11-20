/**
 * Development environment.
 *
 * The browser talks to the API Gateway only; internal service URLs are never
 * exposed to the Angular application.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8090/api/v1',
  defaultLanguage: 'fr' as const,
};
