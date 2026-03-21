import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/organization-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    title: 'Organizations',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/organization-detail.component'),
    canActivate: [UserRouteAccessService],
    title: 'Organization',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'New Organization',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    title: 'Edit Organization',
  },
];

export default routes;
