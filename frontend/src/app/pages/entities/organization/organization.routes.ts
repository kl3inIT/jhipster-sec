import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/organization-list.component'),
    canActivate: [UserRouteAccessService],
    data: { defaultSort: 'id,asc' },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'Organizations',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/organization-detail.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'Organization',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'New Organization',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'Edit Organization',
  },
];

export default routes;
