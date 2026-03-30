import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const brandNavigationData = {
  navigationNodeId: 'entities.brand',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.brand',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/brand-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...brandNavigationData,
      pageTitleKey: 'angappApp.brand.home.title',
      deniedMode: 'in-shell',
    },
    title: 'angappApp.brand.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/brand-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...brandNavigationData,
      pageTitleKey: 'angappApp.brand.detail.title',
      deniedMode: 'route',
    },
    title: 'angappApp.brand.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/brand-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...brandNavigationData,
      pageTitleKey: 'angappApp.brand.home.createLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.brand.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/brand-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...brandNavigationData,
      pageTitleKey: 'angappApp.brand.home.createOrEditLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.brand.home.createOrEditLabel',
  },
];

export default routes;
