---
phase: 05-standalone-frontend-delivery
plan: 07
subsystem: frontend-entity-capabilities
tags: [spring, angular, permissions, capabilities, secured-entities]
dependency_graph:
  requires: [05-02, 05-03]
  provides: [secured-entity-capability-endpoint, secured-entity-capability-client]
  affects: [05-08, 05-09]
tech_stack:
  added: []
  patterns:
    - secured-entity catalog projection
    - metamodel-driven attribute enumeration
    - authenticated capability endpoint
    - shareReplay-backed frontend caching
key_files:
  created:
    - src/test/java/com/vn/core/web/rest/SecuredEntityCapabilityResourceIT.java
    - src/main/java/com/vn/core/service/dto/security/SecuredAttributeCapabilityDTO.java
    - src/main/java/com/vn/core/service/dto/security/SecuredEntityCapabilityDTO.java
    - src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java
    - src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java
    - frontend/src/app/pages/entities/shared/secured-entity-capability.model.ts
    - frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts
    - frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.spec.ts
  modified: []
decisions:
  - "Capability payloads derive from SecuredEntityCatalog plus JPA metamodel enumeration so frontend gating stays aligned with the backend security allowlist."
  - "Frontend entity screens will share one cached capability request via shareReplay(1) instead of refetching per feature component."
requirements-completed: [ENT-03]
metrics:
  duration: resumed
  completed: 2026-03-22
  tasks_completed: 2
  files_created: 8
---

# Phase 5 Plan 07: Shared Capability Contract Summary

Added the shared capability contract for protected-entity screens: an authenticated backend capability endpoint and a cached frontend client that exposes organization, department, and employee permissions in one place.

## What Was Built

### Backend Capability Endpoint

- Added `SecuredEntityCapabilityResource` at `/api/security/entity-capabilities` behind `isAuthenticated()`.
- Added DTOs and `SecuredEntityCapabilityService` to project current-user CRUD and attribute permissions from `SecuredEntityCatalog`, `EntityPermissionEvaluator`, `AttributePermissionEvaluator`, and the JPA metamodel.
- Added integration coverage proving the response changes for `ROLE_PROOF_READER`, `ROLE_PROOF_EDITOR`, and `ROLE_PROOF_NONE`.

### Frontend Shared Capability Client

- Added typed frontend models for entity and attribute capabilities.
- Added `SecuredEntityCapabilityService` that queries `api/security/entity-capabilities`, resolves one entity by code, and caches the shared response with `shareReplay(1)`.
- Added a Vitest spec proving `getEntityCapability('organization')` resolution and one-request caching behavior.

## Task Commits

1. **Task 1 RED: Add failing backend capability endpoint coverage** - `fd90728` (`test(05-07)`)
2. **Task 1 GREEN: Add secured entity capability endpoint** - `f69649a` (`feat(05-07)`)
3. **Task 2: Add frontend capability client** - `22e43ba` (`feat(05-07)`)

## Verification

- `cmd /c npx.cmd ng test --watch=false`
- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 GRADLE_USER_HOME=D:\jhipster\.gradle-home .\gradlew integrationTest --tests "com.vn.core.web.rest.SecuredEntityCapabilityResourceIT"`

## Self-Check: PASSED

The backend now exposes current-user capability data for secured entities, the frontend has a shared cached client ready for list/detail/update gating, and both targeted backend/frontend checks pass.
