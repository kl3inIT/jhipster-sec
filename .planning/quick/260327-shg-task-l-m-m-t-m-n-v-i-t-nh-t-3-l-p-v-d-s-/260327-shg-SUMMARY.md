# Quick Task Summary

- Quick ID: `260327-shg`
- Task: Làm một màn Jmix-style tối thiểu 3 lớp để sửa một record và chỉnh được cả record reference, phục vụ test create/update/delete, performance, và security.

## Outcome

- Replaced the organization detail page with a single workbench-style screen that exposes a 3-layer graph: `Organization -> Department -> Employee`.
- Added inline create, update, delete flows for departments and employees so one root screen can exercise nested reference editing instead of only flat name edits.
- Preserved secure capability gating in the UI so action visibility still follows backend entity and attribute permissions.
- Kept the backend raw-JSON contract and added reference adaptation for `{ organization: { id } }` and `{ department: { id } }` payloads before secure save.

## Implementation Notes

- Frontend:
  - Added `organization-workbench.model.ts` and a new `findWorkbench(...)` service path for nested organization detail reads.
  - Rebuilt `organization-detail.component.*` into a PrimeNG Sakai-aligned workbench with organization, department, and employee editing dialogs plus reload-after-mutation behavior.
  - Extended translations and regenerated merged i18n bundles plus the hash artifact.
- Backend:
  - Updated `DepartmentService` and `EmployeeService` to normalize reference payloads into accessible managed entities before calling `SecureDataManager.save(...)`.
  - Expanded `SecuredEntityEnforcementIT` to prove nested create/update/delete flows, and made the two CRUD-flow tests run outside the class transaction so delete is validated through real request transactions instead of the enclosing test transaction.
  - Seeded dedicated test authorities on demand for those non-transactional CRUD-flow tests and granted explicit entity-level `READ` permissions for the organization graph.

## Verification

- `npm --prefix frontend run test -- --watch=false --include src/app/pages/entities/organization/detail/organization-detail.component.spec.ts`
- `npm --prefix frontend run build`
- `JAVA_HOME=C:\\Users\\admin\\.jdks\\temurin-25.0.2 .\\gradlew.bat integrationTest --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"`

## Residual Notes

- Frontend production build still passes with the existing initial bundle budget warning: `899.93 kB` vs configured `500.00 kB`.
