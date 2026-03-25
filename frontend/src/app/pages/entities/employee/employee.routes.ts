import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { SecuredEntityCapabilityService } from '../shared/service/secured-entity-capability.service';

const employeeNavigationData = {
  navigationNodeId: 'entities.employee',
  sectionId: 'entities',
  breadcrumbKey: 'layout.menu.entities.employee',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/employee-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...employeeNavigationData,
      pageTitleKey: 'angappApp.employee.home.title',
      deniedMode: 'in-shell',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'angappApp.employee.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/employee-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'angappApp.employee.detail.title',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'angappApp.employee.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'angappApp.employee.home.createLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'angappApp.employee.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'angappApp.employee.home.createOrEditLabel',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'angappApp.employee.home.createOrEditLabel',
  },
];

export default routes;
