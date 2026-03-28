import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { ActivateService } from './activate.service';

describe('ActivateService', () => {
  let service: ActivateService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApplicationConfigService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ActivateService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls the activation endpoint with the provided key', () => {
    service.get('activation-key').subscribe();

    const request = httpMock.expectOne(req => req.method === 'GET' && req.url === 'api/activate');
    expect(request.request.params.get('key')).toBe('activation-key');
    request.flush(null);
  });
});
