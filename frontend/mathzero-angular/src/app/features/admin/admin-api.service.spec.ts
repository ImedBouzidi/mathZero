import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../core/config/api-config';
import { AdminApiService, CurriculumImportDocument } from './admin-api.service';

describe('AdminApiService', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let service: AdminApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    });
    service = TestBed.inject(AdminApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('validates uploaded documents without publishing or writing', () => {
    const document: CurriculumImportDocument = {
      code: 'VERIFIED-CURRICULUM',
      nameFr: 'Programme vérifié',
      nameAr: 'منهج موثوق',
      versionCode: 'v1',
      effectiveFrom: '2026-09-01',
      publish: true,
    };

    service.validateCurriculum(document).subscribe();

    const request = httpMock.expectOne(`${baseUrl}/admin/curricula/import?dryRun=true`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.publish).toBe(false);
    request.flush({ data: { valid: true }, meta: { requestId: 'admin-1' } });
  });

  it('publishes only when explicitly selected for the actual import', () => {
    const document: CurriculumImportDocument = {
      code: 'VERIFIED-CURRICULUM',
      nameFr: 'Programme vérifié',
      nameAr: 'منهج موثوق',
      versionCode: 'v1',
      effectiveFrom: '2026-09-01',
      publish: false,
    };

    service.importCurriculum(document, true).subscribe();

    const request = httpMock.expectOne(`${baseUrl}/admin/curricula/import`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.publish).toBe(true);
    request.flush({ data: { published: true }, meta: { requestId: 'admin-2' } });
  });

  it('loads the audit history for a resource', () => {
    const resourceId = 'resource/1';

    service.reviewHistory(resourceId).subscribe((history) => {
      expect(history).toHaveLength(1);
      expect(history[0].decision).toBe('APPROVE');
    });

    const request = httpMock.expectOne(`${baseUrl}/admin/resources/resource%2F1/reviews`);
    expect(request.request.method).toBe('GET');
    request.flush({
      data: [
        {
          id: 'review-1',
          reviewerId: 'reviewer-1',
          decision: 'APPROVE',
          notes: 'Rights checked',
          reviewedAt: '2026-01-01T10:00:00Z',
        },
      ],
      meta: { requestId: 'reviews-1' },
    });
  });
});
