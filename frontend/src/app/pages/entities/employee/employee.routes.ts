import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/employee-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    title: 'Employees',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/employee-detail.component'),
    canActivate: [UserRouteAccessService],
    title: 'Employee',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'New Employee',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'Edit Employee',
  },
];

export default routes;
