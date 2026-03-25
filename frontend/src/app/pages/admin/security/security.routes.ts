import { Routes } from '@angular/router';

const routes: Routes = [
  { path: 'roles', loadComponent: () => import('./roles/list/role-list.component'), title: 'pageTitle.securityRoles' },
  {
    path: 'roles/:name/permissions',
    loadComponent: () => import('./permission-matrix/permission-matrix.component'),
    title: 'pageTitle.permissionMatrix',
  },
  { path: 'row-policies', loadComponent: () => import('./row-policies/list/row-policy-list.component'), title: 'pageTitle.rowPolicies' },
];

export default routes;
