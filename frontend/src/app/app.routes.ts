import { Route } from '@angular/router';

export const registerRoute: Route = {
  path: 'register',
  loadComponent: () => import('./pages/register/register.component'),
  title: 'register.title',
};

export const activateRoute: Route = {
  path: 'activate',
  loadComponent: () => import('./pages/activate/activate.component'),
  title: 'activate.title',
};

export { appRoutes as routes } from '../app.routes';
