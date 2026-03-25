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
      pageTitleKey: 'layout.menu.entities.employee',
      deniedMode: 'in-shell',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'layout.menu.entities.employee',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/employee-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'layout.menu.entities.employee',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'layout.menu.entities.employee',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'layout.menu.entities.employee',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'layout.menu.entities.employee',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/employee-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...employeeNavigationData,
      pageTitleKey: 'layout.menu.entities.employee',
      deniedMode: 'route',
    },
    resolve: { capability: () => inject(SecuredEntityCapabilityService).getEntityCapability('employee') },
    title: 'layout.menu.entities.employee',
  },
];

export default routes;
