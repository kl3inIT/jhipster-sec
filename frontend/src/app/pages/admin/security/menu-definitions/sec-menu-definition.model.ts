export interface ISecMenuDefinition {
  id?: number;
  menuId: string;
  appName: string;
  menuName: string;
  label: string;
  description?: string;
  parentMenuId?: string;
  route?: string;
  icon?: string;
  ordering: number;
}

export interface ISyncResult {
  seeded: number;
  skipped: number;
}

export interface ISyncNode {
  menuId: string;
  appName: string;
  menuName: string;
  label: string;
  parentMenuId?: string;
  route?: string;
  icon?: string;
  ordering: number;
}
