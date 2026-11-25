import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-config';
import { CurriculumService } from './curriculum.service';

describe('CurriculumService', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let service: CurriculumService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    });
    service = TestBed.inject(CurriculumService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('uses the Gateway for paged curriculum search and unwraps the API envelope', () => {
    let response: unknown;
    service.getCurricula('Tunisie').subscribe((value) => (response = value));

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === `${baseUrl}/curricula` &&
        candidate.params.get('search') === 'Tunisie' &&
        candidate.params.get('page') === '0' &&
        candidate.params.get('size') === '100',
    );
    expect(request.request.method).toBe('GET');
    const page = { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 };
    request.flush({ data: page, meta: { requestId: 'curriculum-1' } });

    expect(response).toEqual(page);
  });

  it('encodes path identifiers and requests curriculum chapters through the Gateway', () => {
    service.getChapters('subject/id').subscribe();

    const request = httpMock.expectOne(`${baseUrl}/subjects/subject%2Fid/chapters?page=0&size=100`);
    expect(request.request.method).toBe('GET');
    request.flush({
      data: { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
      meta: { requestId: 'curriculum-2' },
    });
  });

  it('loads the three-cycle chapter catalogue through the Gateway', () => {
    let response: unknown;
    service.getCycleCatalog().subscribe((value) => (response = value));

    const request = httpMock.expectOne(`${baseUrl}/learning/curriculum/cycles`);
    expect(request.request.method).toBe('GET');
    const cycles = [
      {
        cycle: 'PRIMARY',
        nameFr: 'Primaire',
        nameAr: 'الابتدائي',
        grades: [],
      },
    ];
    request.flush({ data: cycles, meta: { requestId: 'cycles-1' } });

    expect(response).toEqual(cycles);
  });
});
