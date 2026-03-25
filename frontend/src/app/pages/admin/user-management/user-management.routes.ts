import { inject } from '@angular/core';
import { ResolveFn, Routes } from '@angular/router';
import { of } from 'rxjs';

import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

export const userManagementResolve: ResolveFn<IUser | null> = route => {
  const login = route.paramMap.get('login');
  if (login) {
    return inject(UserManagementService).find(login);
  }
  return of(null);
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./user-management-placeholder.component'),
    data: {
      defaultSort: 'id,asc',
      title: 'userManagement.home.title',
    },
  },
  {
    path: ':login/view',
    loadComponent: () => import('./user-management-placeholder.component'),
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      title: 'userManagement.home.title',
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./user-management-placeholder.component'),
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      title: 'userManagement.home.title',
    },
  },
  {
    path: ':login/edit',
    loadComponent: () => import('./user-management-placeholder.component'),
    resolve: {
      user: userManagementResolve,
    },
    data: {
      defaultSort: 'id,asc',
      title: 'userManagement.home.title',
    },
  },
];

export default routes;
