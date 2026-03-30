import { IBrand } from '../brand/brand.model';

export interface IShoe {
  id?: number;
  name?: string;
  decription?: string;
  brand?: Pick<IBrand, 'id' | 'name'>;
}

export type NewShoe = Omit<IShoe, 'id'> & { id: null };
