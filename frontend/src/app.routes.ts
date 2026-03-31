import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { activateRoute, registerRoute } from './app/app.routes';

export const appRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./app/pages/login/login.component'),
    title: 'pageTitle.login',
  },
  registerRoute,
  activateRoute,
  {
    path: '',
    loadComponent: () => import('./app/layout/component/main/app.layout').then((m) => m.AppLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./app/pages/home/home.component'),
        // canActivate: [UserRouteAccessService],
        data: {
          navigationNodeId: 'home.dashboard',
          sectionId: 'home',
          breadcrumbKey: 'global.menu.home',
          pageTitleKey: 'pageTitle.home',
          deniedMode: 'route',
        },
        title: 'pageTitle.home',
      },
      
      {
        path: 'accessdenied',
        loadComponent: () => import('./app/pages/error/access-denied.component'),
        title: 'pageTitle.accessDenied',
      },
      {
        path: '404',
        loadComponent: () => import('./app/pages/error/not-found.component'),
        title: 'pageTitle.notFound',
      },
      {
        path: 'error',
        loadComponent: () => import('./app/pages/error/error.component'),
        title: 'pageTitle.error',
      },
      {
        path: 'entities',
        loadChildren: () => import('./app/pages/entities/entity.routes'),
        // canActivate: [UserRouteAccessService],
      },
      {
        path: 'entities/movie',
        loadChildren: () => import('./app/pages/entities/movie/movie.routes'),
        // canActivate: [UserRouteAccessService],
      },

      {
        path: 'entities/topic',
        redirectTo: 'entities/topic-orientation',
        pathMatch: 'full',
      },
  
      {
        path: 'admin',
        loadChildren: () => import('./app/pages/admin/admin.routes'),
        // canActivate: [UserRouteAccessService],
      },
    ],
  },
  { path: '**', redirectTo: '404' },
];
