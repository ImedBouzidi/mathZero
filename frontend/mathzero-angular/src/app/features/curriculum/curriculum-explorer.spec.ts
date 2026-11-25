import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../core/config/api-config';
import { LanguageService } from '../../core/i18n/language.service';
import { CurriculumExplorer } from './curriculum-explorer';

describe('CurriculumExplorer', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<CurriculumExplorer>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [CurriculumExplorer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CurriculumExplorer);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('shows loading and then the honest empty state when no verified curricula are published', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Chargement');
    const request = httpMock.expectOne(`${baseUrl}/curricula?page=0&size=100`);
    request.flush({
      data: { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
      meta: { requestId: 'curriculum-1' },
    });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Le programme sera bientôt disponible');
    expect(element.textContent).not.toContain('Mathématiques —');
  });

  it('offers retry after a failed request and keeps errors explicit', () => {
    httpMock.expectOne(`${baseUrl}/curricula?page=0&size=100`).flush('offline', {
      status: 503,
      statusText: 'Service Unavailable',
    });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[role="alert"]')).toBeTruthy();
    expect(element.textContent).toContain('Réessayer');

    (element.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    httpMock.expectOne(`${baseUrl}/curricula?page=0&size=100`).flush({
      data: { items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
      meta: { requestId: 'curriculum-2' },
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')).toBeNull();
  });

  it('renders labels in the selected language and loads the curriculum hierarchy', () => {
    httpMock.expectOne(`${baseUrl}/curricula?page=0&size=100`).flush({
      data: {
        items: [
          {
            id: 'curriculum-1',
            code: 'TN',
            nameFr: 'Programme tunisien',
            nameAr: 'المنهج التونسي',
            currentVersion: {
              id: 'version-1',
              code: '2026',
              effectiveFrom: '2026-09-01',
              effectiveTo: null,
            },
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      },
      meta: { requestId: 'curriculum-3' },
    });
    TestBed.inject(LanguageService).setLanguage('ar');
    fixture.detectChanges();

    const curriculumButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.curriculum-card',
    ) as HTMLButtonElement;
    expect(curriculumButton.textContent).toContain('المنهج التونسي');
    curriculumButton.click();
    fixture.detectChanges();

    httpMock.expectOne(`${baseUrl}/versions/version-1/levels?page=0&size=100`).flush({
      data: {
        items: [
          {
            id: 'level-1',
            code: 'PRIMARY',
            nameFr: 'Primaire',
            nameAr: 'الابتدائي',
            displayOrder: 0,
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      },
      meta: { requestId: 'curriculum-4' },
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('الابتدائي');
    expect(document.documentElement.dir).toBe('rtl');
  });
});
