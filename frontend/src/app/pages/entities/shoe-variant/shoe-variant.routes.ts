import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const shoeVariantNavigationData = {
  navigationNodeId: 'entities.shoe-variant',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.shoeVariant',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/shoe-variant-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'id,asc',
      ...shoeVariantNavigationData,
      pageTitleKey: 'angappApp.shoeVariant.home.title',
      deniedMode: 'in-shell',
    },
    title: 'angappApp.shoeVariant.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/shoe-variant-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeVariantNavigationData,
      pageTitleKey: 'angappApp.shoeVariant.detail.title',
      deniedMode: 'route',
    },
    title: 'angappApp.shoeVariant.detail.title',
  },
  {
    path: 'new',
    loadComponent: () => import('./update/shoe-variant-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeVariantNavigationData,
      pageTitleKey: 'angappApp.shoeVariant.home.createLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.shoeVariant.home.createLabel',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/shoe-variant-update.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...shoeVariantNavigationData,
      pageTitleKey: 'angappApp.shoeVariant.home.createOrEditLabel',
      deniedMode: 'route',
    },
    title: 'angappApp.shoeVariant.home.createOrEditLabel',
  },
];

export default routes;
