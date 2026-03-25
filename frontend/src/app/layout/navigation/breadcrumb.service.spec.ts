import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { BreadcrumbService } from './breadcrumb.service';

describe('BreadcrumbService', () => {
  let events$: Subject<unknown>;
  let routerStateSnapshot: { root: ActivatedRouteSnapshot };

  beforeEach(() => {
    events$ = new Subject<unknown>();
    routerStateSnapshot = {
      root: buildRouteTree([]),
    };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: Router,
          useValue: {
            events: events$.asObservable(),
            routerState: {
              snapshot: routerStateSnapshot,
            },
          },
        },
      ],
    });
  });

  it('builds a section and current-page trail for list routes', () => {
    const service = TestBed.inject(BreadcrumbService);

    navigateTo(
      buildRouteTree([
        {
          path: '',
          data: {
            navigationNodeId: 'entities.organization',
            breadcrumbKey: 'global.menu.entities.organization',
            pageTitleKey: 'angappApp.organization.home.title',
          },
        },
      ]),
    );

    expect(service.items()).toEqual([
      {
        labelKey: 'global.menu.entities.main',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'global.menu.entities.organization',
        routerLink: undefined,
        current: true,
      },
    ]);
  });

  it('adds a current-page crumb for detail routes', () => {
    const service = TestBed.inject(BreadcrumbService);

    navigateTo(
      buildRouteTree([
        {
          path: ':id/view',
          data: {
            navigationNodeId: 'entities.organization',
            breadcrumbKey: 'global.menu.entities.organization',
            pageTitleKey: 'angappApp.organization.detail.title',
          },
        },
      ]),
    );

    expect(service.items()).toEqual([
      {
        labelKey: 'global.menu.entities.main',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'global.menu.entities.organization',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'angappApp.organization.detail.title',
        current: true,
      },
    ]);
  });

  it('adds a current-page crumb for edit routes', () => {
    const service = TestBed.inject(BreadcrumbService);

    navigateTo(
      buildRouteTree([
        {
          path: ':id/edit',
          data: {
            navigationNodeId: 'entities.organization',
            breadcrumbKey: 'global.menu.entities.organization',
            pageTitleKey: 'angappApp.organization.home.createOrEditLabel',
          },
        },
      ]),
    );

    expect(service.items()).toEqual([
      {
        labelKey: 'global.menu.entities.main',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'global.menu.entities.organization',
        routerLink: ['/entities/organization'],
        current: false,
      },
      {
        labelKey: 'angappApp.organization.home.createOrEditLabel',
        current: true,
      },
    ]);
  });

  it('builds security breadcrumbs for permission-matrix routes', () => {
    const service = TestBed.inject(BreadcrumbService);

    navigateTo(
      buildRouteTree([
        {
          path: 'roles/:name/permissions',
          data: {
            navigationNodeId: 'security.roles',
            breadcrumbKey: 'layout.menu.security.roles',
            pageTitleKey: 'pageTitle.permissionMatrix',
          },
        },
      ]),
    );

    expect(service.items()).toEqual([
      {
        labelKey: 'layout.menu.security.main',
        routerLink: ['/admin/users'],
        current: false,
      },
      {
        labelKey: 'layout.menu.security.roles',
        routerLink: ['/admin/security/roles'],
        current: false,
      },
      {
        labelKey: 'pageTitle.permissionMatrix',
        current: true,
      },
    ]);
  });

  function navigateTo(root: ActivatedRouteSnapshot): void {
    routerStateSnapshot.root = root;
    events$.next(new NavigationEnd(1, '/from', '/to'));
  }
});

function buildRouteTree(
  routes: Array<{
    path?: string;
    data?: Record<string, unknown>;
  }>,
): ActivatedRouteSnapshot {
  let firstChild: ActivatedRouteSnapshot | null = null;

  for (const route of [...routes].reverse()) {
    firstChild = createSnapshot(route.path, route.data ?? {}, firstChild);
  }

  return createSnapshot(undefined, {}, firstChild);
}

function createSnapshot(
  path: string | undefined,
  data: Record<string, unknown>,
  firstChild: ActivatedRouteSnapshot | null,
): ActivatedRouteSnapshot {
  return {
    data,
    firstChild,
    routeConfig:
      path === undefined ? undefined : ({ path } as ActivatedRouteSnapshot['routeConfig']),
  } as ActivatedRouteSnapshot;
}
