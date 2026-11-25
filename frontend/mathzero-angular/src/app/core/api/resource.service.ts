import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';

interface ApiEnvelope<T> {
  readonly data: T;
  readonly meta: { readonly requestId: string };
}

export type ResourceCycle = 'PRIMARY' | 'BASIC' | 'SECONDARY';

export interface ResourceView {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly url: string;
  readonly language: string;
  readonly resourceType: string;
  readonly cycle: ResourceCycle | null;
  readonly gradeCode: string | null;
  readonly subjectCode: string | null;
  readonly topic: string | null;
  readonly licenceCode: string;
  readonly licenceName: string;
  readonly licenceUrl: string;
  readonly attributionText: string;
  readonly attributionUrl: string | null;
}

export interface ResourcePage {
  readonly items: readonly ResourceView[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface ProviderSearchResult {
  readonly title: string;
  readonly url: string;
  readonly snippet: string;
  readonly source: string;
}

@Injectable({ providedIn: 'root' })
export class ResourceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listApproved(options: {
    search?: string;
    language?: string;
    gradeCode?: string;
    subjectCode?: string;
    cycle?: ResourceCycle;
    page?: number;
    size?: number;
  } = {}): Observable<ResourcePage> {
    let params = new HttpParams()
      .set('page', String(options.page ?? 0))
      .set('size', String(options.size ?? 20));
    if (options.search) params = params.set('search', options.search);
    if (options.language) params = params.set('language', options.language);
    if (options.gradeCode) params = params.set('gradeCode', options.gradeCode);
    if (options.subjectCode) params = params.set('subjectCode', options.subjectCode);
    if (options.cycle) params = params.set('cycle', options.cycle);

    return this.http
      .get<ApiEnvelope<ResourcePage>>(`${this.baseUrl}/resources`, { params })
      .pipe(map((response) => response.data));
  }

  searchProviders(
    query: string,
    limit = 10,
    language: 'fr' | 'ar' = 'fr',
  ): Observable<readonly ProviderSearchResult[]> {
    return this.http
      .get<ApiEnvelope<readonly ProviderSearchResult[]>>(
        `${this.baseUrl}/admin/resources/search`,
        {
          params: new HttpParams()
            .set('q', query)
            .set('limit', String(limit))
            .set('language', language),
        },
      )
      .pipe(map((response) => response.data));
  }
}
