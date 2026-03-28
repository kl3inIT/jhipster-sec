export interface IOrganizationWorkbenchEmployee {
  id?: number;
  employeeNumber?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  salary?: number;
  department?: {
    id?: number;
    code?: string;
    name?: string;
  };
}

export interface IOrganizationWorkbenchDepartment {
  id?: number;
  code?: string;
  name?: string;
  costCenter?: string;
  employees?: IOrganizationWorkbenchEmployee[];
}

export interface IOrganizationWorkbench {
  id?: number;
  code?: string;
  name?: string;
  ownerLogin?: string;
  budget?: number;
  departments?: IOrganizationWorkbenchDepartment[];
}
