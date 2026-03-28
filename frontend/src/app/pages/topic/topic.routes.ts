import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const topicNavigationData = {
  navigationNodeId: 'entities.topic',
  sectionId: 'entities',
  breadcrumbKey: 'layout.menu.entities.topic',
};

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/topic-list/topic-list.component').then(m => m.TopicListComponent),
    canActivate: [UserRouteAccessService],
    data: {
      ...topicNavigationData,
      pageTitleKey: 'layout.menu.entities.topic',
      deniedMode: 'in-shell',
    },
    title: 'layout.menu.entities.topic',
  },

//   {
//     path: 'create',
//     loadComponent: () =>
//       import('./components/topic-create/topic-create.component').then(m => m.TopicCreateComponent),
//     canActivate: [UserRouteAccessService],
//     data: {
//       ...topicNavigationData,
//       pageTitleKey: 'layout.menu.entities.topic',
//       deniedMode: 'route',
//     },
//     title: 'layout.menu.entities.topic',
//   },

//   {
//     path: ':id/view',
//     loadComponent: () =>
//       import('./components/topic-details/topic-details.component').then(m => m.TopicDetailsComponent),
//     canActivate: [UserRouteAccessService],
//     data: {
//       ...topicNavigationData,
//       pageTitleKey: 'layout.menu.entities.topic',
//       deniedMode: 'route',
//     },
//     title: 'layout.menu.entities.topic',
//   },

//   {
//     path: ':id/edit',
//     loadComponent: () =>
//       import('./components/topic-edit/topic-edit.component').then(m => m.TopicEditComponent),
//     canActivate: [UserRouteAccessService],
//     data: {
//       ...topicNavigationData,
//       pageTitleKey: 'layout.menu.entities.topic',
//       deniedMode: 'route',
//     },
//     title: 'layout.menu.entities.topic',
//   },
];

export default routes;