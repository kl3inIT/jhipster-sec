import { IDepartment } from '../department/department.model';

export interface IEmployee {
  id?: number;
  employeeNumber?: string;
  firstName?: string;
  lastName?: string;
  department?: Pick<IDepartment, 'id' | 'name'>;
  email?: string;
  salary?: number; // permission-gated -- absent when denied
}

export type NewEmployee = Omit<IEmployee, 'id'> & { id: null };
