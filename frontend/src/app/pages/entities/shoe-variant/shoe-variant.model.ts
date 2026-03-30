import { IShoe } from '../shoe/shoe.model';

export interface IShoeVariant {
  id?: number;
  name?: string;
  decription?: string;
  shoe?: Pick<IShoe, 'id' | 'name'>;
}

export type NewShoeVariant = Omit<IShoeVariant, 'id'> & { id: null };
