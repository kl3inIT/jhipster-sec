# Phase 10: Performance Benchmarking and OpenAPI Documentation for the Security Pipeline - Context

**Gathered:** 2026-03-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Measure the overhead introduced by the security pipeline versus standard endpoints, and annotate the project-specific API surface with accurate OpenAPI documentation. Two workstreams:

1. **Performance benchmarking** — k6 load tests comparing secured (`@SecuredEntity` / `SecureDataManagerImpl`) vs. standard endpoints at 1, 10, and 50 virtual users. Baseline target: security pipeline p95 latency overhead < 10% vs. standard endpoint.

2. **OpenAPI documentation** — Swagger/OpenAPI annotations for `/api/security/**` and the three proof-domain entity controllers (`/api/organizations/**`, `/api/departments/**`, `/api/employees/**`). Document variable response schemas (attribute-filtered by permission), fetch-plan codes used per endpoint, and mark `@SecuredEntity`-backed endpoints with a custom `x-secured-entity` extension.

This phase does NOT add new API endpoints, change security behavior, or modify fetch-plan definitions. It is documentation and measurement only.

</domain>

<decisions>
## Implementation Decisions

### Load test tooling
- **D-01:** Use **k6** as the load test tool. JavaScript-based, runs on Windows without JVM, clean CLI output, no Gradle plugin required.
- **D-02:** Load test scripts live in `load-tests/` at the project root (not `src/test/`). Results are separate from unit/integration test infrastructure.
- **D-03:** k6 results are preserved as a Markdown report in `load-tests/results/` — k6 `--summary-export` JSON converted to a `summary.md` per run, committed as a baseline snapshot.
- **D-04:** Test setup is manual: a `load-tests/README.md` explains how to start the backend with the test profile, seed test data, and run the scripts. No CI integration and no Gradle task for k6 in this phase.

### Benchmark scope
- **D-05:** Benchmark **Organization GET list** (`GET /api/organizations`) and **Organization GET single** (`GET /api/organizations/{id}`) — these are the representative endpoints covering the full security pipeline (CRUD check → fetch-plan resolution → secure serialize).
- **D-06:** Run at **1, 10, and 50 virtual users** (concurrent). Matches the todo specification for concurrency levels.
- **D-07:** Each concurrency level runs two thread groups: (a) the secured `@SecuredEntity` Organization endpoint, and (b) a baseline endpoint that bypasses `SecureDataManagerImpl` (or uses `UnconstrainedDataManager` directly) to isolate pipeline overhead.
- **D-08:** The acceptable overhead target is **< 10% p95 latency delta** between secured and baseline. The report flags if this is exceeded, but does not block builds.

### OpenAPI schema strategy
- **D-09:** Use **full schema + permission note** for secured endpoints. Document all entity fields in `@Schema`, add an `@Operation` description noting that fields may be omitted based on the caller's `VIEW` permission via `SecureEntitySerializerImpl`. No per-role schema variants.
- **D-10:** Mark all `@SecuredEntity`-backed endpoints with a custom OpenAPI extension `x-secured-entity: true` in the operation extensions. This lets consumers identify the security pipeline in the raw spec and Swagger UI.
- **D-11:** Annotate **security endpoints and proof-domain endpoints only**: `/api/security/**` (entity-capabilities, menu-permissions), `/api/organizations/**`, `/api/departments/**`, `/api/employees/**`. Legacy JHipster admin/user/account endpoints are out of scope.

### Fetch-plan documentation
- **D-12:** Document fetch-plan codes **in `@Operation` descriptions only** — no API contract change. Each endpoint description notes which fetch-plan code it uses and what relations that plan includes (e.g., `"Uses fetch-plan: organization-list — includes id, name, status, no nested relations"`). Valid codes come from `src/main/resources/fetch-plans.yml`.
- **D-13:** Do not expose `fetchPlan` as a query parameter in this phase. The fetch plan per endpoint is fixed and the API contract does not change.

