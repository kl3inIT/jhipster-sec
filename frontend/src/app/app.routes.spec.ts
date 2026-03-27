import { Routes } from '@angular/router';

import adminRoutes from './pages/admin/admin.routes';
import securityRoutes from './pages/admin/security/security.routes';
import userManagementRoutes from './pages/admin/user-management/user-management.routes';
import { routes } from './app.routes';

describe('app routes shim', () => {
  it('exposes the shell routes with translated titles and home navigation metadata', () => {
    const shellRoute = routes.find(route => route.path === '');
    const childPaths = shellRoute?.children?.map(route => route.path) ?? [];
    const homeRoute = shellRoute?.children?.find(route => route.path === '');

    expect(routes.some(route => route.path === 'login' && route.title === 'pageTitle.login')).toBe(true);
    expect(childPaths).toEqual(['', 'accessdenied', '404', 'error', 'entities', 'admin']);
    expect(homeRoute?.title).toBe('pageTitle.home');
    expect(homeRoute?.data?.['navigationNodeId']).toBe('home.dashboard');
    expect(homeRoute?.data?.['sectionId']).toBe('home');
    expect(homeRoute?.data?.['breadcrumbKey']).toBe('global.menu.home');
  });

  it('keeps admin and entity areas lazy while route leaves expose stable navigation node ids', async () => {
    const shellRoute = routes.find(route => route.path === '');
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
    expect(userManagementRoutes.every(route => route.data?.['navigationNodeId'] === 'security.users')).toBe(true);
    expect(securityRoutes[0]?.data?.['navigationNodeId']).toBe('security.roles');
    expect(securityRoutes[1]?.data?.['navigationNodeId']).toBe('security.roles');
    expect(securityRoutes[2]?.data?.['navigationNodeId']).toBe('security.menu-definitions');
  });

  it('uses translation keys for shell error and security route titles', () => {
    const shellRoute = routes.find(route => route.path === '');

    expect(shellRoute?.children?.find(route => route.path === 'accessdenied')?.title).toBe('pageTitle.accessDenied');
    expect(shellRoute?.children?.find(route => route.path === '404')?.title).toBe('pageTitle.notFound');
    expect(shellRoute?.children?.find(route => route.path === 'error')?.title).toBe('pageTitle.error');
    expect(securityRoutes.map(route => route.title)).toEqual([
      'pageTitle.securityRoles',
      'pageTitle.permissionMatrix',
      'pageTitle.menuDefinitions',
    ]);
  });
});
