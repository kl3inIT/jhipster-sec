import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'security',
    loadChildren: () => import('./security/security.routes'),
  },
  {
    path: 'users',
    loadChildren: () => import('./user-management/user-management.routes'),
  },
];

export default routes;
