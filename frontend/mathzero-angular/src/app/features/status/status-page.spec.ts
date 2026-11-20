import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '../../core/config/api-config';
import { StatusPage } from './status-page';

describe('StatusPage', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<StatusPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [StatusPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusPage);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('shows a loading state before the Gateway responds', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Vérification de la passerelle');
    httpMock.expectOne(`${baseUrl}/health`);
  });

  it('renders the Gateway status when the health check succeeds', () => {
    const request = httpMock.expectOne(`${baseUrl}/health`);
    request.flush({
      data: {
        status: 'UP',
        service: 'gateway-service',
        version: '0.1.0',
        timestamp: new Date().toISOString(),
      },
      meta: { requestId: 'request-1' },
    });
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('En ligne');
    expect(text).toContain('gateway-service');
    expect(text).toContain('0.1.0');
  });

  it('renders a recoverable error state when the Gateway is unavailable', () => {
    const request = httpMock.expectOne(`${baseUrl}/health`);
    request.flush('unavailable', { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Indisponible');
    expect(element.querySelector('[role="alert"]')).toBeTruthy();
    expect(element.textContent).toContain('Réessayer');
  });
});
