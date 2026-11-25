import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-config';
import { ResourceService } from './resource.service';

describe('ResourceService', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let service: ResourceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    });
    service = TestBed.inject(ResourceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('searches provider candidates in the selected language', () => {
    service.searchProviders('fractions', 10, 'ar').subscribe();

    const request = httpMock.expectOne(
      `${baseUrl}/admin/resources/search?q=fractions&limit=10&language=ar`,
    );
    expect(request.request.method).toBe('GET');
    request.flush({ data: [], meta: { requestId: 'provider-search-1' } });
  });
});
