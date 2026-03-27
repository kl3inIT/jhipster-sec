---
phase: quick
plan: 260327-shg
type: execute
wave: 1
depends_on: []
files_modified:
  - frontend/src/app/pages/entities/organization/detail/organization-workbench.model.ts
  - frontend/src/app/pages/entities/organization/service/organization.service.ts
  - frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
  - frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
  - frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts
  - src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java
autonomous: true
requirements: []
must_haves:
  truths:
    - "A user can open one organization screen and inspect departments and employees without leaving the page"
    - "Department and employee create, update, and delete actions run from the organization workbench instead of nested saves on the organization endpoint"
    - "The first render uses the secured organization detail fetch plan so the three-layer graph loads through one root read path"
    - "Workbench actions are gated by organization, department, and employee capabilities before the user can act"
  artifacts:
    - path: "frontend/src/app/pages/entities/organization/detail/organization-workbench.model.ts"
      provides: "Typed Organization -> Department -> Employee graph for the workbench read path"
    - path: "frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts"
      provides: "Workbench state, capability aggregation, and child CRUD orchestration"
    - path: "frontend/src/app/pages/entities/organization/detail/organization-detail.component.html"
      provides: "Jmix-like three-layer organization workbench UI"
    - path: "src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java"
      provides: "Focused secure CRUD coverage for department and employee actions used by the workbench"
  key_links:
    - from: "organization-detail.component.ts"
      to: "organization.service.ts"
      via: "single root load plus post-mutation refresh"
      pattern: "findWorkbench|find\\("
    - from: "organization-detail.component.ts"
      to: "department.service.ts and employee.service.ts"
      via: "inline create/update/delete submit handlers"
      pattern: "departmentService\\.(create|update|delete)|employeeService\\.(create|update|delete)"
    - from: "SecuredEntityEnforcementIT.java"
      to: "OrganizationResource, DepartmentResource, EmployeeResource"
      via: "nested organization read assertions after child CRUD"
      pattern: "/api/organizations|/api/departments|/api/employees"
---

<objective>
Turn the existing organization detail route into one Jmix-like entity workbench that exposes Organization -> Department -> Employee in a single screen.

Purpose: Support create/update/delete validation plus performance/security validation from one authoritative screen without relying on nested to-many saves through the organization endpoint.
Output: Typed nested read contracts, an upgraded organization workbench UI, and focused frontend/backend regression coverage.
</objective>

<execution_context>
@C:/Users/admin/.codex/get-shit-done/workflows/execute-plan.md
@C:/Users/admin/.codex/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/PROJECT.md
@AGENTS.md
@frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
@frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
@frontend/src/app/pages/entities/organization/update/organization-update.component.ts
@frontend/src/app/pages/entities/department/update/department-update.component.ts
@frontend/src/app/pages/entities/employee/update/employee-update.component.ts
@frontend/src/app/pages/entities/organization/service/organization.service.ts
@frontend/src/app/pages/entities/department/service/department.service.ts
@frontend/src/app/pages/entities/employee/service/employee.service.ts
@frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts
@frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts
@src/main/resources/fetch-plans.yml
@src/main/java/com/vn/core/web/rest/OrganizationResource.java
@src/main/java/com/vn/core/web/rest/DepartmentResource.java
@src/main/java/com/vn/core/web/rest/EmployeeResource.java
@src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java

<locked_decisions>
- D-01: Use `Organization` as the root workbench.
- D-02: Expose `Organization -> Department -> Employee` in one flow.
- D-03: Use the existing nested organization detail fetch plan for the read path.
- D-04: Do not save to-many children through the organization endpoint; use department and employee CRUD endpoints from the same screen.
- D-05: The result must feel Jmix-like: referenced records are visible and editable, not reduced to a label/select.
- D-06: Extend the current organization detail route instead of adding a disconnected demo page.
- D-07: Add focused test coverage for capability gating and CRUD orchestration.
</locked_decisions>

<interfaces>
From `organization.service.ts`:
```ts
find(id: number): Observable<HttpResponse<IOrganization>>;
update(organization: IOrganization): Observable<HttpResponse<IOrganization>>;
```

From `department.service.ts`:
```ts
create(department: NewDepartment): Observable<HttpResponse<IDepartment>>;
update(department: IDepartment): Observable<HttpResponse<IDepartment>>;
delete(id: number): Observable<HttpResponse<{}>>;
```

From `employee.service.ts`:
```ts
create(employee: NewEmployee): Observable<HttpResponse<IEmployee>>;
update(employee: IEmployee): Observable<HttpResponse<IEmployee>>;
delete(id: number): Observable<HttpResponse<{}>>;
```

From `secured-entity-capability.service.ts`:
```ts
query(): Observable<ISecuredEntityCapability[]>;
getEntityCapability(code: string): Observable<ISecuredEntityCapability | null>;
```

From `fetch-plans.yml`:
```yaml
organization-detail:
  extends: organization-list
  properties:
    - budget
    - departments:
        - costCenter
        - employees:
            - email
            - salary
```

Current backend boundary already exists and must stay the source of truth:
- `GET /api/organizations/{id}` returns the nested organization detail graph.
- `POST|PUT|PATCH|DELETE /api/departments` handles department mutations.
- `POST|PUT|PATCH|DELETE /api/employees` handles employee mutations.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Define the typed organization workbench read contract on top of the existing secured detail endpoint</name>
  <files>
    frontend/src/app/pages/entities/organization/detail/organization-workbench.model.ts
    frontend/src/app/pages/entities/organization/service/organization.service.ts
  </files>
  <action>
