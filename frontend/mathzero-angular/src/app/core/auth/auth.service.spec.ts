import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-config';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('keeps the login access token in memory instead of local storage', () => {
    service.login({ email: 'student@example.org', password: 'password' }).subscribe();

    httpMock.expectOne(`${baseUrl}/auth/csrf`).flush({
      data: null,
      meta: { requestId: 'csrf-login' },
    });
    httpMock.expectOne(`${baseUrl}/auth/login`).flush({
      data: {
        accessToken: 'access-token',
        expiresIn: 900,
        user: {
          id: 'user-1',
          email: 'student@example.org',
          displayName: 'Student',
          roles: ['STUDENT'],
          profile: null,
        },
      },
      meta: { requestId: 'login-1' },
    });

    expect(service.accessToken()).toBe('access-token');
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('mathzero.accessToken')).toBeNull();
    expect(localStorage.getItem('legacy.token')).toBeNull();
  });

  it('clears legacy local storage after logout while preserving language', () => {
    localStorage.setItem('legacy.token', 'old-token');
    localStorage.setItem('legacy.user', '{"name":"Old user"}');
    localStorage.setItem('mathzero.language', 'ar');

    service.logout().subscribe();

    httpMock.expectOne(`${baseUrl}/auth/csrf`).flush({
      data: null,
      meta: { requestId: 'csrf-1' },
    });
    httpMock.expectOne(`${baseUrl}/auth/logout`).flush({
      data: null,
      meta: { requestId: 'logout-1' },
    });

    expect(localStorage.getItem('legacy.token')).toBeNull();
    expect(localStorage.getItem('legacy.user')).toBeNull();
    expect(localStorage.getItem('mathzero.language')).toBe('ar');
  });

  it('clears local session data even if obtaining a CSRF token fails', () => {
    localStorage.setItem('legacy.token', 'old-token');
    localStorage.setItem('mathzero.language', 'fr');
    let logoutFailed = false;

    service.logout().subscribe({ error: () => (logoutFailed = true) });

    httpMock.expectOne(`${baseUrl}/auth/csrf`).flush(
      { message: 'Unavailable' },
      { status: 503, statusText: 'Unavailable' },
    );

    expect(logoutFailed).toBe(true);
    expect(localStorage.getItem('legacy.token')).toBeNull();
    expect(localStorage.getItem('mathzero.language')).toBe('fr');
  });
});
