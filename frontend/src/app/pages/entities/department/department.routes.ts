import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const departmentNavigationData = {
  navigationNodeId: 'entities.department',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.department',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/department-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...departmentNavigationData,
      pageTitleKey: 'angappApp.department.home.title',
      deniedMode: 'in-shell',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'angappApp.department.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/department-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...departmentNavigationData,
      pageTitleKey: 'angappApp.department.detail.title',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'angappApp.department.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...departmentNavigationData,
      pageTitleKey: 'angappApp.department.home.createLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'angappApp.department.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/department-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...departmentNavigationData,
      pageTitleKey: 'angappApp.department.home.createOrEditLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('department') },
    title: 'angappApp.department.home.createOrEditLabel',
  },
];

export default routes;
