import { Routes } from '@angular/router';

import adminRoutes from './app/pages/admin/admin.routes';
import securityRoutes from './app/pages/admin/security/security.routes';
import { appRoutes } from './app.routes';

describe('appRoutes', () => {
  it('exposes login and the shell routes with translated titles', () => {
    const shellRoute = appRoutes.find(route => route.path === '');
    const childPaths = shellRoute?.children?.map(route => route.path) ?? [];

    expect(appRoutes.some(route => route.path === 'login' && route.title === 'pageTitle.login')).toBe(true);
    expect(childPaths).toEqual(['', 'accessdenied', '404', 'error', 'entities', 'admin']);
    expect(shellRoute?.children?.find(route => route.path === '')?.title).toBe('pageTitle.home');
    expect(shellRoute?.children?.find(route => route.path === 'accessdenied')?.title).toBe('pageTitle.accessDenied');
    expect(shellRoute?.children?.find(route => route.path === '404')?.title).toBe('pageTitle.notFound');
    expect(shellRoute?.children?.find(route => route.path === 'error')?.title).toBe('pageTitle.error');
  });

  it('guards the admin route for ROLE_ADMIN and mounts security and users children', async () => {
    const shellRoute = appRoutes.find(route => route.path === '');
    const adminRoute = shellRoute?.children?.find(route => route.path === 'admin');
    const usersRoute = adminRoutes.find(route => route.path === 'users');
    const loadedUserRouteModule = await usersRoute?.loadChildren?.();
    const loadedUserRoutes = Array.isArray(loadedUserRouteModule)
      ? loadedUserRouteModule
      : ((loadedUserRouteModule as { default?: Routes } | undefined)?.default ?? undefined);

    expect(adminRoute?.data?.['authorities']).toEqual(['ROLE_ADMIN']);
    expect(adminRoutes.map(route => route.path)).toEqual(['security', 'users']);
    expect(loadedUserRoutes?.map(route => route.path)).toEqual(['', ':login/view', 'new', ':login/edit']);
  });

  it('uses translation keys for security route titles', () => {
    expect(securityRoutes.map(route => route.title)).toEqual([
      'pageTitle.securityRoles',
      'pageTitle.permissionMatrix',
      'pageTitle.rowPolicies',
    ]);
  });
});
