import { inject } from '@angular/core';
import { ResolveFn, Routes } from '@angular/router';
import { of } from 'rxjs';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

export const userManagementResolve: ResolveFn<IUser | null> = route => {
  const login = route.paramMap.get('login');
  if (login) {
    return inject(UserManagementService).find(login);
  }
  return of(null);
};

const userManagementRouteData = {
  navigationNodeId: 'security.users',
  sectionId: 'security',
  breadcrumbKey: 'global.menu.admin.userManagement',
  deniedMode: 'route' as const,
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./user-management-placeholder.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...userManagementRouteData,
      pageTitleKey: 'userManagement.home.title',
    },
    title: 'userManagement.home.title',
  },
  {
    path: ':login/view',
    loadComponent: () => import('./user-management-placeholder.component'),
    canActivate: [UserRouteAccessService],
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      ...userManagementRouteData,
      pageTitleKey: 'userManagement.detail.title',
    },
    title: 'userManagement.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./user-management-placeholder.component'),
    canActivate: [UserRouteAccessService],
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      ...userManagementRouteData,
      pageTitleKey: 'userManagement.home.createLabel',
    },
    title: 'userManagement.home.createLabel',
  },
  {
    path: ':login/edit',
    loadComponent: () => import('./user-management-placeholder.component'),
    canActivate: [UserRouteAccessService],
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      ...userManagementRouteData,
      pageTitleKey: 'userManagement.home.createOrEditLabel',
    },
    title: 'userManagement.home.createOrEditLabel',
  },
];

export default routes;
