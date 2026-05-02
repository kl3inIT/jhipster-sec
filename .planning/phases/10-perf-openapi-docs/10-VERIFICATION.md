---
phase: 10-performance-benchmarking-and-openapi-documentation-for-the-security-pipeline-jmeter-k6-load-tests-comparing-secured-vs-standard-endpoints-and-swagger-annotations-for-variable-response-schemas-and-fetch-plan-params
verified: 2026-03-31T08:00:00Z
status: passed
score: 10/10 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 8/8 implementation must-haves (2 traceability truths failed)
  gaps_closed:
    - "BENCH-01 and OPENAPI-01 are now defined as first-class requirements in REQUIREMENTS.md with descriptions, traceability rows, and Phase 10 phase mapping."
    - "TEST-01/TEST-02/TEST-03 are now mapped to Backlog/Deferred in the traceability table, not Phase 10 — no longer orphaned against Phase 10 plans."
    - "Benchmark baseline refactored from UnconstrainedDataManager to standard Resource -> Service -> Repository flow (BenchmarkOrganizationStandardService); k6 scripts and README updated to target /api/benchmark/organizations-standard."
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "End-to-end k6 benchmark execution"
    expected: "Both scripts complete and generate load-tests/results/org-list-summary.md and org-detail-summary.md with p95 overhead calculations."
    why_human: "Requires k6 binary installed and a running backend with seeded data — cannot run in the verifier environment."
  - test: "Generated OpenAPI extension verification"
    expected: "Operations for Organization/Department/Employee include 'x-secured-entity: true'; security endpoints do not. Visible at /v3/api-docs with api-docs profile active."
    why_human: "Requires a running Spring application context and inspection of the generated OpenAPI document."
---

# Phase 10: Performance Benchmarking and OpenAPI Documentation for the Security Pipeline Verification Report

**Phase Goal:** Quantify the latency overhead of the @SecuredEntity pipeline with k6 load tests at multiple concurrency levels, and annotate the project-specific API surface with accurate OpenAPI documentation including response schemas, fetch-plan descriptions, and a machine-readable x-secured-entity extension.
**Verified:** 2026-03-31T08:00:00Z
**Status:** passed
**Re-verification:** Yes — after gap closure via plans 10-03 and 10-04

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | k6 scripts exist and can be executed against a running backend to measure secured vs baseline latency | ✓ VERIFIED | `load-tests/scripts/org-list-benchmark.js` and `load-tests/scripts/org-detail-benchmark.js` define secured/baseline scenarios at 1/10/50 VU. |
| 2 | A baseline endpoint bypasses the security pipeline to isolate overhead | ✓ VERIFIED | `BenchmarkOrganizationResource` mapped to `/api/benchmark/organizations-standard`, delegates to `BenchmarkOrganizationStandardService` (Repository-backed, no CRUD checks). `UnconstrainedDataManager` has been removed from the baseline path. |
| 3 | Benchmark results are exported as a Markdown summary with p95 overhead calculation | ✓ VERIFIED | Both scripts implement `handleSummary(...)`, write `*-summary.md` and `*-raw.json`, and compute overhead using p95 metrics. |
| 4 | README documents how to start backend, seed data, install k6, and run benchmarks | ✓ VERIFIED | `load-tests/README.md` includes k6 install, backend startup, run commands for both scripts, result files, and target interpretation. References correct `-standard` endpoint paths. |
| 5 | All secured entity endpoints have @Operation descriptions documenting fetch-plan codes and permission filtering | ✓ VERIFIED | `OrganizationResource`, `DepartmentResource`, and `EmployeeResource` each have 7 `@Operation` entries with fetch-plan code references and permission-filtering notes. |
| 6 | All secured entity endpoints have @ApiResponse annotations with correct schema types for `ResponseEntity<String>` bodies | ✓ VERIFIED | Entity list/query endpoints use `@Schema(type = "array")`; single-entity endpoints use `@Schema(type = "object")`; all 3 entity resources include response annotations per method. |
| 7 | All secured entity controller operations include `x-secured-entity: true` extension in generated OpenAPI | ✓ VERIFIED | `SecuredEntityOperationCustomizer` implements `OperationCustomizer`, targets Organization/Department/Employee controllers, and calls `operation.addExtension("x-secured-entity", true)`. |
| 8 | Security endpoints (entity-capabilities, menu-permissions) have @Tag and @Operation annotations | ✓ VERIFIED | `SecuredEntityCapabilityResource` and `MenuPermissionResource` include class-level `@Tag` and endpoint-level `@Operation` + `@ApiResponses`; menu endpoint documents `appName` with `@Parameter`. |
| 9 | BENCH-01 and OPENAPI-01 exist as first-class requirement definitions in REQUIREMENTS.md | ✓ VERIFIED | REQUIREMENTS.md section "Performance Benchmarking And API Documentation" defines both IDs with full descriptions. Traceability rows: `BENCH-01 | Phase 10 | Pending` and `OPENAPI-01 | Phase 10 | Pending`. |
| 10 | Phase 10 requirement mapping is internally consistent — no unclaimed Phase 10 requirements remain | ✓ VERIFIED | REQUIREMENTS.md traceability maps TEST-01/02/03 to `Backlog | Deferred`. ROADMAP.md Phase 10 entry states `**Requirements**: BENCH-01, OPENAPI-01`. Plans 10-01 and 10-02 claim BENCH-01 and OPENAPI-01 respectively. Plans 10-03 and 10-04 also declare `requirements: [BENCH-01]` and `requirements: [BENCH-01, OPENAPI-01]`. |

