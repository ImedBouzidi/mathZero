import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let httpMock: HttpTestingController;
  let refreshCalls: number;
  let accessToken: string;

  beforeEach(() => {
    refreshCalls = 0;
    accessToken = 'expired-token';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            accessToken: () => accessToken,
            refresh: () => {
              refreshCalls += 1;
              accessToken = 'refreshed-token';
              return of(true);
            },
            clearSession: () => undefined,
          },
        },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('does not send an expired bearer token or refresh recursively on a CSRF 401', () => {
    let requestFailed = false;
    TestBed.inject(HttpClient)
      .get(`${baseUrl}/auth/csrf`, { withCredentials: true })
      .subscribe({ error: () => (requestFailed = true) });

    const request = httpMock.expectOne(`${baseUrl}/auth/csrf`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.withCredentials).toBe(true);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(requestFailed).toBe(true);
    expect(refreshCalls).toBe(0);
  });

  it('retries a protected request with the refreshed token without a custom header', () => {
    let response: unknown;
    TestBed.inject(HttpClient)
      .get(`${baseUrl}/admin/resources`)
      .subscribe((value) => (response = value));

    const initialRequest = httpMock.expectOne(`${baseUrl}/admin/resources`);
    expect(initialRequest.request.headers.get('Authorization')).toBe('Bearer expired-token');
    initialRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    const retryRequest = httpMock.expectOne(`${baseUrl}/admin/resources`);
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer refreshed-token');
    expect(retryRequest.request.headers.has('X-MathZero-Retry')).toBe(false);
    retryRequest.flush({ data: [], meta: { requestId: 'retry-1' } });

    expect(refreshCalls).toBe(1);
    expect(response).toEqual({ data: [], meta: { requestId: 'retry-1' } });
  });
});
