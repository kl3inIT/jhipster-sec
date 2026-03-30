import { Routes } from '@angular/router';

const routes: Routes = [
  { path: 'brand', loadChildren: () => import('./brand/brand.routes') },
  { path: 'shoe', loadChildren: () => import('./shoe/shoe.routes') },
  { path: 'shoe-variant', loadChildren: () => import('./shoe-variant/shoe-variant.routes') },
  { path: 'organization', loadChildren: () => import('./organization/organization.routes') },
  { path: 'department', loadChildren: () => import('./department/department.routes') },
  { path: 'employee', loadChildren: () => import('./employee/employee.routes') },
];

export default routes;
