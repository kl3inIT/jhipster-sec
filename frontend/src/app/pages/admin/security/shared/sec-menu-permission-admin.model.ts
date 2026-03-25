export interface ISecMenuPermissionAdmin {
  id?: number;
  role: string;
  appName: string;
  menuId: string;
  effect: string; // 'ALLOW' | 'DENY'
}
