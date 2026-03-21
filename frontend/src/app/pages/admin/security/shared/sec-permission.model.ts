export interface ISecPermission {
  id?: number;
  authorityName: string;
  targetType: string;
  target: string;
  action: string;
  effect: string;
}
