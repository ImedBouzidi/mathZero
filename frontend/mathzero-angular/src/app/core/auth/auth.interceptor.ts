import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken();
  const isPublicAuthEndpoint = [
    '/auth/csrf',
    '/auth/login',
    '/auth/register',
    '/auth/refresh',
    '/auth/logout',
    '/auth/logout-all',
  ].some((endpoint) => request.url.includes(endpoint));
  let headers = request.headers;

  if (token && !isPublicAuthEndpoint) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  if (['POST', 'PATCH', 'DELETE'].includes(request.method.toUpperCase())) {
    const csrf = document.cookie
      .split('; ')
      .find((entry) => entry.startsWith('mathzero_csrf='))
      ?.split('=')[1];
    if (csrf) {
      headers = headers.set('X-CSRF-Token', decodeURIComponent(csrf));
    }
  }

  const authenticatedRequest = request.clone({ headers, withCredentials: request.url.includes('/auth/') });
  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isPublicAuthEndpoint || !token) {
        if (error.status === 401 && !isPublicAuthEndpoint) {
          auth.clearSession();
        }
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap((refreshed) => {
          if (!refreshed) {
            auth.clearSession();
            return throwError(() => error);
          }
          const retryHeaders = request.headers.set(
            'Authorization',
            `Bearer ${auth.accessToken()}`,
          );
          const retry = request.clone({
            headers: retryHeaders,
            withCredentials: true,
          });
          return next(retry);
        }),
      );
    }),
  );
};
