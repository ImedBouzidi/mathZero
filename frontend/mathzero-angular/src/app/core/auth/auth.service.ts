import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';
import { ProfileService } from '../profile/profile.service';
import { AuthResponse, AuthUser, LoginRequest, ProfileRequest, RegisterRequest } from './auth.models';

interface ApiEnvelope<T> {
  readonly data: T;
  readonly meta: { readonly requestId: string };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly profileService = inject(ProfileService);

  private readonly accessTokenState = signal<string | null>(null);
  private readonly userState = signal<AuthUser | null>(null);
  private readonly sessionCheckedState = signal(false);
  private refreshRequest$: Observable<boolean> | null = null;

  readonly accessToken = this.accessTokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => this.userState() !== null);
  readonly sessionChecked = this.sessionCheckedState.asReadonly();

  ensureSession(): Observable<boolean> {
    if (this.sessionCheckedState()) {
      return of(this.isAuthenticated());
    }
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.refresh().pipe(
      finalize(() => {
        this.refreshRequest$ = null;
        this.sessionCheckedState.set(true);
      }),
      shareReplay(1),
    );
    return this.refreshRequest$;
  }

  ensureCsrf(): Observable<void> {
    return this.http
      .get<ApiEnvelope<void>>(`${this.baseUrl}/auth/csrf`, { withCredentials: true })
      .pipe(map(() => undefined));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.ensureCsrf().pipe(
      switchMap(() =>
        this.http
          .post<ApiEnvelope<AuthResponse>>(`${this.baseUrl}/auth/register`, request, { withCredentials: true })
          .pipe(map((response) => response.data), tap((auth) => this.setSession(auth))),
      ),
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.ensureCsrf().pipe(
      switchMap(() =>
        this.http
          .post<ApiEnvelope<AuthResponse>>(`${this.baseUrl}/auth/login`, request, { withCredentials: true })
          .pipe(map((response) => response.data), tap((auth) => this.setSession(auth))),
      ),
    );
  }

  refresh(): Observable<boolean> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }
    this.refreshRequest$ = this.ensureCsrf().pipe(
      switchMap(() =>
        this.http
          .post<ApiEnvelope<AuthResponse>>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true })
          .pipe(
            map((response) => response.data),
            tap((auth) => this.setSession(auth)),
            map(() => true),
            catchError((error: HttpErrorResponse) =>
              of(error.status === 401 ? false : false),
            ),
          ),
      ),
      finalize(() => {
        this.refreshRequest$ = null;
      }),
      shareReplay(1),
    );
    return this.refreshRequest$;
  }

  logout(): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap(() =>
        this.http
          .post<ApiEnvelope<void>>(`${this.baseUrl}/auth/logout`, {}, { withCredentials: true })
          .pipe(
            map(() => undefined),
            catchError(() => of(undefined)),
          ),
      ),
      finalize(() => this.clearSession()),
    );
  }

  logoutAll(): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap(() =>
        this.http
          .post<ApiEnvelope<void>>(`${this.baseUrl}/auth/logout-all`, {}, { withCredentials: true })
          .pipe(
            map(() => undefined),
            catchError(() => of(undefined)),
          ),
      ),
      finalize(() => this.clearSession()),
    );
  }

  me(): Observable<AuthUser> {
    return this.http
      .get<ApiEnvelope<AuthUser>>(`${this.baseUrl}/auth/me`)
      .pipe(map((response) => response.data), tap((user) => this.setUser(user)));
  }

  updateProfile(request: ProfileRequest): Observable<AuthUser> {
    return this.http
      .patch<ApiEnvelope<AuthUser>>(`${this.baseUrl}/auth/profile`, request)
      .pipe(map((response) => response.data), tap((user) => this.setUser(user)));
  }

  clearSession(): void {
    this.accessTokenState.set(null);
    this.userState.set(null);
    this.profileService.setProfile(null);
    this.clearPersistedSession();
  }

  private clearPersistedSession(): void {
    for (let index = localStorage.length - 1; index >= 0; index -= 1) {
      const key = localStorage.key(index);
      if (key && key !== 'mathzero.language') {
        localStorage.removeItem(key);
      }
    }
  }

  private setSession(auth: AuthResponse): void {
    this.accessTokenState.set(auth.accessToken);
    this.setUser(auth.user);
  }

  private setUser(user: AuthUser): void {
    this.userState.set(user);
    this.profileService.setProfile(user.profile);
  }
}
