import { Route } from '@angular/router';

export const registerRoute: Route = {
  path: 'register',
  loadComponent: () => import('./pages/register/register.component'),
  title: 'register.title',
};

export { appRoutes as routes } from '../app.routes';
