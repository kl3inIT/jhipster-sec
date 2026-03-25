import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const organizationNavigationData = {
  navigationNodeId: 'entities.organization',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.organization',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/organization-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...organizationNavigationData,
      pageTitleKey: 'angappApp.organization.home.title',
      deniedMode: 'in-shell',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'angappApp.organization.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/organization-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...organizationNavigationData,
      pageTitleKey: 'angappApp.organization.detail.title',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'angappApp.organization.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...organizationNavigationData,
      pageTitleKey: 'angappApp.organization.home.createLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'angappApp.organization.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/organization-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...organizationNavigationData,
      pageTitleKey: 'angappApp.organization.home.createOrEditLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('organization') },
    title: 'angappApp.organization.home.createOrEditLabel',
  },
];

export default routes;
