import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';

/** Standard success envelope returned by the public API. */
export interface ApiEnvelope<T> {
  data: T;
  meta: {
    requestId: string;
  };
}

/** Public health snapshot of the API Gateway. */
export interface GatewayHealth {
  status: string;
  service: string;
  version: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  check(): Observable<GatewayHealth> {
    return this.http
      .get<ApiEnvelope<GatewayHealth>>(`${this.baseUrl}/health`)
      .pipe(map((response) => response.data));
  }
}
