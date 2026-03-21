import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/department-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    title: 'Departments',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/department-detail.component'),
    canActivate: [UserRouteAccessService],
    title: 'Department',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'New Department',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'Edit Department',
  },
];

export default routes;
