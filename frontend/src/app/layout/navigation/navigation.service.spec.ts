import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReplaySubject, firstValueFrom } from 'rxjs';

import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { NAVIGATION_STORAGE_KEY, SHELL_APP_NAME } from './navigation.constants';
import { NavigationService } from './navigation.service';

describe('NavigationService', () => {
  let httpMock: HttpTestingController;
  let authenticationState$: ReplaySubject<Account | null>;

  const adminAccount = new Account(
    true,
    ['ROLE_ADMIN', 'ROLE_USER'],
    'admin@test.com',
    'Admin',
    'en',
    'User',
    'admin',
    null,
  );

  function configureTestingModule(): void {
    authenticationState$ = new ReplaySubject<Account | null>(1);
    authenticationState$.next(adminAccount);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ApplicationConfigService, useValue: { getEndpointFor: (path: string) => path } },
        { provide: AccountService, useValue: { getAuthenticationState: () => authenticationState$.asObservable() } },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  }

  beforeEach(() => {
    sessionStorage.clear();
    configureTestingModule();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  it('requests navigation grants with the configured app name', async () => {
    const service = TestBed.inject(NavigationService);
    const queryPromise = firstValueFrom(service.query());

    const request = httpMock.expectOne(
      req => req.url === 'api/security/navigation-grants' && req.params.get('appName') === SHELL_APP_NAME,
    );
    expect(request.request.method).toBe('GET');
    request.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['home.dashboard', 'security.roles'] });

    expect(await queryPromise).toEqual({
      appName: SHELL_APP_NAME,
      allowedNodeIds: ['home.dashboard', 'security.roles'],
    });
  });

  it('reuses the in-memory cache without issuing a second HTTP request', async () => {
    const service = TestBed.inject(NavigationService);

    const firstQuery = firstValueFrom(service.query());
    const request = httpMock.expectOne(req => req.url === 'api/security/navigation-grants');
    request.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['home.dashboard', 'entities.organization'] });

    const secondQuery = firstValueFrom(service.query());

    httpMock.expectNone(req => req.url === 'api/security/navigation-grants');
    expect(await firstQuery).toEqual({
      appName: SHELL_APP_NAME,
      allowedNodeIds: ['home.dashboard', 'entities.organization'],
    });
    expect(await secondQuery).toEqual({
      appName: SHELL_APP_NAME,
      allowedNodeIds: ['home.dashboard', 'entities.organization'],
    });
  });

  it('uses a sessionStorage warm start when cached grants are present', async () => {
    sessionStorage.setItem(
      NAVIGATION_STORAGE_KEY,
      JSON.stringify({ appName: SHELL_APP_NAME, allowedNodeIds: ['home.dashboard', 'entities.employee'] }),
    );
    sessionStorage.setItem(`${NAVIGATION_STORAGE_KEY}:login`, 'admin');

    const service = TestBed.inject(NavigationService);
    const response = await firstValueFrom(service.query());

    httpMock.expectNone(req => req.url === 'api/security/navigation-grants');
    expect(response).toEqual({
      appName: SHELL_APP_NAME,
      allowedNodeIds: ['home.dashboard', 'entities.employee'],
    });
  });

  it('clears both caches when the authenticated login changes', async () => {
    const service = TestBed.inject(NavigationService);

    const firstQuery = firstValueFrom(service.query());
    const firstRequest = httpMock.expectOne(req => req.url === 'api/security/navigation-grants');
    firstRequest.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['home.dashboard'] });
    await firstQuery;

    expect(sessionStorage.getItem(NAVIGATION_STORAGE_KEY)).toContain('home.dashboard');

    authenticationState$.next(
      new Account(true, ['ROLE_USER'], 'user@test.com', 'Regular', 'en', 'User', 'user', null),
    );

    expect(sessionStorage.getItem(NAVIGATION_STORAGE_KEY)).toBeNull();

    const secondQuery = firstValueFrom(service.query());
    const secondRequest = httpMock.expectOne(req => req.url === 'api/security/navigation-grants');
    secondRequest.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['entities.department'] });

    expect(await secondQuery).toEqual({
      appName: SHELL_APP_NAME,
      allowedNodeIds: ['entities.department'],
    });
  });

  it('hides sections whose filtered child list becomes empty', async () => {
    const service = TestBed.inject(NavigationService);
    const visibleTreePromise = firstValueFrom(service.visibleTree());

    const request = httpMock.expectOne(req => req.url === 'api/security/navigation-grants');
    request.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['entities.department'] });

    const visibleTree = await visibleTreePromise;
    expect(visibleTree).toHaveLength(1);
    expect(visibleTree[0]?.id).toBe('entities');
    expect(visibleTree[0]?.children.map(child => child.id)).toEqual(['entities.department']);
    expect(visibleTree.some(section => section.id === 'home')).toBe(false);
    expect(visibleTree.some(section => section.id === 'security')).toBe(false);
  });

  it('falls back to the first allowed route in registry order when a section has no visible sibling', async () => {
    const service = TestBed.inject(NavigationService);
    const fallbackPromise = firstValueFrom(service.resolveFallbackRoute('security'));

    const request = httpMock.expectOne(req => req.url === 'api/security/navigation-grants');
    request.flush({ appName: SHELL_APP_NAME, allowedNodeIds: ['entities.employee', 'security.row-policies'] });

    expect(await fallbackPromise).toBe('/admin/security/row-policies');

    const overallFallback = await firstValueFrom(service.resolveFallbackRoute('home'));
    expect(overallFallback).toBe('/entities/employee');
  });
});
