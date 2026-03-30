export interface IBrand {
  id?: number;
  name?: string;
  description?: string;
}

export type NewBrand = Omit<IBrand, 'id'> & { id: null };
