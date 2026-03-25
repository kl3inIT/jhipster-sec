import { HTTP_INTERCEPTORS, HttpClient, HttpHeaders, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AlertService } from 'app/core/util/alert.service';
import { NotificationInterceptor } from './notification.interceptor';

class MockAlertService {
  readonly calls: Array<Record<string, unknown>> = [];

  addAlert(alert: Record<string, unknown>): void {
    this.calls.push(alert);
  }
}

describe('NotificationInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let alertService: MockAlertService;

  beforeEach(() => {
    alertService = new MockAlertService();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: AlertService, useValue: alertService },
        { provide: HTTP_INTERCEPTORS, useClass: NotificationInterceptor, multi: true },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('normalizes user management headers into donor translation keys', () => {
    http.post('/api/test', {}).subscribe();

    const request = httpMock.expectOne('/api/test');
    request.flush(
      {},
      {
        headers: new HttpHeaders({
          'x-jhipstersec-app-alert': 'jhipstersec.userManagement.created',
          'x-jhipstersec-app-params': 'alice',
        }),
      },
    );

    expect(alertService.calls[0]).toEqual({
      type: 'success',
      translationKey: 'userManagement.created',
      translationParams: { param: 'alice' },
    });
  });

  it('normalizes admin authority and security alert headers', () => {
    http.post('/api/admin', {}).subscribe();
    http.get('/api/security').subscribe();

    const adminRequest = httpMock.expectOne('/api/admin');
    adminRequest.flush(
      {},
      {
        headers: new HttpHeaders({
          'x-jhipstersec-app-alert': 'jhipstersec.adminAuthority.created',
          'x-jhipstersec-app-params': 'ROLE_EDITOR',
        }),
      },
    );

    const securityRequest = httpMock.expectOne('/api/security');
    securityRequest.flush(
      {},
      {
        headers: new HttpHeaders({
          'x-jhipstersec-app-alert': 'jhipstersec.secRole.created',
          'x-jhipstersec-app-params': 'SEC_AUDITOR',
        }),
      },
    );

    expect(alertService.calls[0]?.['translationKey']).toBe('angappApp.adminAuthority.created');
    expect(alertService.calls[1]?.['translationKey']).toBe('security.alert.secRole.created');
  });

  it('passes through unknown translation keys unchanged', () => {
    http.post('/api/custom', {}).subscribe();

    const request = httpMock.expectOne('/api/custom');
    request.flush(
      {},
      {
        headers: new HttpHeaders({
          'x-jhipstersec-app-alert': 'custom.key',
          'x-jhipstersec-app-params': 'value',
        }),
      },
    );

    expect(alertService.calls[0]?.['translationKey']).toBe('custom.key');
  });
});
