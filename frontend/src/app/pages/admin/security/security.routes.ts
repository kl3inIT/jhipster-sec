import { Routes } from '@angular/router';

const routes: Routes = [
  { path: 'roles', loadComponent: () => import('./roles/list/role-list.component'), title: 'Security Roles' },
  {
    path: 'roles/:name/permissions',
    loadComponent: () => import('./permission-matrix/permission-matrix.component'),
    title: 'Permission Matrix',
  },
  { path: 'row-policies', loadComponent: () => import('./row-policies/list/row-policy-list.component'), title: 'Row Policies' },
];

export default routes;
