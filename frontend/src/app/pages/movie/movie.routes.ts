import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const movieNavigationData = {
  navigationNodeId: 'entities.movie',
  sectionId: 'entities',
  breadcrumbKey: 'layout.menu.entities.movie',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/movie-list/movie-profile-list.component').then(m => m.default),
    canActivate: [UserRouteAccessService],
    data: {
      ...movieNavigationData,
      pageTitleKey: 'layout.menu.entities.movie',
      deniedMode: 'in-shell',
    },
    title: 'layout.menu.entities.movie',
  },
  {
    path: 'new',
    loadComponent: () => import('./components/movie-create/movie-profile-create.component').then(m => m.default),
    canActivate: [UserRouteAccessService],
    data: {
      ...movieNavigationData,
      pageTitleKey: 'layout.menu.entities.movie',
      deniedMode: 'route',
    },
    title: 'layout.menu.entities.movie',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./components/movie-details/movie-profile-detail-page.component').then(m => m.default),
    canActivate: [UserRouteAccessService],
    data: {
      ...movieNavigationData,
      pageTitleKey: 'layout.menu.entities.movie',
      deniedMode: 'route',
    },
    title: 'layout.menu.entities.movie',
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./components/movie-edit/movie-profile-edit-page.component').then(m => m.default),
    canActivate: [UserRouteAccessService],
    data: {
      ...movieNavigationData,
      pageTitleKey: 'layout.menu.entities.movie',
      deniedMode: 'route',
    },
    title: 'layout.menu.entities.movie',
  },
];

export default routes;
