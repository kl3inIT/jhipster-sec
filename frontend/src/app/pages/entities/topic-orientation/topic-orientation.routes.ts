import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const topicOrientationNavigationData = {
  navigationNodeId: 'entities.topic-orientation',
  sectionId: 'entities',
  breadcrumbKey: 'global.menu.entities.topicOrientation',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/topic-orientation-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      defaultSort: 'code,desc',
      ...topicOrientationNavigationData,
      pageTitleKey: 'angappApp.topicOrientation.home.title',
      deniedMode: 'in-shell',
    },
    title: 'angappApp.topicOrientation.home.title',
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/topic-orientation-detail.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...topicOrientationNavigationData,
      pageTitleKey: 'angappApp.topicOrientation.detail.title',
      deniedMode: 'route',
    },
    title: 'angappApp.topicOrientation.detail.title',
  },
  {
    path: 'new',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: ':id/edit',
    redirectTo: '',
    pathMatch: 'full',
  },
];

export default routes;
