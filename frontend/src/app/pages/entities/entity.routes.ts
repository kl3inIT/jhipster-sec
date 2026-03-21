import { Routes } from '@angular/router';

const routes: Routes = [
  { path: 'organization', loadChildren: () => import('./organization/organization.routes') },
  { path: 'department', loadChildren: () => import('./department/department.routes') },
  { path: 'employee', loadChildren: () => import('./employee/employee.routes') },
];

export default routes;
