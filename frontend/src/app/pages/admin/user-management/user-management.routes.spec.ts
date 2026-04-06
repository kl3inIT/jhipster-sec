import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, convertToParamMap } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';

import routes, { userManagementResolve } from './user-management.routes';
import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

const LAZY_IMPORT_TIMEOUT_MS = 15000;

class MockUserManagementService {
  findCalls: string[] = [];

  find(login: string) {
    this.findCalls.push(login);
    return of({ id: 1, login });
  }
}

describe('userManagementRoutes', () => {
  let service: MockUserManagementService;

  beforeEach(() => {
    service = new MockUserManagementService();
    TestBed.configureTestingModule({
      providers: [{ provide: UserManagementService, useValue: service }],
    });
  });

  function resolveUser(params: Record<string, string>): Promise<IUser | null> {
    return TestBed.runInInjectionContext(() =>
      firstValueFrom(
        userManagementResolve(
          {
            paramMap: convertToParamMap(params),
          } as ActivatedRouteSnapshot,
          {} as RouterStateSnapshot,
        ) as Observable<IUser | null>,
      ),
    );
  }

  function resolveComponentName(loadedComponent: unknown): string | undefined {
    if (typeof loadedComponent === 'function' && 'name' in loadedComponent) {
      return loadedComponent.name;
    }

    if (
      typeof loadedComponent === 'object' &&
      loadedComponent !== null &&
      'default' in loadedComponent &&
      typeof loadedComponent.default === 'function'
    ) {
      return loadedComponent.default.name;
    }

    return undefined;
  }

  function expectComponentName(loadedComponent: unknown, expectedSuffix: string): void {
    expect(resolveComponentName(loadedComponent)?.endsWith(expectedSuffix)).toBe(true);
  }

  it('exposes the route skeleton with correct paths', () => {
    expect(routes.map(route => route.path)).toEqual(['', ':login/view', 'new', ':login/edit']);
    expect(routes.every(route => route.data?.['defaultSort'] === 'id,asc')).toBe(true);
  });

  it('lazy-loads the list component for the root path', async () => {
    const rootRoute = routes.find(r => r.path === '');
    expect(rootRoute?.loadComponent).toBeDefined();
    const mod = await rootRoute!.loadComponent!();
    expectComponentName(mod, 'UserManagementListComponent');
  }, LAZY_IMPORT_TIMEOUT_MS);

  it('lazy-loads the detail component for the view path', async () => {
    const viewRoute = routes.find(r => r.path === ':login/view');
    expect(viewRoute?.loadComponent).toBeDefined();
    const mod = await viewRoute!.loadComponent!();
    expectComponentName(mod, 'UserManagementDetailComponent');
  }, LAZY_IMPORT_TIMEOUT_MS);

  it('lazy-loads the update component for the new path', async () => {
    const newRoute = routes.find(r => r.path === 'new');
    expect(newRoute?.loadComponent).toBeDefined();
    const mod = await newRoute!.loadComponent!();
    expectComponentName(mod, 'UserManagementUpdateComponent');
  }, LAZY_IMPORT_TIMEOUT_MS);

  it('lazy-loads the update component for the edit path', async () => {
    const editRoute = routes.find(r => r.path === ':login/edit');
    expect(editRoute?.loadComponent).toBeDefined();
    const mod = await editRoute!.loadComponent!();
    expectComponentName(mod, 'UserManagementUpdateComponent');
  }, LAZY_IMPORT_TIMEOUT_MS);

  it('resolves a user when the login param is present', async () => {
    const resolved = await resolveUser({ login: 'alice' });

    expect(service.findCalls).toEqual(['alice']);
    expect(resolved?.login).toBe('alice');
  });

  it('returns null when the login param is absent', async () => {
    const resolved = await resolveUser({});

    expect(service.findCalls).toEqual([]);
    expect(resolved).toBeNull();
  });
});
