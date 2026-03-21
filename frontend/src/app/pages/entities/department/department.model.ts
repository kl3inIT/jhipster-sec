import { IOrganization } from '../organization/organization.model';

export interface IDepartment {
  id?: number;
  code?: string;
  name?: string;
  organization?: Pick<IOrganization, 'id' | 'name'>;
  costCenter?: string;
}

export type NewDepartment = Omit<IDepartment, 'id'> & { id: null };
