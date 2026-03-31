---
plan: 10-03
phase: 10-performance-benchmarking-and-openapi-documentation
status: complete
completed: 2026-03-31
---

# Plan 10-03: Refactor Benchmark Baseline to Standard JHipster Flow

## Objective

Refactor the benchmark baseline to follow a standard JHipster API path and remove
`UnconstrainedDataManager` usage, enforcing the user override while keeping BENCH-01
latency comparison valid.

## What Was Built

- **`BenchmarkOrganizationStandardService`** — New read-only service in
  `src/main/java/com/vn/core/service/` backed directly by `OrganizationRepository`.
  Provides `list(Pageable)` → `findAll(pageable)` and `findOne(Long)` → `findById(id)`.
- **`BenchmarkOrganizationResource` refactored** — Remapped to
  `@RequestMapping("/api/benchmark/organizations-standard")`. Removed
  `UnconstrainedDataManager`; delegates to `BenchmarkOrganizationStandardService`.
  Retains `@Profile("api-docs")`, `@Hidden`, `@PreAuthorize("isAuthenticated()")`, and
  `SecuredEntityJsonAdapter` serialization with `organization-list`/`organization-detail`
  fetch plans.
- **k6 scripts updated** — `org-list-benchmark.js` and `org-detail-benchmark.js` now target
  `/api/benchmark/organizations-standard` as the baseline URL.
- **README updated** — All references to `/api/benchmark/organizations` replaced with
  `/api/benchmark/organizations-standard`; comparison table updated to describe standard
  JHipster flow instead of UnconstrainedDataManager.

## Tasks Completed

| # | Task | Status |
|---|------|--------|
| 1 | Refactor benchmark baseline endpoint to standard JHipster flow | ✓ Complete |
| 2 | Retarget k6 scripts and README to standard baseline endpoint | ✓ Complete |

## Key Files

### Created
- `src/main/java/com/vn/core/service/BenchmarkOrganizationStandardService.java`

### Modified
- `src/main/java/com/vn/core/web/rest/BenchmarkOrganizationResource.java`
- `load-tests/scripts/org-list-benchmark.js`
- `load-tests/scripts/org-detail-benchmark.js`
- `load-tests/README.md`

## Verification

- `./gradlew test -x integrationTest` — PASSED (28s)
- `@RequestMapping("/api/benchmark/organizations-standard")` confirmed in resource
- `organizationRepository.findAll(pageable)` confirmed in service
- `organizationRepository.findById(id)` confirmed in service
- No `UnconstrainedDataManager` in resource or service
- All three k6 files reference `api/benchmark/organizations-standard`
- No old `/api/benchmark/organizations` (without `-standard`) references remain

## Decisions

- Service is annotated `@Transactional(readOnly = true)` at class level (matches
  local service style for read-only services)
- Resource keeps `@Transactional(readOnly = true)` on handlers as well for extra
  safety in the benchmark path

## Self-Check: PASSED