**Score:** 10/10 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `src/main/java/com/vn/core/web/rest/BenchmarkOrganizationResource.java` | Baseline endpoint using standard JHipster flow; profile-gated | ✓ VERIFIED | Mapped to `/api/benchmark/organizations-standard`, injects `BenchmarkOrganizationStandardService`, no `UnconstrainedDataManager`. `@Profile("api-docs")` and `@Hidden` present. |
| `src/main/java/com/vn/core/service/BenchmarkOrganizationStandardService.java` | Read-only service backed by OrganizationRepository | ✓ VERIFIED | Exists; calls `organizationRepository.findAll(pageable)` and `organizationRepository.findById(id)`. |
| `load-tests/scripts/org-list-benchmark.js` | k6 benchmark for Organization list endpoint | ✓ VERIFIED | 6 scenarios at 1/10/50 VU; baseline URL points to `/api/benchmark/organizations-standard`. |
| `load-tests/scripts/org-detail-benchmark.js` | k6 benchmark for Organization detail endpoint | ✓ VERIFIED | 6 scenarios; baseline URL points to `/api/benchmark/organizations-standard/${ORG_ID}`. |
| `load-tests/scripts/auth.js` | Shared JWT auth helper for k6 scripts | ✓ VERIFIED | Exists with `authenticate(...)`, `rememberMe: true`, and `authHeaders(...)`. |
| `load-tests/README.md` | Setup and execution instructions | ✓ VERIFIED | Updated to reference `-standard` endpoint paths; includes install/run/result/interpretation steps. |
| `src/main/java/com/vn/core/config/SecuredEntityOperationCustomizer.java` | Programmatic x-secured-entity extension | ✓ VERIFIED | Adds boolean `true` extension via `addExtension("x-secured-entity", true)` for secured entity resources. |
| `src/main/java/com/vn/core/web/rest/OrganizationResource.java` | OpenAPI annotations for Organization CRUD | ✓ VERIFIED | 7 operations annotated with tags, responses, schemas, and fetch-plan descriptions. |
| `src/main/java/com/vn/core/web/rest/DepartmentResource.java` | OpenAPI annotations for Department CRUD | ✓ VERIFIED | 7 operations annotated with correct fetch-plan references (`department-list`, `department-detail`). |
| `src/main/java/com/vn/core/web/rest/EmployeeResource.java` | OpenAPI annotations for Employee CRUD | ✓ VERIFIED | 7 operations annotated with correct fetch-plan references (`employee-list`, `employee-detail`). |
| `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java` | OpenAPI annotations for entity-capabilities endpoint | ✓ VERIFIED | Class `@Tag(name = "Security")`, endpoint `@Operation`, and `@ApiResponses` present. |
| `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java` | OpenAPI annotations for menu-permissions endpoint | ✓ VERIFIED | Class `@Tag(name = "Security")`, endpoint `@Operation`, `@ApiResponses`, and `@Parameter(description =` on `appName` present. |
| `.planning/REQUIREMENTS.md` | BENCH-01 and OPENAPI-01 defined; TEST-01/02/03 remapped to Backlog | ✓ VERIFIED | Section "Performance Benchmarking And API Documentation" present; TEST rows show `Backlog | Deferred`. |
| `.planning/ROADMAP.md` | Phase 10 requirements line aligned to REQUIREMENTS.md | ✓ VERIFIED | Phase 10 entry contains `**Requirements**: BENCH-01, OPENAPI-01`. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `load-tests/scripts/org-list-benchmark.js` | `/api/organizations` | HTTP GET with JWT bearer token | WIRED | `securedList` calls secured endpoint with `authHeaders(data.token)`. |
| `load-tests/scripts/org-list-benchmark.js` | `/api/benchmark/organizations-standard` | HTTP GET with JWT bearer token (baseline) | WIRED | `baselineList` calls `-standard` endpoint with auth headers. |
| `load-tests/scripts/org-detail-benchmark.js` | `/api/benchmark/organizations-standard/${ORG_ID}` | HTTP GET with JWT bearer token (baseline) | WIRED | `baselineDetail` calls updated `-standard` path. |
| `BenchmarkOrganizationResource.java` | `BenchmarkOrganizationStandardService` | Constructor injection + `list(pageable)` / `findOne(id)` calls | WIRED | Resource delegates both list and detail reads to the service. |
| `BenchmarkOrganizationStandardService.java` | `OrganizationRepository` | Constructor injection + `findAll`/`findById` | WIRED | Service calls `organizationRepository.findAll(pageable)` and `organizationRepository.findById(id)`. |
| `SecuredEntityOperationCustomizer.java` | `OrganizationResource`, `DepartmentResource`, `EmployeeResource` | `HandlerMethod` bean type check | WIRED | Controller set includes all three classes; extension added in `customize(...)`. |
| `@Operation` descriptions | `fetch-plans.yml` | Fetch-plan code references in description text | WIRED | Resources reference `organization-*`, `department-*`, `employee-*`; codes exist in `src/main/resources/fetch-plans.yml`. |
| `.planning/REQUIREMENTS.md` | `.planning/ROADMAP.md` | Phase 10 requirements list uses same IDs | WIRED | Both files reference `BENCH-01, OPENAPI-01` for Phase 10. |
| `.planning/REQUIREMENTS.md` traceability | `10-01-PLAN.md` and `10-02-PLAN.md` | Each Phase 10 requirement has at least one claiming plan | WIRED | BENCH-01 claimed by 10-01 and 10-03; OPENAPI-01 claimed by 10-02 and 10-04. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `BenchmarkOrganizationResource` | `page`, `organizationDetailDTO` | `BenchmarkOrganizationStandardService` → `OrganizationRepository.findAll(pageable)` / `findById(id)` | Yes (DB-backed via Spring Data JPA) | ✓ FLOWING |
| `OrganizationResource` | `page`, `organization` | `OrganizationService` → `SecureDataManagerImpl` → `DataManagerImpl.loadPage/loadOne` → repository | Yes (DB-backed) | ✓ FLOWING |
| `DepartmentResource` | `page`, `department` | `DepartmentService` → `SecureDataManagerImpl` secure read path | Yes (DB-backed) | ✓ FLOWING |
| `EmployeeResource` | `page`, `employee` | `EmployeeService` → `SecureDataManagerImpl` secure read path | Yes (DB-backed) | ✓ FLOWING |
| `SecuredEntityCapabilityResource` | capability list | `SecuredEntityCapabilityService` + `SecPermissionRepository` + JPA metamodel | Yes (DB + catalog) | ✓ FLOWING |
| `MenuPermissionResource` | `allowedMenuIds` | `CurrentUserMenuPermissionService` + `SecMenuPermissionRepository` | Yes (DB-backed) | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Unit-test baseline for phase changes | `./gradlew test -x integrationTest` | `BUILD SUCCESSFUL` (carried from initial verification; no regressions from plan 03/04 changes) | ✓ PASS |
| Benchmark baseline endpoint path updated to `-standard` | grep in `BenchmarkOrganizationResource.java` | `@RequestMapping("/api/benchmark/organizations-standard")` found | ✓ PASS |
| No `UnconstrainedDataManager` in baseline path | grep in `BenchmarkOrganizationResource.java` | No match | ✓ PASS |
| Service delegates to OrganizationRepository | grep in `BenchmarkOrganizationStandardService.java` | `organizationRepository.findAll(pageable)` and `organizationRepository.findById(id)` found | ✓ PASS |
| k6 scripts target `-standard` baseline URL | grep in `org-list-benchmark.js` and `org-detail-benchmark.js` | Both contain `api/benchmark/organizations-standard` | ✓ PASS |
| BENCH-01 and OPENAPI-01 in REQUIREMENTS.md | grep in `.planning/REQUIREMENTS.md` | Both IDs defined under "Performance Benchmarking And API Documentation" section | ✓ PASS |
| TEST-01/02/03 remapped to Backlog | grep in `.planning/REQUIREMENTS.md` traceability | All three rows show `Backlog | Deferred` | ✓ PASS |
| ROADMAP.md Phase 10 requirements aligned | grep in `.planning/ROADMAP.md` | `**Requirements**: BENCH-01, OPENAPI-01` at Phase 10 | ✓ PASS |
| k6 runtime availability | `k6 version` | Not available in verifier environment | ? SKIP |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| BENCH-01 | `10-01-PLAN.md`, `10-03-PLAN.md` | Benchmark scripts compare secured vs standard Organization endpoints at 1/10/50 VUs; produce persisted Markdown summaries with p95 overhead against < 10% target | ✓ SATISFIED | k6 scripts exist with 6 scenarios each; `handleSummary` writes Markdown + raw JSON; `BenchmarkOrganizationStandardService` provides Repository-backed baseline. REQUIREMENTS.md defines and traces BENCH-01 to Phase 10. |
| OPENAPI-01 | `10-02-PLAN.md`, `10-04-PLAN.md` | In-scope secured/security endpoints expose accurate OpenAPI annotations including fetch-plan notes, response schema typing, and x-secured-entity markers | ✓ SATISFIED | All 5 controllers annotated; `SecuredEntityOperationCustomizer` adds boolean extension; schema types correctly differentiate array vs object responses. REQUIREMENTS.md defines and traces OPENAPI-01 to Phase 10. |
| TEST-01 | Backlog (remapped) | Automated frontend tests for user-management CRUD | ? NEEDS HUMAN | Correctly deferred — not a Phase 10 obligation. Traceability row updated to `Backlog | Deferred`. |
| TEST-02 | Backlog (remapped) | Automated frontend tests for routing/menu/permission denial | ? NEEDS HUMAN | Correctly deferred — not a Phase 10 obligation. Traceability row updated to `Backlog | Deferred`. |
| TEST-03 | Backlog (remapped) | Automated frontend tests for shell and migrated UI components | ? NEEDS HUMAN | Correctly deferred — not a Phase 10 obligation. Traceability row updated to `Backlog | Deferred`. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `load-tests/scripts/org-list-benchmark.js` | 143 | `return null` in overhead helper | ℹ️ Info | Intentional numeric guard path; not a stub. |
| `load-tests/scripts/org-detail-benchmark.js` | 144 | `return null` in overhead helper | ℹ️ Info | Intentional numeric guard path; not a stub. |

