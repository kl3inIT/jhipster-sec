import { Routes } from '@angular/router';

import adminRoutes from './app/pages/admin/admin.routes';
import securityRoutes from './app/pages/admin/security/security.routes';
import userManagementRoutes from './app/pages/admin/user-management/user-management.routes';
import { appRoutes } from './app.routes';

describe('appRoutes', () => {
  it('exposes login and the shell routes with translated titles', () => {
    const shellRoute = appRoutes.find(route => route.path === '');
    const childPaths = shellRoute?.children?.map(route => route.path) ?? [];

    expect(appRoutes.some(route => route.path === 'login' && route.title === 'pageTitle.login')).toBe(true);
    expect(childPaths).toEqual(['', 'accessdenied', '404', 'error', 'entities', 'admin']);
    expect(shellRoute?.children?.find(route => route.path === '')?.title).toBe('pageTitle.home');
    expect(shellRoute?.children?.find(route => route.path === '')?.data?.['navigationNodeId']).toBe('home.dashboard');
    expect(shellRoute?.children?.find(route => route.path === 'accessdenied')?.title).toBe('pageTitle.accessDenied');
    expect(shellRoute?.children?.find(route => route.path === '404')?.title).toBe('pageTitle.notFound');
    expect(shellRoute?.children?.find(route => route.path === 'error')?.title).toBe('pageTitle.error');
  });

  it('keeps admin and entity areas lazy while leaf routes expose navigation metadata', async () => {
    const shellRoute = appRoutes.find(route => route.path === '');
    const entitiesRoute = shellRoute?.children?.find(route => route.path === 'entities');
    const adminRoute = shellRoute?.children?.find(route => route.path === 'admin');
    const usersRoute = adminRoutes.find(route => route.path === 'users');
    const loadedUserRouteModule = await usersRoute?.loadChildren?.();
    const loadedUserRoutes = Array.isArray(loadedUserRouteModule)
      ? loadedUserRouteModule
      : ((loadedUserRouteModule as { default?: Routes } | undefined)?.default ?? undefined);

    expect(typeof entitiesRoute?.loadChildren).toBe('function');
    expect(typeof adminRoute?.loadChildren).toBe('function');
    expect(adminRoute?.data?.['authorities']).toBeUndefined();
    expect(adminRoutes.map(route => route.path)).toEqual(['security', 'users']);
    expect(loadedUserRoutes?.map(route => route.path)).toEqual(['', ':login/view', 'new', ':login/edit']);
    expect(userManagementRoutes[0]?.data?.['navigationNodeId']).toBe('security.users');
    expect(userManagementRoutes[2]?.data?.['navigationNodeId']).toBe('security.users');
    expect(securityRoutes[0]?.data?.['navigationNodeId']).toBe('security.roles');
    expect(securityRoutes[1]?.data?.['navigationNodeId']).toBe('security.roles');
    expect(securityRoutes[2]?.data?.['navigationNodeId']).toBe('security.menu-definitions');
  });

  it('uses translation keys for security route titles', () => {
    expect(securityRoutes.map(route => route.title)).toEqual([
      'pageTitle.securityRoles',
      'pageTitle.permissionMatrix',
      'pageTitle.menuDefinitions',
    ]);
  });
});
