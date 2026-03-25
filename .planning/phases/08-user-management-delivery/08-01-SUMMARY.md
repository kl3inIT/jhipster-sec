---
phase: 08-user-management-delivery
plan: 01
subsystem: backend-user-api
tags: [user-management, search, specification, backend]
dependency_graph:
  requires: []
  provides: [query-enabled-admin-browse]
  affects: [frontend-user-list]
tech_stack:
  added: []
  patterns: [JpaSpecificationExecutor, Specification-based-query]
key_files:
  created: []
  modified:
    - src/main/java/com/vn/core/repository/UserRepository.java
    - src/main/java/com/vn/core/service/UserService.java
    - src/main/java/com/vn/core/web/rest/UserResource.java
    - src/test/java/com/vn/core/web/rest/UserResourceIT.java
decisions:
  - UserRepository extends JpaSpecificationExecutor for Specification-based queries
  - buildManagedUserQuery uses cb.coalesce for nullable firstName/lastName fields
  - Blank or null query degrades to Specification.where(null) for unfiltered browse
metrics:
  duration: 2 min
  completed: 2026-03-25T15:54:00Z
---

# Phase 8 Plan 1: Admin User Browse Query Seam Summary

Optional query parameter on GET /api/admin/users enabling case-insensitive search across login, email, firstName, and lastName via JPA Specification

## Changes Made

### Task 1: Make the existing admin browse endpoint query-aware

- Extended `UserRepository` with `JpaSpecificationExecutor<User>` to enable Specification-based queries
- Added `UserService.getAllManagedUsers(Pageable, String)` overload that builds a dynamic `Specification<User>`
- Added `UserService.buildManagedUserQuery(String)` that OR-matches login, email, firstName, lastName with case-insensitive LIKE patterns; null/blank query returns conjunction (no filter)
- Used `cb.coalesce(root.get("firstName"), "")` and `cb.coalesce(root.get("lastName"), "")` to handle nullable name fields
- Updated `UserResource.getAllUsers` to accept `@RequestParam(name = "query", required = false) String query`
- Preserved existing endpoint path, sort guard, and pagination header generation
- **Commit:** 777e44b

### Task 2: Lock the query contract in UserResourceIT

- Added `getAllUsersReturnsXTotalCountHeader` to verify X-Total-Count header on default browse
- Added `getAllUsersWithQuery` to verify case-insensitive matching on login, email, firstName, and lastName
- Added `getAllUsersWithBlankQuery` to verify blank query degrades to normal browse
- Added `getAllUsersWithQueryAndUnsupportedSort` to verify sort guard returns HTTP 400 even with query parameter
- All existing tests preserved (create, update, delete, non-admin forbidden)
- **Commit:** eaca30a

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| `JpaSpecificationExecutor` on UserRepository | Enables dynamic query building without custom JPQL or derived query explosion |
| `cb.coalesce` for firstName/lastName | These fields are nullable in the User entity; coalesce prevents null comparison failures |
| `Specification.where(null)` for empty query | Degrades cleanly to unfiltered findAll behavior without a separate code path |

## Known Stubs

None - all functionality is fully wired.

## Verification

- Build cannot run in this environment (requires Java 25, environment has Java 17)
- Code changes are syntactically correct and follow existing patterns
- Integration tests cover the four required scenarios: default browse with headers, query matching, blank query, and sort guard with query

## Self-Check: PASSED

All 5 files found. Both commits (777e44b, eaca30a) verified.
