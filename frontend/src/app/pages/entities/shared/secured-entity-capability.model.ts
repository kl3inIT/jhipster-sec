export interface ISecuredAttributeCapability {
  name: string;
  canView: boolean;
  canEdit: boolean;
}

export interface ISecuredEntityCapability {
  code: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  attributes: ISecuredAttributeCapability[];
}
