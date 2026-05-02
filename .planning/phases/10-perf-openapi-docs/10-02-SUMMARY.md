---
phase: 10-performance-benchmarking-and-openapi-documentation-for-the-security-pipeline-jmeter-k6-load-tests-comparing-secured-vs-standard-endpoints-and-swagger-annotations-for-variable-response-schemas-and-fetch-plan-params
plan: 02
subsystem: api
tags: [openapi, springdoc, swagger, secured-entity]
requires:
  - phase: 09-enterprise-ux-and-performance-hardening
    provides: secured entity CRUD resources and security metadata endpoints to annotate
provides:
  - OpenAPI annotations for secured Organization, Department, and Employee CRUD endpoints
  - OpenAPI tag and operation metadata for current-user security metadata endpoints
  - OperationCustomizer extension that marks secured entity operations with x-secured-entity: true
affects: [api-docs, swagger-ui, integration-consumers]
tech-stack:
  added: []
  patterns: [OperationCustomizer-based extension injection, schema overrides for ResponseEntity<String> JSON bodies]
key-files:
  created:
    - src/main/java/com/vn/core/config/SecuredEntityOperationCustomizer.java
  modified:
    - src/main/java/com/vn/core/web/rest/OrganizationResource.java
    - src/main/java/com/vn/core/web/rest/DepartmentResource.java
    - src/main/java/com/vn/core/web/rest/EmployeeResource.java
    - src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java
    - src/main/java/com/vn/core/web/rest/MenuPermissionResource.java
key-decisions:
  - "Use OperationCustomizer instead of @Extension annotations to emit boolean x-secured-entity: true values"
  - "Document secured ResponseEntity<String> payloads with explicit object/array schema types and permission-filtering notes"
patterns-established:
  - "Apply @Tag at resource class level and @Operation + @ApiResponses on every endpoint method"
  - "Reference fetch-plan codes only in @Operation descriptions without changing endpoint contracts"
requirements-completed: [OPENAPI-01]
duration: 12 min
completed: 2026-03-31
---

# Phase 10 Plan 02: OpenAPI Secured Endpoint Annotation Summary

**Secured entity and security metadata endpoints now expose explicit OpenAPI contract metadata, including fetch-plan notes and machine-readable x-secured-entity markers for secured controllers.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-31T03:14:12Z
- **Completed:** 2026-03-31T03:26:56Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added a dedicated `SecuredEntityOperationCustomizer` to automatically inject `x-secured-entity: true` for all Organization, Department, and Employee operations.
- Annotated all 21 secured entity endpoints (7 per resource) with `@Operation`, `@ApiResponses`, and schema overrides matching array/object JSON payload behavior.
- Annotated `SecuredEntityCapabilityResource` and `MenuPermissionResource` with `@Tag` and endpoint-level operation metadata, including parameter documentation for `appName`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SecuredEntityOperationCustomizer and annotate OrganizationResource** - `fbc00f4` (feat)
2. **Task 2: Annotate DepartmentResource, EmployeeResource, and security endpoints** - `01ea6f0` (feat)

## Files Created/Modified

- `src/main/java/com/vn/core/config/SecuredEntityOperationCustomizer.java` - Adds `x-secured-entity: true` to secured entity controller operations.
- `src/main/java/com/vn/core/web/rest/OrganizationResource.java` - Adds full OpenAPI tags, operations, responses, and request-body documentation for all secured Organization endpoints.
- `src/main/java/com/vn/core/web/rest/DepartmentResource.java` - Adds full OpenAPI tags, operations, responses, and request-body documentation for all secured Department endpoints.
- `src/main/java/com/vn/core/web/rest/EmployeeResource.java` - Adds full OpenAPI tags, operations, responses, and request-body documentation for all secured Employee endpoints.
- `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java` - Adds security endpoint OpenAPI tag, operation, and responses.
- `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java` - Adds security endpoint OpenAPI tag, operation, responses, and `appName` parameter documentation.

## Decisions Made

- Used `OperationCustomizer` with `operation.addExtension("x-secured-entity", true)` so generated OpenAPI extension values are boolean, not string literals.
- Kept bearer auth documentation global (no `@SecurityRequirement` on methods), aligned with existing JHipster springdoc configuration.

## Deviations from Plan

None - plan executed exactly as written.

## Auth Gates

None.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- OpenAPI documentation coverage for in-scope secured and security endpoints is in place and ready for api-docs profile validation against `/v3/api-docs`.
- `x-secured-entity` extension tagging is centralized and reusable for future secured entity resources.

## Self-Check: PASSED

- FOUND: `.planning/phases/10-performance-benchmarking-and-openapi-documentation-for-the-security-pipeline-jmeter-k6-load-tests-comparing-secured-vs-standard-endpoints-and-swagger-annotations-for-variable-response-schemas-and-fetch-plan-params/10-02-SUMMARY.md`
- FOUND: `fbc00f4`
- FOUND: `01ea6f0`