### Claude's Discretion
- Exact k6 script structure (shared auth setup, thresholds format, sleep intervals between requests)
- Whether the baseline endpoint for benchmarking reuses an existing `UnconstrainedDataManager` path or adds a temporary test-only bypass endpoint (prefer reusing existing infrastructure)
- Exact `@ApiResponse` content type and media type annotations for `ResponseEntity<String>` (raw JSON) endpoints
- springdoc configuration needed to enable the `x-secured-entity` extension in generated spec output

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project context and security pipeline
- `.planning/PROJECT.md` — Core value, constraints (fetch plans YAML only, PrimeNG-first, brownfield safety), key decisions table
- `.planning/REQUIREMENTS.md` — TEST-01/TEST-02/TEST-03 pending requirements; PERF-01/PERF-02/PERF-03 validated in Phase 9 for context
- `.planning/STATE.md` — Phase 9 completion notes; `Pending Todos` section has the original performance-test and Swagger todo descriptions

### Security pipeline implementation (what is being measured and documented)
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — The pipeline under test: CRUD check → fetch-plan resolution → secure serialize/merge
- `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java` — Serialization edge called from Resource controllers
- `src/main/java/com/vn/core/security/web/SecuredEntityPayloadValidator.java` — Validation edge

### Endpoints to annotate and benchmark
- `src/main/java/com/vn/core/web/rest/OrganizationResource.java` — Primary benchmark + annotation target
- `src/main/java/com/vn/core/web/rest/DepartmentResource.java` — Annotation target
- `src/main/java/com/vn/core/web/rest/EmployeeResource.java` — Annotation target
- `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java` — Security endpoint annotation target
- `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java` — Security endpoint annotation target (if project-custom)

### Fetch-plan definitions (source of truth for @Operation descriptions)
- `src/main/resources/fetch-plans.yml` — All valid fetch-plan codes and their field/relation sets

### OpenAPI infrastructure
- `build.gradle` — springdoc-openapi dependency already present
- `src/main/resources/config/application.yml` — `!api-docs` profile disables OpenAPI; `application-api-docs.yml` enables it

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `springdoc-openapi-starter-webmvc-api` is already a dependency — no new dependencies needed for annotation support
- `OrganizationResource`, `DepartmentResource`, `EmployeeResource` already exist with the full secured CRUD surface — annotation targets are well-defined
- `SecuredEntityCapabilityResource` and `MenuPermissionResource` exist as security endpoint annotation targets
- OpenAPI is currently enabled only under the `api-docs` Spring profile — no change needed to that infrastructure

### Established Patterns
- Resource controllers return `ResponseEntity<String>` for secured entities (raw JSON from `SecuredEntityJsonAdapter`) — `@Schema(type = "object")` or similar will be needed to document these accurately
- `@ParameterObject Pageable pageable` already used in list endpoints — pageable params are already documented by springdoc
- JWT bearer auth is already configured by JHipster's springdoc integration — no need to re-add `@SecurityRequirement`
- No `@Operation`, `@ApiResponse`, or `@Tag` annotations exist anywhere in project-custom resource files currently

### Integration Points
- k6 scripts authenticate via `POST /api/authenticate` (JHipster JWT login) — need to capture the token in k6 setup phase
- The baseline endpoint for benchmarking should reuse `UnconstrainedDataManager` if a suitable existing path exists, to avoid adding test-only code to production controllers

</code_context>

<specifics>
## Specific Ideas

- The todo file explicitly named the overhead goal as "< 20%" but the user tightened it to **< 10% p95**. This is the binding threshold for the benchmark report.
- The todo file's suggested structure: two thread groups (secured vs baseline), metrics at p50/p95/p99 latency + throughput (req/s) + error rate — use this structure in the k6 script.

</specifics>

<deferred>
## Deferred Ideas

- Exposing `?fetchPlan=` as an optional HTTP query parameter to let callers override the default fetch plan — deferred, no API contract change this phase.
- CI integration for k6 benchmarks (Gradle task, GitHub Actions step) — deferred, manual execution only in Phase 10.
- Annotation coverage for legacy JHipster admin/user/account/authority endpoints — out of scope for Phase 10.
- Per-role `@ApiResponse` schema variants — rejected as hard to maintain; full-schema + permission note chosen instead.

</deferred>

---

*Phase: 10-performance-benchmarking-and-openapi-documentation*
*Context gathered: 2026-03-31*
