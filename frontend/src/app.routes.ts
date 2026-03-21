import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

export const appRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./app/pages/login/login.component'),
  },
  {
    path: '',
    loadComponent: () => import('./app/layout/component/main/app.layout').then(m => m.AppLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./app/pages/home/home.component'),
        canActivate: [UserRouteAccessService],
      },
      {
        path: 'accessdenied',
        loadComponent: () => import('./app/pages/error/access-denied.component'),
      },
      {
        path: '404',
        loadComponent: () => import('./app/pages/error/not-found.component'),
      },
      {
        path: 'error',
        loadComponent: () => import('./app/pages/error/error.component'),
      },
      {
        path: 'entities',
        loadChildren: () => import('./app/pages/entities/entity.routes'),
        canActivate: [UserRouteAccessService],
      },
      {
        path: 'admin/security',
        loadChildren: () => import('./app/pages/admin/security/security.routes'),
        canActivate: [UserRouteAccessService],
        data: { authorities: ['ROLE_ADMIN'] },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
