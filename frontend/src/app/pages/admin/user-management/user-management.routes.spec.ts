import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, convertToParamMap } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';

import routes, { userManagementResolve } from './user-management.routes';
import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

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

  it('exposes the donor route skeleton', () => {
    expect(routes.map(route => route.path)).toEqual(['', ':login/view', 'new', ':login/edit']);
    expect(routes.every(route => route.data?.['defaultSort'] === 'id,asc')).toBe(true);
  });

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
