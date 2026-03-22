import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/department-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'Departments',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/department-detail.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'Department',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'New Department',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'Edit Department',
  },
];

export default routes;
