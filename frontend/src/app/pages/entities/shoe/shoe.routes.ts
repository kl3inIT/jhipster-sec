import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const shoeNavigationData = {
  navigationNodeId: 'entities.shoe',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.shoe',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/shoe-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...shoeNavigationData,
      pageTitleKey: 'angappApp.shoe.home.title',
      deniedMode: 'in-shell',
    },
    title: 'angappApp.shoe.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/shoe-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeNavigationData,
      pageTitleKey: 'angappApp.shoe.detail.title',
      deniedMode: 'route',
    },
    title: 'angappApp.shoe.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/shoe-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeNavigationData,
      pageTitleKey: 'angappApp.shoe.home.createLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.shoe.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/shoe-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeNavigationData,
      pageTitleKey: 'angappApp.shoe.home.createOrEditLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.shoe.home.createOrEditLabel',
  },
];

export default routes;
