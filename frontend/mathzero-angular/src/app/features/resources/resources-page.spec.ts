import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '../../core/config/api-config';
import { ResourcesPage } from './resources-page';

describe('ResourcesPage', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<ResourcesPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [ResourcesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResourcesPage);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('loads one approved resource list per cycle without a search query', () => {
    const requests = httpMock.match((request) => request.url === `${baseUrl}/resources`);
    expect(requests.length).toBe(3);
    expect(requests.map((request) => request.request.params.get('cycle')).sort()).toEqual([
      'BASIC',
      'PRIMARY',
      'SECONDARY',
    ]);
    expect(requests.every((request) => !request.request.params.has('search'))).toBe(true);

    for (const request of requests) {
      const cycle = request.request.params.get('cycle');
      request.flush({
        data: {
          items: [
            {
              id: `resource-${cycle}`,
              title: `Ressource ${cycle}`,
              description: '',
              url: 'https://example.org/resource',
              language: 'fr',
              resourceType: 'ARTICLE',
              cycle,
              gradeCode: null,
              subjectCode: null,
              topic: null,
              licenceCode: 'CC-BY-4.0',
              licenceName: 'Creative Commons',
              licenceUrl: 'https://creativecommons.org/licenses/by/4.0/',
              attributionText: 'Auteur',
              attributionUrl: null,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
        meta: { requestId: `resources-${cycle}` },
      });
    }
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Ressource PRIMARY');
    expect(element.textContent).toContain('Ressource BASIC');
    expect(element.textContent).toContain('Ressource SECONDARY');
    expect(element.querySelector('input[type="search"]')).toBeNull();
  });

  it('renders the cycle sections and empty states when no approved resources exist', () => {
    const requests = httpMock.match((request) => request.url === `${baseUrl}/resources`);
    expect(requests.length).toBe(3);

    for (const request of requests) {
      request.flush({
        data: {
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        },
        meta: { requestId: 'empty-resources' },
      });
    }
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Primaire');
    expect(element.textContent).toContain('Enseignement de base');
    expect(element.textContent).toContain('Secondaire');
    expect(element.textContent?.match(/Aucune ressource pour ce niveau/g)).toHaveLength(3);
    expect(element.textContent).toContain(
      'Les ressources vérifiées et approuvées apparaîtront ici pour ce cycle.',
    );
  });
});
