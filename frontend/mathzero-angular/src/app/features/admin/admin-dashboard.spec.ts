import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../core/config/api-config';
import { AdminDashboard } from './admin-dashboard';

describe('AdminDashboard', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<AdminDashboard>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboard);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpMock.expectOne(`${baseUrl}/admin/resources?page=0&size=100`).flush({
      data: { items: [] },
      meta: { requestId: 'resources-1' },
    });
    httpMock.expectOne(`${baseUrl}/admin/resources/providers`).flush({
      data: { providers: [] },
      meta: { requestId: 'providers-1' },
    });
    httpMock.expectOne(`${baseUrl}/admin/resources/suggestions`).flush({
      data: { items: [] },
      meta: { requestId: 'suggestions-1' },
    });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('does not render the external search panel or the manual creation form', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('input[name="providerSearchQuery"]')).toBeNull();
    expect(element.querySelector('select[name="providerSearchCycle"]')).toBeNull();
    expect(element.querySelector('select[name="providerSearchLanguage"]')).toBeNull();
    expect(element.querySelector('input[name="title"]')).toBeNull();
    expect(element.querySelector('input[name="licenceCode"]')).toBeNull();
    expect(element.textContent).not.toContain('Rechercher des ressources externes');
    expect(element.textContent).not.toContain('Enregistrer comme brouillon');
  });

  it('filters the three cycle columns by the selected provider', async () => {
    const element = fixture.nativeElement as HTMLElement;
    Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Réessayer'))!
      .click();
    httpMock.expectOne(`${baseUrl}/admin/resources/suggestions`).flush({
      data: {
        items: [
          {
            provider: 'WIKIMEDIA',
            cycle: 'PRIMARY',
            language: 'fr',
            query: 'fractions',
            title: 'Fractions',
            url: 'https://fr.wikipedia.org/wiki/Fractions',
            snippet: 'Aperçu Wikimedia',
            source: 'Wikimedia',
          },
          {
            provider: 'OPENSTAX',
            cycle: 'BASIC',
            language: 'en',
            query: 'algebra',
            title: 'Elementary Algebra',
            url: 'https://openstax.org/details/books/elementary-algebra',
            snippet: 'Aperçu OpenStax',
            source: 'OPENSTAX',
          },
          {
            provider: 'PHET',
            cycle: 'SECONDARY',
            language: 'fr',
            query: 'fonction',
            title: 'Fonctions',
            url: 'https://phet.colorado.edu/fr/simulations/function-builder',
            snippet: 'Aperçu PhET',
            source: 'PHET',
          },
        ],
      },
      meta: { requestId: 'suggestions-filter' },
    });
    fixture.detectChanges();

    const dropdown = element.querySelector<HTMLSelectElement>('select[name="suggestionProvider"]')!;
    expect(dropdown.value).toBe('ALL');
    expect(dropdown.querySelectorAll('option').length).toBe(5);

    dropdown.value = 'OPENSTAX';
    dropdown.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('Aperçu OpenStax');
    expect(element.textContent).not.toContain('Aperçu Wikimedia');
    expect(element.textContent).not.toContain('Aperçu PhET');
    expect(element.textContent).toContain('Enseignement de base');
    expect(element.textContent).toContain('Primaire');
    expect(element.textContent).toContain('Secondaire');
  });

  it('renders suggestions when the response is a bare array without provider metadata', () => {
    const element = fixture.nativeElement as HTMLElement;
    Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Réessayer'))!
      .click();
    httpMock.expectOne(`${baseUrl}/admin/resources/suggestions`).flush({
      data: [
        {
          cycle: 'PRIMARY',
          language: 'fr',
          query: 'fractions',
          title: 'Fractions',
          url: 'https://fr.wikipedia.org/wiki/Fractions',
          snippet: 'Aperçu sans provider',
          source: 'Wikimedia',
        },
      ],
      meta: { requestId: 'suggestions-legacy' },
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Aperçu sans provider');
    expect(element.textContent).toContain('CC BY-SA 4.0 · Contributeurs de Wikipédia');
  });

  it('renders legacy Wikimedia suggestions without a provider field in the BASIC and SECONDARY columns', () => {
    const element = fixture.nativeElement as HTMLElement;
    Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Réessayer'))!
      .click();
    httpMock.expectOne(`${baseUrl}/admin/resources/suggestions`).flush({
      data: {
        items: [
          {
            cycle: 'BASIC',
            language: 'fr',
            query: 'équation du premier degré',
            title: 'Équation du premier degré',
            url: 'https://fr.wikipedia.org/wiki/%C3%89quation_du_premier_degr%C3%A9',
            snippet: 'Une équation du premier degré',
            source: 'Wikimedia',
          },
          {
            cycle: 'SECONDARY',
            language: 'fr',
            query: 'trigonométrie',
            title: 'Trigonométrie',
            url: 'https://fr.wikipedia.org/wiki/Trigonom%C3%A9trie',
            snippet: 'Formule de trigonométrie',
            source: 'Wikimedia',
          },
        ],
      },
      meta: { requestId: 'fb383886-8299-4894-8548-eddf2bc4e80e' },
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Équation du premier degré');
    expect(element.textContent).toContain('Une équation du premier degré');
    expect(element.textContent).toContain('Trigonométrie');
    expect(element.textContent).toContain('CC BY-SA 4.0 · Contributeurs de Wikipédia');

    const basicColumn = Array.from(element.querySelectorAll('div.rounded-2xl')).find((column) =>
      column.textContent?.includes('Enseignement de base'),
    )!;
    expect(basicColumn.textContent).toContain('Équation du premier degré');
    expect(basicColumn.textContent).not.toContain('Trigonométrie');

    const secondaryColumn = Array.from(element.querySelectorAll('div.rounded-2xl')).find((column) =>
      column.textContent?.includes('Secondaire'),
    )!;
    expect(secondaryColumn.textContent).toContain('Trigonométrie');
  });

  it('shows suggestions from every provider and publishes one with a single approval', async () => {
    const element = fixture.nativeElement as HTMLElement;
    const refreshButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Réessayer'),
    )!;
    refreshButton.click();

    httpMock.expectOne(`${baseUrl}/admin/resources/suggestions`).flush({
      data: {
        items: [
          {
            provider: 'WIKIMEDIA',
            cycle: 'PRIMARY',
            language: 'fr',
            query: 'fractions',
            title: 'Fractions',
            url: 'https://fr.wikipedia.org/wiki/Fractions',
            snippet: 'Aperçu Wikimedia',
            source: 'Wikimedia',
          },
          {
            provider: 'OPENSTAX',
            cycle: 'PRIMARY',
            language: 'en',
            query: 'fractions',
            title: 'Elementary Algebra',
            url: 'https://openstax.org/details/books/elementary-algebra',
            snippet: 'Aperçu OpenStax',
            source: 'OPENSTAX',
          },
          {
            provider: 'GEOGEBRA',
            cycle: 'BASIC',
            language: 'fr',
            query: 'géométrie',
            title: 'Géométrie dynamique',
            url: 'https://www.geogebra.org/m/abc123',
            snippet: 'Aperçu GeoGebra',
            source: 'GEOGEBRA',
          },
          {
            provider: 'PHET',
            cycle: 'SECONDARY',
            language: 'fr',
            query: 'fonction',
            title: 'Fonctions',
            url: 'https://phet.colorado.edu/fr/simulations/function-builder',
            snippet: 'Aperçu PhET',
            source: 'PHET',
          },
        ],
      },
      meta: { requestId: 'suggestions-2' },
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Suggestions par cycle');
    expect(element.textContent).toContain('Aperçu Wikimedia');
    expect(element.textContent).toContain('Aperçu OpenStax');
    expect(element.textContent).toContain('Aperçu GeoGebra');
    expect(element.textContent).toContain('Aperçu PhET');
    expect(element.textContent).toContain('CC BY 4.0 · OpenStax');
    expect(element.textContent).toContain('Licence non commerciale · GeoGebra');

    const card = Array.from(element.querySelectorAll('li')).find((item) =>
      item.textContent?.includes('Aperçu OpenStax'),
    )!;
    Array.from(card.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Approuver'))!
      .click();

    const request = httpMock.expectOne((req) =>
      req.url === `${baseUrl}/admin/resources/suggestions/approve` && req.method === 'POST',
    );
    expect(request.request.body).toEqual({
      provider: 'OPENSTAX',
      cycle: 'PRIMARY',
      language: 'en',
      query: 'fractions',
      resultUrl: 'https://openstax.org/details/books/elementary-algebra',
    });
    request.flush({
      data: {
        id: 'approved-openstax',
        provider: 'OPENSTAX',
        title: 'Elementary Algebra',
        description: 'Aperçu OpenStax',
        url: 'https://openstax.org/details/books/elementary-algebra',
        language: 'en',
        resourceType: 'DOCUMENT',
        cycle: 'PRIMARY',
        reviewStatus: 'APPROVED',
        reviewNotes: 'Approved from OPENSTAX suggestions',
        licenceCode: 'CC-BY-4.0',
        licenceName: 'Creative Commons Attribution 4.0',
        licenceUrl: 'https://creativecommons.org/licenses/by/4.0/',
        attributionText: 'OpenStax, Elementary Algebra (CC BY 4.0)',
        attributionUrl: 'https://openstax.org/details/books/elementary-algebra',
      },
      meta: { requestId: 'approve-openstax' },
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).not.toContain('Aperçu OpenStax');
    expect(element.textContent).toContain('Ressource approuvée et publiée dans son tableau de bord.');
  });

  it('shows resource rights details and loads review history from the queue', () => {
    const element = fixture.nativeElement as HTMLElement;
    const resourceId = 'resource-1';
    const refreshButtons = Array.from(element.querySelectorAll('button')).filter((button) =>
      button.textContent?.includes('Réessayer'),
    );
    refreshButtons[refreshButtons.length - 1]!.click();

    httpMock.expectOne(`${baseUrl}/admin/resources?page=0&size=100`).flush({
      data: {
        items: [
          {
            id: resourceId,
            provider: 'MANUAL',
            title: 'Fractions',
            description: 'An explanation',
            url: 'https://example.org/fractions',
            language: 'fr',
            resourceType: 'ARTICLE',
            cycle: 'BASIC',
            reviewStatus: 'SUBMITTED',
            reviewNotes: null,
            licenceCode: 'CC-BY-4.0',
            licenceName: 'Creative Commons Attribution',
            licenceUrl: 'https://creativecommons.org/licenses/by/4.0/',
            attributionText: 'Author, CC BY 4.0',
            attributionUrl: 'https://example.org/author',
          },
        ],
      },
      meta: { requestId: 'resources-2' },
    });
    fixture.detectChanges();

    expect(
      element.querySelector('a[href="https://example.org/fractions"]')?.textContent?.trim(),
    ).toBe('Fractions');
    expect(element.textContent).toContain('Author, CC BY 4.0');

    const historyButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Historique des décisions'),
    );
    historyButton!.click();
    const request = httpMock.expectOne(`${baseUrl}/admin/resources/${resourceId}/reviews`);
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
    fixture.detectChanges();

    expect(element.textContent).toContain('Approuver');
    expect(element.textContent).toContain('reviewer-1');
    expect(element.textContent).toContain('Rights checked');
  });
});