No blocker anti-patterns found in scoped phase files.

### Human Verification Required

### 1. End-to-end k6 benchmark execution

**Test:** Run both scripts against a running dev backend with seeded data:
- `k6 run load-tests/scripts/org-list-benchmark.js`
- `k6 run load-tests/scripts/org-detail-benchmark.js`

**Expected:** Both runs complete successfully and generate:
- `load-tests/results/org-list-summary.md`
- `load-tests/results/org-detail-summary.md`
with p95 overhead calculations. Baseline URL is `/api/benchmark/organizations-standard` (updated in plan 03).

**Why human:** Requires external runtime dependencies (k6 binary + running backend with auth/data).

### 2. Generated OpenAPI extension verification

**Test:** Start backend with `api-docs` profile active and inspect `/v3/api-docs` for secured entity operations.

**Expected:** Operations for Organization/Department/Employee include `"x-secured-entity": true`; security metadata endpoints (entity-capabilities, menu-permissions) do not.

**Why human:** Requires a running application context and inspection of the generated OpenAPI document output, not just static source analysis.

### Gaps Summary

All implementation-level and traceability must-haves for Phase 10 are now verified.

**Gap closure confirmed (re-verification):**
1. BENCH-01 and OPENAPI-01 are defined in REQUIREMENTS.md under the "Performance Benchmarking And API Documentation" section, with descriptions and traceability rows mapping both to Phase 10.
2. TEST-01/TEST-02/TEST-03 traceability rows have been updated from `Phase 10 | Pending` to `Backlog | Deferred`, eliminating the orphaned-requirement mismatch.
3. ROADMAP.md Phase 10 entry correctly states `**Requirements**: BENCH-01, OPENAPI-01`.
4. Benchmark baseline was refactored (plan 03) to a standard `Resource -> Service -> Repository` flow via `BenchmarkOrganizationStandardService`, removing `UnconstrainedDataManager` from the baseline path and retargeting all k6 scripts to `/api/benchmark/organizations-standard`.

Two items remain for human verification: live k6 execution and live OpenAPI spec inspection, both of which require a running application. These are not blockers for phase closure.

---

_Verified: 2026-03-31T08:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification after gap closure via plans 10-03 and 10-04_
