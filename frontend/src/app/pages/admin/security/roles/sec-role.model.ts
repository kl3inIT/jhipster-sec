export interface ISecRole {
  name: string; // PK -- UPPER_SNAKE_CASE, max 50
  displayName?: string; // max 255
  type: string; // RESOURCE in the current backend contract
}
