import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-config';
import { GatewayHealth, HealthService } from './health.service';

describe('HealthService', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let service: HealthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    });
    service = TestBed.inject(HealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls the Gateway health endpoint and unwraps the envelope', () => {
    const health: GatewayHealth = {
      status: 'UP',
      service: 'gateway-service',
      version: '0.1.0',
      timestamp: '2026-11-20T12:00:00Z',
    };
    let received: GatewayHealth | undefined;

    service.check().subscribe((value) => {
      received = value;
    });

    const request = httpMock.expectOne(`${baseUrl}/health`);
    expect(request.request.method).toBe('GET');
    request.flush({ data: health, meta: { requestId: 'request-1' } });

    expect(received).toEqual(health);
  });
});
