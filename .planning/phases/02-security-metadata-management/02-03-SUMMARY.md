---
phase: 02-security-metadata-management
plan: 03
subsystem: web
tags: [rest, crud, spring-mvc, spring-security, liquibase, seed-data, admin-api]

# Dependency graph
requires:
  - phase: 02-security-metadata-management/01
    provides: SecPermission and SecRowPolicy JPA entities, AuthorityRepository, SecPermissionRepository, SecRowPolicyRepository
  - phase: 02-security-metadata-management/02
    provides: SecRoleDTO, SecPermissionDTO, SecRowPolicyDTO, SecPermissionMapper, SecRowPolicyMapper

provides:
  - SecRoleAdminResource (CRUD at /api/admin/sec/roles) with conflict detection and cascade permission delete
  - SecPermissionAdminResource (CRUD at /api/admin/sec/permissions) with FK validation to jhi_authority
  - SecRowPolicyAdminResource (CRUD at /api/admin/sec/row-policies) with code uniqueness check
  - Seed roles: ROLE_ACCOUNTANT, ROLE_STOCKKEEPER, ROLE_DIRECTOR (context=dev,test)

affects:
  - 02-04 (integration tests will exercise these controllers)
  - 03 (Phase 3 security engine reads permissions and row policies via these admin APIs)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@PreAuthorize class-level: all three controllers apply ROLE_ADMIN gate at class level, covering all methods"
    - "Cascade delete via application code: SecRoleAdminResource calls secPermissionRepository.deleteByAuthorityName before deleteById in @Transactional to provide audit consistency"
    - "FK validation in controller: SecPermissionAdminResource validates authorityName existence before saving"
    - "Code uniqueness guard: SecRowPolicyAdminResource checks existing code on POST and only on code-change on PUT"

key-files:
  created:
    - src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java
    - src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java
    - src/main/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResource.java
    - src/main/resources/config/liquibase/changelog/20260321000400_seed_security_roles.xml
  modified:
    - src/main/resources/config/liquibase/master.xml

key-decisions:
  - "Class-level @PreAuthorize on all three controllers: applying ROLE_ADMIN at class level rather than per-method ensures no method can be inadvertently exposed without authorization"
  - "Cascade delete via application code in @Transactional: explicit secPermissionRepository.deleteByAuthorityName before authorityRepository.deleteById provides audit consistency even when DB cascade is present"
  - "Seed data uses context=dev,test: production databases do not receive test roles via Liquibase seed changelogs"

patterns-established:
  - "Admin controller pattern: @RestController + class-level @PreAuthorize + HeaderUtil + ResponseUtil.wrapOrNotFound + BadRequestAlertException for all admin CRUD surfaces"

requirements-completed: [SEC-01, SEC-02, SEC-03]

# Metrics
duration: 5min
completed: 2026-03-21
---

# Phase 2 Plan 03: Admin REST Controllers and Seed Data Summary

**Three admin REST controllers exposing full CRUD for roles (/api/admin/sec/roles), permissions (/api/admin/sec/permissions), and row policies (/api/admin/sec/row-policies), all gated with ROLE_ADMIN, plus seed roles for dev/test environments.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T08:49:00Z
- **Completed:** 2026-03-21T08:54:03Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- SecRoleAdminResource exposes full CRUD with cascade permission delete, conflict detection, and manual Authority<->SecRoleDTO mapping (avoiding MapStruct complexity with Persistable lifecycle)
- SecPermissionAdminResource validates FK to jhi_authority before creating or updating permissions
- SecRowPolicyAdminResource checks code uniqueness on POST and selectively on PUT (only if code changes)
- Seed changelog adds ROLE_ACCOUNTANT, ROLE_STOCKKEEPER, ROLE_DIRECTOR with context=dev,test so production databases remain clean
- ArchUnit TechnicalStructureTest passes confirming web->repository layer access is compliant

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SecRoleAdminResource controller** - `a1cb88b` (feat)
2. **Task 2: Create SecPermissionAdminResource, SecRowPolicyAdminResource, and seed data** - `24ca073` (feat)

## Files Created/Modified

- `src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java` - Role CRUD with @PreAuthorize ROLE_ADMIN, nameexists conflict check, cascade permission delete in @Transactional deleteRole
- `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` - Permission CRUD with FK validation (authorityRepository.findById), idexists/idnull/idmismatch/notfound/rolenotfound checks
- `src/main/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResource.java` - Row policy CRUD with codeexists check on POST and conditional check on PUT
- `src/main/resources/config/liquibase/changelog/20260321000400_seed_security_roles.xml` - Seed ROLE_ACCOUNTANT, ROLE_STOCKKEEPER, ROLE_DIRECTOR with context=dev,test
- `src/main/resources/config/liquibase/master.xml` - Added include for seed changelog

## Decisions Made

- Class-level @PreAuthorize on all three controllers: ensures no method can be inadvertently exposed without ROLE_ADMIN authorization
- Cascade delete via application code in @Transactional: explicit secPermissionRepository.deleteByAuthorityName before authorityRepository.deleteById provides audit consistency even when DB cascade is present
- Seed data uses context=dev,test: production databases do not receive test roles via Liquibase

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None — all three controllers are fully wired to their repositories and mappers. No placeholder data flows.

## Self-Check: PASSED

All 5 files present. Both task commits exist (a1cb88b, 24ca073). Key content verified:
- SecRoleAdminResource.java contains @RequestMapping("/api/admin/sec/roles") and @PreAuthorize
- SecPermissionAdminResource.java contains @RequestMapping("/api/admin/sec/permissions") and authorityRepository.findById FK check
- SecRowPolicyAdminResource.java contains @RequestMapping("/api/admin/sec/row-policies") and findByCode uniqueness check
- 20260321000400_seed_security_roles.xml contains ROLE_ACCOUNTANT, ROLE_STOCKKEEPER, ROLE_DIRECTOR with context="dev,test"
- master.xml contains 20260321000400_seed_security_roles.xml include entry
- ./gradlew compileJava: BUILD SUCCESSFUL
- ./gradlew test --tests "com.vn.core.TechnicalStructureTest": BUILD SUCCESSFUL

---
*Phase: 02-security-metadata-management*
*Completed: 2026-03-21*
