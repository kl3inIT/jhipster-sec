export interface IOrganization {
  id?: number;
  code?: string;
  name?: string;
  ownerLogin?: string;
  budget?: number; // permission-gated -- absent when denied
}

export type NewOrganization = Omit<IOrganization, 'id'> & { id: null };
