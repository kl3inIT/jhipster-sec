export interface ISecRowPolicy {
  id?: number;
  code: string; // max 100
  entityName: string; // max 255 -- entity code from catalog
  operation: string; // "READ" | "UPDATE" | "DELETE"
  policyType: string; // "SPECIFICATION" | "JPQL"
  expression: string; // max 1000
}
