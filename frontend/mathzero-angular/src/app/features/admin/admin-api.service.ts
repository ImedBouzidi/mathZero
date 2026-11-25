import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ResourceCycle } from '../../core/api/resource.service';
import { API_BASE_URL } from '../../core/config/api-config';

interface ApiEnvelope<T> {
  readonly data: T;
  readonly meta: { readonly requestId: string };
}

const SUGGESTION_PROVIDERS: readonly ImportProvider[] = [
  'WIKIMEDIA',
  'OPENSTAX',
  'GEOGEBRA',
  'PHET',
];

function asText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function isImportProvider(value: string): value is ImportProvider {
  return (SUGGESTION_PROVIDERS as readonly string[]).includes(value);
}

function isResourceCycle(value: string): value is ResourceCycle {
  return value === 'PRIMARY' || value === 'BASIC' || value === 'SECONDARY';
}

/** Accept both the current envelope and older/partial suggestion payloads. */
function normalizeSuggestion(raw: unknown): ResourceSuggestion | null {
  if (typeof raw !== 'object' || raw === null) {
    return null;
  }
  const item = raw as Record<string, unknown>;
  const title = asText(item['title']);
  const url = asText(item['url']);
  if (!title || !url) {
    return null;
  }

  const source = asText(item['source']);
  const provider = asText(item['provider']).toUpperCase() || source.toUpperCase();
  const cycle = asText(item['cycle']).toUpperCase();
  const language = asText(item['language']).toLowerCase();
  if (!isImportProvider(provider) || !isResourceCycle(cycle)) {
    return null;
  }

  return {
    provider,
    cycle,
    language: language === 'ar' || language === 'en' ? language : 'fr',
    query: asText(item['query']),
    title,
    url,
    snippet: asText(item['snippet']),
    source: source || provider,
  };
}

export interface CurriculumImportDocument {
  readonly code: string;
  readonly nameFr: string;
  readonly nameAr: string;
  readonly versionCode: string;
  readonly effectiveFrom: string;
  readonly effectiveTo?: string | null;
  readonly sourceReference?: string | null;
  readonly reviewer?: string | null;
  readonly publish: boolean;
  readonly levels?: readonly unknown[];
  readonly specializations?: readonly unknown[];
  readonly subjects?: readonly unknown[];
  readonly skills?: readonly unknown[];
}

export interface CurriculumImportResult {
  readonly dryRun: boolean;
  readonly valid: boolean;
  readonly published: boolean;
  readonly curriculumCode: string;
  readonly versionCode: string;
  readonly levels: number;
  readonly grades: number;
  readonly specializations: number;
  readonly subjects: number;
  readonly chapters: number;
  readonly skills: number;
  readonly errors: readonly string[];
  readonly warnings: readonly string[];
}

export type ResourceType =
  'ARTICLE' | 'AUDIO' | 'DOCUMENT' | 'IMAGE' | 'INTERACTIVE' | 'OTHER' | 'VIDEO';
export type ResourceReviewStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
export type ResourceReviewAction = 'SUBMIT' | 'APPROVE' | 'REJECT';

export type ImportProvider = 'WIKIMEDIA' | 'OPENSTAX' | 'GEOGEBRA' | 'PHET';

export interface ResourceSuggestion {
  readonly provider: ImportProvider;
  readonly cycle: ResourceCycle;
  readonly language: 'fr' | 'ar' | 'en';
  readonly query: string;
  readonly title: string;
  readonly url: string;
  readonly snippet: string;
  readonly source: string;
}

export interface ApproveSuggestionRequest {
  readonly provider: ImportProvider;
  readonly cycle: ResourceCycle;
  readonly language: 'fr' | 'ar' | 'en';
  readonly query: string;
  readonly resultUrl: string;
}

export interface AdminResource {
  readonly id: string;
  readonly provider: string;
  readonly title: string;
  readonly description: string | null;
  readonly url: string;
  readonly language: string;
  readonly resourceType: ResourceType;
  readonly cycle: ResourceCycle | null;
  readonly reviewStatus: ResourceReviewStatus;
  readonly reviewNotes: string | null;
  readonly licenceCode: string;
  readonly licenceName: string;
  readonly licenceUrl: string;
  readonly attributionText: string;
  readonly attributionUrl: string | null;
}

export interface ResourceReview {
  readonly id: string;
  readonly reviewerId: string;
  readonly decision: ResourceReviewAction;
  readonly notes: string | null;
  readonly reviewedAt: string;
}

export interface ProviderDescriptor {
  readonly provider: string;
  readonly status: string;
  readonly configured: boolean;
  readonly note: string;
}

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  validateCurriculum(document: CurriculumImportDocument): Observable<CurriculumImportResult> {
    return this.http
      .post<ApiEnvelope<CurriculumImportResult>>(
        `${this.baseUrl}/admin/curricula/import`,
        { ...document, publish: false },
        { params: new HttpParams().set('dryRun', 'true') },
      )
      .pipe(map((response) => response.data));
  }

  importCurriculum(
    document: CurriculumImportDocument,
    publish: boolean,
  ): Observable<CurriculumImportResult> {
    return this.http
      .post<ApiEnvelope<CurriculumImportResult>>(`${this.baseUrl}/admin/curricula/import`, {
        ...document,
        publish,
      })
      .pipe(map((response) => response.data));
  }

  listResources(): Observable<readonly AdminResource[]> {
    return this.http
      .get<ApiEnvelope<{ readonly items: readonly AdminResource[] }>>(
        `${this.baseUrl}/admin/resources`,
        { params: { page: '0', size: '100' } },
      )
      .pipe(map((response) => response.data?.items ?? []));
  }

  listProviders(): Observable<readonly ProviderDescriptor[]> {
    return this.http
      .get<ApiEnvelope<{ readonly providers: readonly ProviderDescriptor[] }>>(
        `${this.baseUrl}/admin/resources/providers`,
      )
      .pipe(map((response) => response.data?.providers ?? []));
  }

  listSuggestions(): Observable<readonly ResourceSuggestion[]> {
    return this.http
      .get<ApiEnvelope<unknown>>(`${this.baseUrl}/admin/resources/suggestions`)
      .pipe(
        map((response) => {
          const payload = response?.data;
          const rawItems: readonly unknown[] = Array.isArray(payload)
            ? payload
            : ((payload as { readonly items?: readonly unknown[] } | null)?.items ?? []);
          return rawItems
            .map(normalizeSuggestion)
            .filter((item): item is ResourceSuggestion => item !== null);
        }),
      );
  }

  approveSuggestion(request: ApproveSuggestionRequest): Observable<AdminResource> {
    return this.http
      .post<ApiEnvelope<AdminResource>>(
        `${this.baseUrl}/admin/resources/suggestions/approve`,
        request,
      )
      .pipe(map((response) => response.data));
  }

  reviewResource(resourceId: string, action: ResourceReviewAction): Observable<AdminResource> {
    return this.http
      .post<ApiEnvelope<AdminResource>>(
        `${this.baseUrl}/admin/resources/${encodeURIComponent(resourceId)}/review`,
        { action },
      )
      .pipe(map((response) => response.data));
  }

  reviewHistory(resourceId: string): Observable<readonly ResourceReview[]> {
    return this.http
      .get<ApiEnvelope<readonly ResourceReview[]>>(
        `${this.baseUrl}/admin/resources/${encodeURIComponent(resourceId)}/reviews`,
      )
      .pipe(map((response) => response.data));
  }
}
