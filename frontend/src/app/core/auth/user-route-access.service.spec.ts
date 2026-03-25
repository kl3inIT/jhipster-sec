import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { NavigationService } from 'app/layout/navigation/navigation.service';
import { StateStorageService } from './state-storage.service';
import { UserRouteAccessService } from './user-route-access.service';

describe('UserRouteAccessService', () => {
  const authenticatedAccount = new Account(true, ['ROLE_USER'], 'user@example.com', 'User', 'en', 'Test', 'user', null);
  let currentAccount: Account | null;

  let accountService: {
    identity: ReturnType<typeof vi.fn>;
    hasAnyAuthority: ReturnType<typeof vi.fn>;
  };
  let navigationService: {
    isNodeVisible: ReturnType<typeof vi.fn>;
    getLeaf: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };
  let stateStorageService: {
    storeUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    currentAccount = authenticatedAccount;
    accountService = {
      identity: vi.fn(() => of(currentAccount)),
      hasAnyAuthority: vi.fn(() => false),
    };
    navigationService = {
      isNodeVisible: vi.fn((nodeId: string) => {
        if (nodeId === 'security.users') {
          return of((currentAccount?.authorities ?? []).includes('ROLE_ADMIN'));
        }
        return of(true);
      }),
      getLeaf: vi.fn(() => null),
    };
    router = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };
    stateStorageService = {
      storeUrl: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AccountService, useValue: accountService },
        { provide: NavigationService, useValue: navigationService },
        { provide: Router, useValue: router },
        { provide: StateStorageService, useValue: stateStorageService },
      ],
    });
  });

  async function runGuard(routeData: Record<string, unknown>, url: string): Promise<boolean> {
    return TestBed.runInInjectionContext(() =>
      firstValueFrom(
        UserRouteAccessService({ data: routeData } as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as Observable<boolean>,
      ),
    );
  }

  it('redirects unauthenticated users to /login and stores the blocked url', async () => {
    accountService.identity.mockReturnValue(of(null));

    const canActivate = await runGuard({ navigationNodeId: 'entities.organization' }, '/entities/organization');

    expect(canActivate).toBe(false);
    expect(stateStorageService.storeUrl).toHaveBeenCalledWith('/entities/organization');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('redirects authenticated hidden leaves to /accessdenied with blocked-route metadata', async () => {
    navigationService.isNodeVisible.mockReturnValue(of(false));

    const canActivate = await runGuard(
      {
        navigationNodeId: 'security.users',
        sectionId: 'security',
        breadcrumbKey: 'global.menu.admin.userManagement',
      },
      '/admin/users',
    );

    expect(canActivate).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/accessdenied'], {
      state: {
        blockedUrl: '/admin/users',
        blockedLabelKey: 'global.menu.admin.userManagement',
        sectionId: 'security',
      },
    });
  });

  it('allows the user management route when the account includes ROLE_ADMIN', async () => {
    currentAccount = new Account(true, ['ROLE_ADMIN'], 'admin@example.com', 'Admin', 'en', 'User', 'admin', null);

    const canActivate = await runGuard(
      {
        navigationNodeId: 'security.users',
        sectionId: 'security',
        breadcrumbKey: 'global.menu.admin.userManagement',
      },
      '/admin/users',
    );

    expect(canActivate).toBe(true);
    expect(navigationService.isNodeVisible).toHaveBeenCalledWith('security.users');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('denies the user management route when the account loses ROLE_ADMIN', async () => {
    currentAccount = authenticatedAccount;

    const canActivate = await runGuard(
      {
        navigationNodeId: 'security.users',
        sectionId: 'security',
        breadcrumbKey: 'global.menu.admin.userManagement',
      },
      '/admin/users',
    );

    expect(canActivate).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/accessdenied'], {
      state: {
        blockedUrl: '/admin/users',
        blockedLabelKey: 'global.menu.admin.userManagement',
        sectionId: 'security',
      },
    });
  });

  it('allows authenticated users through when the navigation leaf is visible', async () => {
    navigationService.isNodeVisible.mockReturnValue(of(true));

    const canActivate = await runGuard({ navigationNodeId: 'entities.organization' }, '/entities/organization');

    expect(canActivate).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('allows public shell routes without navigation metadata', async () => {
    const canActivate = await runGuard({}, '/accessdenied');

    expect(canActivate).toBe(true);
    expect(navigationService.isNodeVisible).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