Create a dedicated nested read model for the workbench instead of overloading the flat list/update contracts. In `organization-workbench.model.ts`, define typed interfaces for the exact Organization -> Department -> Employee graph returned by the existing `organization-detail` fetch plan per D-01, D-02, and D-03. Keep it read-oriented: departments include employee collections, and permission-gated scalar fields such as `budget` and `salary` remain optional because they may be absent.

Extend `organization.service.ts` with a workbench-specific read method, for example `findWorkbench(id: number): Observable<HttpResponse<IOrganizationWorkbench>>`, that still calls `GET /api/organizations/{id}`. Do not add new read endpoints, do not add department or employee list calls for initial screen hydration, and do not change the backend fetch-plan source. The point of this task is to make the single-read path explicit and typed so the UI can depend on it without `any` or ad hoc casting.
  </action>
  <verify>
    <automated>npm --prefix frontend run build</automated>
  </verify>
  <done>The frontend has an explicit nested workbench model, and `OrganizationService` exposes one typed read method for the existing secured organization detail endpoint without introducing extra initial-read API chatter.</done>
</task>

<task type="auto">
  <name>Task 2: Replace the flat organization detail page with a single-screen Jmix-like workbench</name>
  <files>
    frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
    frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
  </files>
  <action>
Rework the current detail component into the actual workbench on the same `:id/view` route per D-05 and D-06. The first render must load the root organization once through `findWorkbench(...)` and render nested departments and employees from that response per D-03. Do not issue department or employee queries on initialization; the whole point is a single secured read path for performance and security testing.

In the component state, aggregate capabilities for `organization`, `department`, and `employee` using the existing `SecuredEntityCapabilityService` cache so the screen can gate child actions before rendering per D-07. Preserve the current organization-level field visibility rules and workspace back-navigation behavior.

In the template, build a three-layer Jmix-like flow using PrimeNG-first composition: a root organization summary/edit affordance, nested department sections, and nested employee sections where referenced records are directly visible from the workbench. Add inline create/edit/delete actions for departments and employees from this page, but route none of those actions through organization nested saves. The submit handlers in `organization-detail.component.ts` must call `DepartmentService.create/update/delete` and `EmployeeService.create/update/delete` directly per D-04. After every successful child mutation, reload the root organization through the secured workbench read path so row rules and attribute filtering remain authoritative.

Use the existing update components only as field-behavior references; do not bounce the user away to `/entities/department/...` or `/entities/employee/...` for the core workbench flow. Keep the result functional on both desktop and mobile layouts.
  </action>
  <verify>
    <automated>npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/entities/organization/detail/organization-detail.component.spec.ts && npm --prefix frontend run build</automated>
  </verify>
  <done>The organization detail route now behaves as a three-layer workbench, department and employee records are visible on the same screen, child CRUD runs through their own endpoints, and successful mutations refresh the root organization graph instead of relying on nested organization writes.</done>
</task>

<task type="auto">
  <name>Task 3: Add focused regression coverage for capability gating and workbench CRUD orchestration</name>
  <files>
    frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts
    src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java
  </files>
  <action>
Expand `organization-detail.component.spec.ts` so it proves the workbench behavior rather than the old flat detail card. Cover at least these cases per D-07:
1. Child create/edit/delete affordances stay hidden when the corresponding department or employee capability is denied.
2. The component loads the organization graph through the root workbench read path and renders nested data from that response.
3. After successful department or employee create/update/delete actions, the component triggers a root reload instead of navigating away.

Extend `SecuredEntityEnforcementIT.java` with focused backend coverage that supports the same screen-level workflow. Add permission setup helpers as needed so the test can create/update/delete through the existing department and employee endpoints, then re-read `GET /api/organizations/{id}` and assert the nested organization detail graph reflects the change while denied fields still stay omitted. Keep these tests on `integrationTest` rather than `test`, matching the repository rule captured in `.planning/STATE.md`.

Prefer one department orchestration case and one employee orchestration case over broad test sprawl. Make the sequences explicit and stable:
- Department case: create a new department under the owned organization, verify it appears in the nested organization detail response, then delete it and verify it disappears.
- Employee case: create an employee under the owned department, update an allowed field, verify the nested organization detail response reflects the update, then delete the employee and verify it disappears.
  </action>
  <verify>
    <automated>npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/entities/organization/detail/organization-detail.component.spec.ts && ./gradlew integrationTest --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"</automated>
  </verify>
  <done>The frontend spec proves capability gating plus root-refresh orchestration, and the backend integration suite proves the department/employee CRUD endpoints support the workbench flow while the nested organization detail read remains securely filtered.</done>
</task>

</tasks>

<verification>
1. `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/entities/organization/detail/organization-detail.component.spec.ts` passes.
2. `npm --prefix frontend run build` passes.
3. `./gradlew integrationTest --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"` passes.
4. Opening `/entities/organization/:id/view` shows nested departments and employees from one root load, and child mutations refresh the same workbench screen rather than redirecting to separate entity pages.
</verification>

<success_criteria>
- The existing organization detail route becomes the canonical three-layer workbench.
- The workbench reads Organization -> Department -> Employee from the current nested fetch-plan-backed organization detail endpoint.
- Department and employee CRUD actions are executed from the workbench through their own endpoints, never through nested organization saves.
- Capability gating exists for organization, department, and employee affordances on the same screen.
- Focused frontend and backend regression coverage guards the workbench behavior.
</success_criteria>

<output>
After completion, create `.planning/quick/260327-shg-task-l-m-m-t-m-n-v-i-t-nh-t-3-l-p-v-d-s-/260327-shg-SUMMARY.md`
</output>
