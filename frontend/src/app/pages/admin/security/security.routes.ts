import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const securityRouteData = {
  sectionId: 'security',
  deniedMode: 'route' as const,
};

const routes: Routes = [
  {
    path: 'roles',
    loadComponent: () => import('./roles/list/role-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...securityRouteData,
      navigationNodeId: 'security.roles',
      breadcrumbKey: 'layout.menu.security.roles',
      pageTitleKey: 'pageTitle.securityRoles',
    },
    title: 'pageTitle.securityRoles',
  },
  {
    path: 'roles/:name/permissions',
    loadComponent: () => import('./permission-matrix/permission-matrix.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...securityRouteData,
      navigationNodeId: 'security.roles',
      breadcrumbKey: 'layout.menu.security.roles',
      pageTitleKey: 'pageTitle.permissionMatrix',
    },
    title: 'pageTitle.permissionMatrix',
  },
  {
    path: 'row-policies',
    loadComponent: () => import('./row-policies/list/row-policy-list.component'),
    canActivate: [UserRouteAccessService],
    data: {
      ...securityRouteData,
      navigationNodeId: 'security.row-policies',
      breadcrumbKey: 'layout.menu.security.rowPolicies',
      pageTitleKey: 'pageTitle.rowPolicies',
    },
    title: 'pageTitle.rowPolicies',
  },
];

export default routes;
