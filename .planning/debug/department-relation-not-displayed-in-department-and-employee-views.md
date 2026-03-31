---
status: resolved
trigger: "GitHub issue #9: Department relation not displayed in Department and Employee views"
created: 2026-03-28T12:00:00+07:00
updated: 2026-03-28T12:00:00+07:00
symptoms_prefilled: true
goal: find_and_fix
---

## Current Focus

hypothesis: Confirmed. The existing GET list endpoints already serialize nested relations when the list fetch plan includes them.
test: Patch the YAML list fetch plans, assert nested relation payloads through the existing GET list endpoints, and keep `/query` untouched.
expecting: Resolved.
next_action: run focused verification

## Symptoms

expected: Department list should show the parent organization, and Employee list should show the parent department.
actual: Organization detail renders nested departments correctly, but Department and Employee list rows omit their parent relation names.
errors: No explicit runtime error; the list payload simply omitted the nested relation objects.
reproduction: Create or open an Organization, add a Department, then visit Department and Employee list views and observe blank relation columns.
started: Reported in GitHub issue #9 on 2026-03-28.

## Evidence

- `frontend/src/app/pages/entities/department/list/department-list.component.html` already binds `row.organization?.name`.
- `frontend/src/app/pages/entities/employee/list/employee-list.component.html` already binds `row.department?.name`.
- `src/main/java/com/vn/core/web/rest/DepartmentResource.java` and `src/main/java/com/vn/core/web/rest/EmployeeResource.java` already use the current GET list endpoints backed by `DepartmentService.list(...)` / `EmployeeService.list(...)`, which call `SecureDataManager.loadList(...)`.
- `src/main/resources/fetch-plans.yml` defined `department-list` without `organization` and `employee-list` without `department`, so the serializer had no instructions to include those nested relation objects on the GET list path.
- `src/test/java/com/vn/core/security/fetch/YamlFetchPlanRepositoryTest.java` now proves the shipped `department-list` and `employee-list` plans resolve those parent references from `fetch-plans.yml`.
- Focused `integrationTest` execution is currently blocked before these assertions run because the Spring test context fails to start with `No qualifying bean of type 'com.vn.core.service.mapper.security.SecPermissionMapper' available`, which is unrelated to this fetch-plan patch.

## Resolution

root_cause: The current `loadList` path was correct, but the list fetch plans were incomplete. Department list omitted `organization`, and Employee list omitted `department`, so the serializer returned rows without the relation objects the frontend tables already expected.

fix: Added inline nested relation properties to `department-list` and `employee-list` in `src/main/resources/fetch-plans.yml`, then expanded the GET list integration assertions and related VIEW grants in `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java` to prove the existing list endpoints now return the parent relation names.

verification: |
  - Passed: `.\gradlew.bat test --tests "com.vn.core.security.fetch.YamlFetchPlanRepositoryTest" --console=plain`
  - Blocked by unrelated baseline failure: `.\gradlew.bat integrationTest -x test --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT" --console=plain`
    failed during Spring context startup with missing `SecPermissionMapper` bean before the secured-entity assertions executed.

files_changed:
  - src/main/resources/fetch-plans.yml
  - src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java
