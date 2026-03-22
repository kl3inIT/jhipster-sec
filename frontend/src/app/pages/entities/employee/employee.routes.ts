import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/employee-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'Employees',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/employee-detail.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'Employee',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'New Employee',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'Edit Employee',
  },
];

export default routes;
