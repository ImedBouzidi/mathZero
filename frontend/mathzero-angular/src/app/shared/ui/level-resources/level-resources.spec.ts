import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '../../../core/config/api-config';
import { LevelResources } from './level-resources';

describe('LevelResources', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<LevelResources>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [LevelResources],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LevelResources);
    fixture.componentRef.setInput('cycle', 'PRIMARY');
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('renders the approved resources returned for its cycle', () => {
    const request = httpMock.expectOne((candidate) => candidate.url === `${baseUrl}/resources`);
    expect(request.request.params.get('cycle')).toBe('PRIMARY');
    expect(request.request.params.get('size')).toBe('100');

    request.flush({
      data: {
        items: [
          {
            id: 'resource-1',
            title: 'Fraction (mathématiques)',
            description: 'Aperçu primaire',
            url: 'https://fr.wikipedia.org/wiki/Fraction_(math%C3%A9matiques)',
            language: 'fr',
            resourceType: 'ARTICLE',
            cycle: 'PRIMARY',
            gradeCode: null,
            subjectCode: null,
            topic: 'fractions',
            licenceCode: 'CC-BY-SA-4.0',
            licenceName: 'Creative Commons Attribution-ShareAlike 4.0',
            licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
            attributionText: 'Wikipedia contributors, Fraction (mathématiques) (CC BY-SA 4.0)',
            attributionUrl: 'https://fr.wikipedia.org/wiki/Fraction_(math%C3%A9matiques)',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      },
      meta: { requestId: 'resources-1' },
    });
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Fraction (mathématiques)');
    expect(text).toContain('Creative Commons Attribution-ShareAlike 4.0');
  });

  it('reloads when mounted for a different education cycle', () => {
    const primaryRequest = httpMock.expectOne(
      (candidate) => candidate.url === `${baseUrl}/resources`,
    );
    expect(primaryRequest.request.params.get('cycle')).toBe('PRIMARY');
    primaryRequest.flush({
      data: {
        items: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
      },
      meta: { requestId: 'primary-empty' },
    });
    fixture.detectChanges();

    fixture.componentRef.setInput('cycle', 'BASIC');
    fixture.detectChanges();

    const basicRequest = httpMock.expectOne(
      (candidate) => candidate.url === `${baseUrl}/resources`,
    );
    expect(basicRequest.request.params.get('cycle')).toBe('BASIC');
    basicRequest.flush({
      data: {
        items: [
          {
            id: 'basic-resource',
            title: 'Géométrie',
            description: 'Ressource de géométrie pour le cycle basic.',
            url: 'https://fr.wikipedia.org/wiki/G%C3%A9om%C3%A9trie',
            language: 'fr',
            resourceType: 'ARTICLE',
            cycle: 'BASIC',
            gradeCode: null,
            subjectCode: null,
            topic: 'géométrie',
            licenceCode: 'CC-BY-SA-4.0',
            licenceName: 'Creative Commons Attribution-ShareAlike 4.0',
            licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
            attributionText: 'Wikipedia contributors',
            attributionUrl: null,
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      },
      meta: { requestId: 'basic-resource' },
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Géométrie');
  });
});
