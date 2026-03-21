---
phase: 05-standalone-frontend-delivery
plan: "01"
subsystem: backend-api
tags: [catalog-endpoint, permission-filter, rest-api, integration-tests]
dependency_graph:
  requires: []
  provides: [GET /api/admin/sec/catalog, authorityName filter on GET /api/admin/sec/permissions]
  affects: [SecPermissionAdminResource, SecCatalogAdminResource]
tech_stack:
  added: []
  patterns: [admin-sec REST resource pattern, JPA metamodel attribute enumeration]
key_files:
  created:
    - src/main/java/com/vn/core/service/dto/security/SecCatalogEntryDTO.java
    - src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResourceIT.java
  modified:
    - src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java
    - src/test/resources/config/liquibase/changelog/20260321000800_seed_proof_security_test_data.xml
decisions:
  - Attribute enumeration uses EntityManager.getMetamodel().entity(cls).getAttributes() sorted alphabetically — consistent with JPA metamodel, avoids reflection on entity class directly
  - authorityName filter uses null/blank check — empty string treated as no-filter to avoid accidental empty-result queries
metrics:
  duration: "11 min"
  completed: "2026-03-22"
  tasks: 2
  files: 6
---

# Phase 5 Plan 01: Backend API Gaps for Permission Matrix Summary

**One-liner:** Catalog endpoint enumerating secured entities with JPA metamodel attributes, plus authorityName query filter on permission list endpoint.

## What Was Built

### Task 1: Catalog endpoint and DTO

Added `SecCatalogEntryDTO` (plain POJO with `code`, `displayName`, `operations`, `attributes` fields) and `SecCatalogAdminResource` providing `GET /api/admin/sec/catalog`. The endpoint calls `SecuredEntityCatalog.entries()` and for each entry enumerates JPA metamodel attributes via `EntityManager.getMetamodel().entity(cls).getAttributes()`. Operations are sorted alphabetically. The endpoint is admin-only via `@PreAuthorize`.

Three integration tests in `SecCatalogAdminResourceIT` verify: JSON array has >= 3 entries, the organization entry contains the expected attributes (budget, code, departments, id, name, ownerLogin), and non-admin access returns 403.

### Task 2: authorityName filter on permissions endpoint

Modified `SecPermissionAdminResource.getAllPermissions()` to accept `@RequestParam(required = false) String authorityName`. When the parameter is present and non-blank, it delegates to the existing `SecPermissionRepository.findByAuthorityName()`. When absent or blank, it falls back to `findAll()` preserving backward compatibility.

Added two integration tests: `getAllPermissions_filterByAuthorityName` verifying only permissions for the requested role are returned, and `getAllPermissions_noFilterReturnsAll` verifying the unfiltered call returns at least as many results as the filtered call.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed pre-existing sequence collision in integration tests**

- **Found during:** Task 2 (while running SecPermissionAdminResourceIT)
- **Issue:** `testGetAllPermissions` was already failing with `duplicate key value violates unique constraint "sec_permission_pkey"`. The test seed data in `20260321000800_seed_proof_security_test_data.xml` manually inserts `sec_permission` rows with IDs 1000-1016 and `sec_row_policy` rows with IDs 1100-1102. The shared `sequence_generator` starts at 1050 and Hibernate allocates in blocks of 50, causing it to issue IDs that collide with pre-seeded rows (e.g. ID 1100 is pre-seeded but also allocated by the sequence).
- **Fix:** Added Liquibase changeset `20260321000800-2` to the test seed file that resets `sequence_generator` to 2000 after all seed data is inserted.
- **Files modified:** `src/test/resources/config/liquibase/changelog/20260321000800_seed_proof_security_test_data.xml`
- **Commit:** bffdd54

## Known Stubs

None — both endpoints return real data from the live catalog and repository.

## Self-Check: PASSED

Files created:
- FOUND: src/main/java/com/vn/core/service/dto/security/SecCatalogEntryDTO.java
- FOUND: src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java
- FOUND: src/test/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResourceIT.java

Commits:
- f7c1ad5: feat(05-01): add catalog endpoint and DTO
- bffdd54: feat(05-01): add authorityName filter to permission list endpoint
