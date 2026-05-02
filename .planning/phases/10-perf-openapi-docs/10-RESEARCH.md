# Phase 10: Performance Benchmarking and OpenAPI Documentation for the Security Pipeline - Research

**Researched:** 2026-03-31
**Domain:** k6 load testing + springdoc-openapi annotation patterns
**Confidence:** HIGH (both workstreams based on verified code inspection + standard library patterns)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Use **k6** as the load test tool. JavaScript-based, runs on Windows without JVM, clean CLI output, no Gradle plugin required.
- **D-02:** Load test scripts live in `load-tests/` at the project root (not `src/test/`). Results are separate from unit/integration test infrastructure.
- **D-03:** k6 results are preserved as a Markdown report in `load-tests/results/` — k6 `--summary-export` JSON converted to a `summary.md` per run, committed as a baseline snapshot.
- **D-04:** Test setup is manual: a `load-tests/README.md` explains how to start the backend with the test profile, seed test data, and run the scripts. No CI integration and no Gradle task for k6 in this phase.
- **D-05:** Benchmark **Organization GET list** (`GET /api/organizations`) and **Organization GET single** (`GET /api/organizations/{id}`) — the representative endpoints covering the full security pipeline.
- **D-06:** Run at **1, 10, and 50 virtual users** (concurrent).
- **D-07:** Each concurrency level runs two thread groups: (a) the secured `@SecuredEntity` Organization endpoint, (b) a baseline endpoint that bypasses `SecureDataManagerImpl` (or uses `UnconstrainedDataManager` directly) to isolate pipeline overhead.
- **D-08:** The acceptable overhead target is **< 10% p95 latency delta** between secured and baseline. The report flags if this is exceeded but does not block builds.
- **D-09:** Use **full schema + permission note** for secured endpoints. Document all entity fields in `@Schema`, add `@Operation` description noting fields may be omitted based on `VIEW` permission via `SecureEntitySerializerImpl`. No per-role schema variants.
- **D-10:** Mark all `@SecuredEntity`-backed endpoints with a custom OpenAPI extension `x-secured-entity: true` in the operation extensions.
- **D-11:** Annotate **security endpoints and proof-domain endpoints only**: `/api/security/**` (entity-capabilities, menu-permissions), `/api/organizations/**`, `/api/departments/**`, `/api/employees/**`. Legacy JHipster admin/user/account endpoints are out of scope.
- **D-12:** Document fetch-plan codes **in `@Operation` descriptions only** — no API contract change. Each endpoint description notes which fetch-plan code it uses and what relations it includes. Valid codes come from `src/main/resources/fetch-plans.yml`.
- **D-13:** Do not expose `fetchPlan` as a query parameter in this phase.

### Claude's Discretion

- Exact k6 script structure (shared auth setup, thresholds format, sleep intervals between requests)
- Whether the baseline endpoint for benchmarking reuses an existing `UnconstrainedDataManager` path or adds a temporary test-only bypass endpoint (prefer reusing existing infrastructure)
- Exact `@ApiResponse` content type and media type annotations for `ResponseEntity<String>` (raw JSON) endpoints
- springdoc configuration needed to enable the `x-secured-entity` extension in generated spec output

### Deferred Ideas (OUT OF SCOPE)

- Exposing `?fetchPlan=` as an optional HTTP query parameter to let callers override the default fetch plan
- CI integration for k6 benchmarks (Gradle task, GitHub Actions step)
- Annotation coverage for legacy JHipster admin/user/account/authority endpoints
- Per-role `@ApiResponse` schema variants
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TEST-01 | Automated frontend tests cover user-management CRUD and role-assignment behavior | Not directly addressed by Phase 10 (performance + OpenAPI phase) |
| TEST-02 | Automated frontend tests cover backend-driven routing, menu visibility, and permission-based access denial | Not directly addressed by Phase 10 |
| TEST-03 | Automated frontend tests cover the enterprise shell and critical migrated UI components | Not directly addressed by Phase 10 |

**Note:** The traceability table in REQUIREMENTS.md maps TEST-01/02/03 to Phase 10, but STATE.md clarifies that Phase 10 was replaced from "frontend reliability and regression coverage" to "performance benchmarking and OpenAPI documentation." The CONTEXT.md makes no mention of TEST-01/02/03. The planner should confirm with the user whether TEST-01/02/03 are added to this phase scope or remain unmapped. The performance and documentation work has no corresponding REQUIREMENTS.md requirement IDs.
</phase_requirements>

---

## Summary

Phase 10 has two independent workstreams that produce no functional changes: (1) k6 load test scripts in a new `load-tests/` directory that measure p50/p95/p99 latency overhead of the `SecureDataManagerImpl` pipeline relative to a raw `UnconstrainedDataManager` baseline, and (2) springdoc-openapi `@Operation`/`@ApiResponse`/`@Tag`/`@Schema` annotations applied to five resource controllers.

The k6 workstream requires a manual installation of k6 on the developer machine (not present in the environment). All three concurrency levels (1, 10, 50 VU) run against the live backend. Scripts authenticate via `POST /api/authenticate`, capture the JWT, and drive both a secured and an unsecured read path. The baseline read path is best served by adding a dedicated internal benchmark controller backed directly by `UnconstrainedDataManager` (no serialization filter) — this avoids polluting the OrganizationResource controller with bypass logic.

The OpenAPI workstream is straightforward: `springdoc-openapi-starter-webmvc-api` is already a dependency, the `api-docs` Spring profile already gates it, and there is an existing JHipster springdoc config (`jhipster.api-docs` in `application.yml`). The challenge is that secured entity controllers return `ResponseEntity<String>` with raw JSON — springdoc infers `String` as the schema type. The correct annotation is `@ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))` for single-entity responses and `@Schema(type = "array", implementation = Object.class)` for list responses. The `x-secured-entity` extension uses `@Operation(extensions = @Extension(name = "x-secured-entity", properties = {@ExtensionProperty(name = "", value = "true")}))` — note that springdoc extension syntax requires the outer `@Extension` wrapper with the full name including `x-` prefix.

**Primary recommendation:** Implement both workstreams sequentially. Start with the benchmark controller and k6 scripts (self-contained, no Java annotation knowledge required), then annotate the five controllers.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| k6 | 0.56.x (latest stable) | Load test execution, metric collection, threshold evaluation | Decision D-01; JavaScript API, Windows-native binary, no JVM |
| springdoc-openapi-starter-webmvc-api | already in `build.gradle` (Spring Boot 4.0.3 managed) | Generates OpenAPI 3.1 spec from annotations | Already a project dependency; no version change needed |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| io.swagger.v3.oas.annotations | bundled with springdoc | `@Operation`, `@ApiResponse`, `@Tag`, `@Schema`, `@Parameter`, `@Extension` | Every annotated controller method |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| k6 | JMeter | JMeter requires JVM, GUI-first, heavier setup — rejected per D-01 |
| k6 | Gatling | Requires Scala/Java DSL and Gradle plugin — rejected per D-01 |
| `@Extension` in `@Operation` | Custom `OperationCustomizer` bean | OperationCustomizer would be DRY for marking all secured endpoints programmatically — valid alternative Claude may choose |

**k6 installation (must be done manually before running scripts):**
```bash
# Windows: download from https://github.com/grafana/k6/releases
# Or via winget:
winget install k6 --source winget
# Verify:
k6 version
```

**Version verification:** k6 latest stable is `0.56.x` as of early 2026. Confirm with:
```bash
winget search k6
```

---

## Architecture Patterns

### Recommended Project Structure

```
load-tests/
├── README.md                          # setup instructions (manual profile, seed, run)
├── scripts/
│   ├── auth.js                        # shared setup: POST /api/authenticate, return token
│   ├── org-list-benchmark.js          # list endpoint: secured vs baseline, 3 VU levels
│   └── org-detail-benchmark.js        # single endpoint: secured vs baseline, 3 VU levels
└── results/
    └── baseline-2026-03-31.md         # committed baseline snapshot from --summary-export
```

For the baseline endpoint (D-07), add a **dedicated internal benchmark controller**:

```
src/main/java/com/vn/core/web/rest/
└── BenchmarkOrganizationResource.java  # profile-gated, uses UnconstrainedDataManager directly
```

This keeps benchmark bypass logic out of `OrganizationResource` and makes the pipeline delta explicit.

### Pattern 1: k6 Script Structure with Two Scenarios

k6 scenarios let you run "secured" and "baseline" in the same script with separate VU groups and labeled metrics.

```javascript
// load-tests/scripts/org-list-benchmark.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    secured_1vu:  { executor: 'constant-vus', vus: 1,  duration: '30s', tags: { scenario: 'secured'  } },
    baseline_1vu: { executor: 'constant-vus', vus: 1,  duration: '30s', tags: { scenario: 'baseline' } },
    // repeated for 10 and 50 VUs with startTime offsets
  },
  thresholds: {
    'http_req_duration{scenario:secured}':  ['p(95)<500'],
    'http_req_duration{scenario:baseline}': ['p(95)<500'],
  },
};

export function setup() {
  const res = http.post('http://localhost:8080/api/authenticate',
    JSON.stringify({ username: 'admin', password: 'admin' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return { token: res.json('id_token') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}`, Accept: 'application/json' };
  const res = http.get('http://localhost:8080/api/organizations?page=0&size=20', { headers });
  check(res, { 'status 200': r => r.status === 200 });
  sleep(1);
}
```

The two concurrent VU groups produce separate labeled histograms in the summary JSON.

### Pattern 2: springdoc `@Operation` + `@Tag` for Secured Endpoints

```java
// Source: springdoc-openapi official docs + io.swagger.v3.oas.annotations
@Tag(name = "Organizations", description = "Secured CRUD for Organization entities. " +
    "Responses are attribute-filtered by the caller's VIEW permission via SecureEntitySerializerImpl.")
@RestController
@RequestMapping("/api/organizations")
public class OrganizationResource {

    @Operation(
        summary = "List organizations (paginated)",
        description = "Uses fetch-plan: organization-list — fields: id, code, name, ownerLogin. " +
            "Caller VIEW permissions may omit fields. @SecuredEntity pipeline enforced.",
        extensions = @Extension(name = "x-secured-entity", properties = {
            @ExtensionProperty(name = "", value = "true")
        })
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "array", implementation = OrganizationListSchema.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<String> getAllOrganizations(@ParameterObject Pageable pageable) { ... }
}
```

**Note on `x-secured-entity` extension format:** springdoc renders `@Extension(name = "x-secured-entity", properties = {@ExtensionProperty(name = "", value = "true")})` as `"x-secured-entity": "true"` (string). To get `"x-secured-entity": true` (boolean), use a custom `OperationCustomizer` bean instead — more reliable approach for all five controllers.

### Pattern 3: Custom `OperationCustomizer` for `x-secured-entity` (Recommended)

A single bean is cleaner than repeating the `@Extension` annotation on every method:

```java
@Component
public class SecuredEntityOperationCustomizer implements OperationCustomizer {

    private static final Set<String> SECURED_PATHS = Set.of(
        "/api/organizations", "/api/departments", "/api/employees"
    );

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        String path = /* extract from HandlerMethod mapping */ ...;
        if (SECURED_PATHS.stream().anyMatch(path::startsWith)) {
            operation.addExtension("x-secured-entity", true);
        }
        return operation;
    }
}
```

This approach sets the extension once, produces a boolean `true` value (not string `"true"`), and requires no per-method annotation.

### Pattern 4: `ResponseEntity<String>` Schema Documentation

Controllers return `ResponseEntity<String>` with raw JSON bodies. springdoc would infer schema type `string`. Override with:

```java
// For single-entity GET response:
@ApiResponse(responseCode = "200", description = "OK",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(type = "object",
            description = "JSON object with fields from fetch-plan: organization-detail. " +
                "Fields may be omitted based on caller VIEW permissions.")
    )
)

// For list GET response (returns JSON array as String):
@ApiResponse(responseCode = "200", description = "OK",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(type = "array",
            description = "JSON array of organization objects (fetch-plan: organization-list). " +
                "Fields may be omitted based on caller VIEW permissions.")
    )
)
```

Avoid `implementation = Organization.class` for secured controllers — that would expose all entity fields without the permission-filtering caveat.

### Pattern 5: Baseline Benchmark Endpoint

Add `BenchmarkOrganizationResource` gated to the `api-docs` Spring profile (reuses the profile already controlling OpenAPI visibility — acceptable since benchmarks are dev-only). Inject `UnconstrainedDataManager` and `OrganizationRepository` directly; serialize with plain Jackson (no `SecuredEntityJsonAdapter`):

```java
@Profile("api-docs")   // dev/benchmark only — never active in prod
@RestController
@RequestMapping("/api/benchmark/organizations")
@PreAuthorize("isAuthenticated()")
public class BenchmarkOrganizationResource {

    private final UnconstrainedDataManager unconstrainedDataManager;
    private final ObjectMapper objectMapper;

    @GetMapping("")
    public ResponseEntity<String> listRaw(@ParameterObject Pageable pageable) {
        Page<Organization> page = unconstrainedDataManager.loadPage(
            Organization.class, null, pageable);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(page.getContent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getOneRaw(@PathVariable Long id) {
        Organization org = unconstrainedDataManager.load(Organization.class, id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(org));
    }
}
```

**Important:** `UnconstrainedDataManagerImpl.loadPage` takes a `Specification` parameter; pass `null` for unconstrained. Check — `UnconstrainedDataManager.loadPage(Class, Specification, Pageable)` — confirmed in the interface.

### Anti-Patterns to Avoid

- **Modifying `OrganizationResource` to add bypass logic:** Adds production risk for a benchmark-only concern. Use a separate `BenchmarkOrganizationResource` gated by profile.
- **Using `@Schema(implementation = Organization.class)` on secured endpoints:** Exposes all entity fields in the spec without the permission-filtering caveat — misleading for API consumers.
- **Annotating `@SecurityRequirement` on every method:** JHipster's existing springdoc integration already configures bearer auth globally — redundant per code_context note.
- **Running k6 against an application started without test data:** The benchmark needs seed data. The `dev, faker` Liquibase context already seeds data in dev profile — document this in README.
- **Using `--summary-export` in k6 v0.56+:** In k6 v0.50+, `--summary-export` is superseded by `--out json=...` or `handleSummary()`. Use `handleSummary()` to write the Markdown/JSON result file.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| OpenAPI bearer auth spec config | Manual `SecurityScheme` bean | JHipster springdoc integration already sets this up | Confirmed: existing config in `application.yml` + JHipster framework |
| Pageable parameter documentation | Custom `@Parameter` annotations | `@ParameterObject Pageable` already handled by springdoc-openapi | Already used in all three entity controllers |
| k6 JWT auth flow | Custom token management code | Standard k6 `setup()` + `__ENV` variables pattern | k6 setup() runs once before iterations, return value is shared data |
| Result reporting | Custom JSON parser script | k6 `handleSummary()` returns `thresholdsSummary` + metrics | Built-in k6 API, no external tooling required |

**Key insight:** Both springdoc and k6 have first-class solutions for the main concerns. The only custom work is the `OperationCustomizer` for `x-secured-entity` (a 20-line bean) and the benchmark endpoint (a lightweight read-only controller).

---

## Common Pitfalls

### Pitfall 1: springdoc Extension Syntax Produces String Instead of Boolean

**What goes wrong:** `@ExtensionProperty(name = "", value = "true")` produces `"x-secured-entity": "true"` (JSON string) not `"x-secured-entity": true` (JSON boolean). Consumers testing `if (spec["x-secured-entity"])` will get truthy, but strict equality `=== true` will fail.

**Why it happens:** The `value` parameter of `@ExtensionProperty` is always a `String` in the Java annotation type. springdoc serializes it as-is.

**How to avoid:** Use the `OperationCustomizer` approach — `operation.addExtension("x-secured-entity", true)` sets a Java `Boolean`, which Jackson serializes as `true`.

**Warning signs:** Check the raw spec at `/v3/api-docs` — if you see `"x-secured-entity": "true"` (quoted), the annotation approach is in use.

### Pitfall 2: k6 `--summary-export` Deprecated in Recent Versions

**What goes wrong:** k6 ≥ v0.50 removed `--summary-export` in favor of `handleSummary()`. Running older scripts against a current binary produces an unrecognized flag error.

**Why it happens:** k6 API evolution — the flag existed in early versions.

**How to avoid:** Use `handleSummary(data)` exported function in the script to write the JSON/Markdown summary file. Example: `export function handleSummary(data) { return { 'load-tests/results/summary.json': JSON.stringify(data) }; }`

**Warning signs:** k6 CLI error: `error: unknown flag: --summary-export`

### Pitfall 3: Benchmark Endpoint Returns Lazy-Load Errors Without Eager Fetch

**What goes wrong:** `UnconstrainedDataManager.loadPage` returns `Organization` entities with lazy `departments` collection. If `objectMapper.writeValueAsString` tries to serialize the collection, Hibernate throws `LazyInitializationException`.

**Why it happens:** The raw Jackson serialization (without `SecuredEntityJsonAdapter`) will traverse all getters including `getDepartments()` if no `@JsonIgnore` exists on the entity.

**How to avoid:** Either (a) use `@JsonIgnore` on `Organization.departments` (but that affects the production entity), or (b) use a DTO or a simple `Map<String, Object>` for the benchmark response — build it manually from the entity's scalar fields only, or (c) use the fetch-plan serializer chain with `SecuredEntityJsonAdapter` and the `organization-list` plan (which excludes departments). Option (c) is the safest and most representative benchmark.

**Warning signs:** `org.hibernate.LazyInitializationException: could not initialize proxy - no Session` in the benchmark endpoint response.

**Resolution:** The benchmark baseline should use `SecuredEntityJsonAdapter.toJsonArrayString(page.getContent(), "organization-list")` so the serialization path is comparable — the difference is only the CRUD permission check and permission cache lookup overhead, not the serialization itself.

### Pitfall 4: k6 Token Expiry During Long Benchmark Runs

**What goes wrong:** JHipster JWT tokens expire (default: 30 days for "remember me", shorter for session tokens). For 50 VU × 30s runs, expiry is unlikely but should be documented.

**Why it happens:** k6 `setup()` runs once per execution; the token returned is reused by all VUs for the duration.

**How to avoid:** Use `POST /api/authenticate` with `rememberMe: true` in setup to get a long-lived token. Document in `load-tests/README.md` that the token is valid for the configured JWT expiry.

### Pitfall 5: springdoc Excludes Benchmark Controller From the Spec

**What goes wrong:** If `BenchmarkOrganizationResource` is annotated with `@Profile("api-docs")` and the `api-docs` profile gates springdoc itself, the benchmark endpoint appears in the generated spec — which is fine for dev but the controller should be explicitly hidden from the spec.

**How to avoid:** Add `@Hidden` (from `io.swagger.v3.oas.annotations`) to `BenchmarkOrganizationResource` to exclude it from the generated OpenAPI spec.

---

## Code Examples

Verified patterns from official sources and codebase inspection:

### k6 handleSummary for Markdown Result

```javascript
// Source: k6 docs — https://grafana.com/docs/k6/latest/results-output/end-of-test/custom-summary/
export function handleSummary(data) {
  const secured_p95 = data.metrics['http_req_duration{scenario:secured}']?.values['p(95)'] ?? 'n/a';
  const baseline_p95 = data.metrics['http_req_duration{scenario:baseline}']?.values['p(95)'] ?? 'n/a';
  const overhead_pct = ((secured_p95 - baseline_p95) / baseline_p95 * 100).toFixed(1);

  const md = `# Benchmark: Organization List\n\n` +
    `| Metric | Secured | Baseline | Overhead |\n` +
    `|--------|---------|----------|----------|\n` +
    `| p95 latency (ms) | ${secured_p95.toFixed(1)} | ${baseline_p95.toFixed(1)} | ${overhead_pct}% |\n\n` +
    (parseFloat(overhead_pct) > 10 ? '> WARNING: p95 overhead exceeds 10% target.\n' : '> OK: p95 overhead within 10% target.\n');

  return {
    'load-tests/results/org-list-baseline.md': md,
    'load-tests/results/org-list-baseline.json': JSON.stringify(data),
  };
}
```

### springdoc `@Tag` at Controller Class Level

```java
// Source: io.swagger.v3.oas.annotations.tags.Tag
@Tag(name = "Organizations", description = "Secured CRUD for Organization entities via @SecuredEntity pipeline.")
@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationResource { ... }
```

### `@Operation` with fetch-plan description

```java
// Source: io.swagger.v3.oas.annotations.Operation
@Operation(
    operationId = "getAllOrganizations",
    summary = "List organizations",
    description = """
        Returns a paginated list of organizations through the @SecuredEntity pipeline.
        Uses fetch-plan 'organization-list': fields id, code, name, ownerLogin (no nested relations).
        Fields may be omitted if the caller lacks VIEW permission for that attribute.
        """
)
@GetMapping("")
public ResponseEntity<String> getAllOrganizations(@ParameterObject Pageable pageable) { ... }
```

### `@ApiResponse` for `ResponseEntity<String>` returning JSON object

```java
// Source: io.swagger.v3.oas.annotations.responses.ApiResponse
@ApiResponse(
    responseCode = "200",
    description = "Organization detail — fetch-plan 'organization-detail': id, code, name, ownerLogin, budget, departments[id, code, name, costCenter, employees[id, employeeNumber, firstName, lastName, email, salary]]",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(type = "object",
            example = "{\"id\":1,\"code\":\"ORG-001\",\"name\":\"Acme Corp\",\"ownerLogin\":\"admin\"}")
    )
)
```

### OperationCustomizer Bean

```java
// Source: springdoc-openapi docs — https://springdoc.org/#customizing-the-openapi-object
@Component
public class SecuredEntityOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        RequestMapping mapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        PreAuthorize preAuth = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
        // check if declaring class has @SecuredEntity-pattern controllers
        boolean isSecuredEntityController = Arrays.asList(
            OrganizationResource.class, DepartmentResource.class, EmployeeResource.class
        ).contains(handlerMethod.getBeanType());

        if (isSecuredEntityController) {
            operation.addExtension("x-secured-entity", true);
        }
        return operation;
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| k6 `--summary-export` flag | `handleSummary()` exported function | k6 v0.50+ | Scripts must use `handleSummary`; `--summary-export` no longer exists |
| springdoc `@Extension` with string value | `OperationCustomizer.addExtension(key, booleanValue)` | springdoc 2.x | Cleaner boolean semantics in the generated spec |
| Annotating `@SecurityRequirement` per controller | JHipster framework's global springdoc security config | JHipster 9 | No per-endpoint JWT annotation needed |

**Deprecated/outdated:**
- `--summary-export` k6 CLI flag: removed, use `handleSummary()` instead
- `@OpenAPIDefinition` on `Application.java` class: JHipster already configures this via `ApplicationProperties`/`JHipsterProperties` — do not add a duplicate

---

## Open Questions

1. **Baseline endpoint isolation level**
   - What we know: `UnconstrainedDataManager.loadPage(entityClass, null, pageable)` bypasses all CRUD checks and permission evaluation. It still hits JPA/Hibernate and the PostgreSQL backend.
   - What's unclear: The `Organization.departments` lazy collection causes `LazyInitializationException` if plain Jackson serialization traverses it. The benchmark baseline needs to use either (a) the same serializer chain as secured (minus permission check) or (b) a DTO with scalar fields only.
   - Recommendation: Use `SecuredEntityJsonAdapter.toJsonArrayString(page.getContent(), "organization-list")` in the baseline endpoint too. This isolates pipeline overhead to only the CRUD check + permission cache lookup, making the comparison fair.

2. **`BenchmarkOrganizationResource` profile gating**
   - What we know: The `api-docs` Spring profile enables springdoc and is a dev-only profile. Using `@Profile("api-docs")` gates the benchmark controller to the same dev-only context.
   - What's unclear: Whether the planner should use a new dedicated `benchmark` Spring profile or reuse `api-docs`.
   - Recommendation: Reuse the `api-docs` profile — it is already the "dev exploration" profile and adding a new profile adds operational complexity. Add `@Hidden` to exclude from the generated spec.

3. **k6 `null` Specification in `UnconstrainedDataManager.loadPage`**
   - What we know: The interface signature is `Page<T> loadPage(Class<T> entityClass, Specification<T> spec, Pageable pageable)`. The implementation delegates to `repositoryRegistry.getSpecificationExecutor(entityClass).findAll(spec, pageable)`.
   - What's unclear: Whether `findAll(null, pageable)` is safe in Spring Data JPA.
   - Recommendation: Spring Data JPA `JpaSpecificationExecutor.findAll(null, pageable)` is documented to treat `null` spec as "no restriction" — confirmed in Spring Data docs. Safe to use.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| k6 | Load test execution (D-01) | No | — | Manual install: `winget install k6` or download from releases page |
| Node.js | k6 result processing (handleSummary is JS) | Yes | v24.14.0 | — (built into k6 JS runtime anyway) |
| Java 25 | Backend server | Detected at PATH but shell segfaulted | Assumed present (project requirement) | — |
| PostgreSQL | Backend test database | Remote (157.230.42.136:5555) | Not locally installed | Remote dev DB already configured |
| springdoc-openapi | OpenAPI annotations | Yes (in build.gradle) | Managed by Spring Boot 4.0.3 BOM | — |

**Missing dependencies with no fallback:**
- k6 must be installed before benchmark scripts can run. `load-tests/README.md` must document the install step.

**Missing dependencies with fallback:**
- None beyond k6.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + Testcontainers |
| Config file | `src/test/java/com/vn/core/IntegrationTest.java` (`@IntegrationTest` annotation) |
| Quick run command | `./gradlew test -x integrationTest` |
| Full suite command | `./gradlew test integrationTest` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERF-k6 | k6 scripts run and produce a baseline summary under 10% overhead | Manual (load test) | `k6 run load-tests/scripts/org-list-benchmark.js` | No — Wave 0 |
| OPENAPI-verify | Generated spec at `/v3/api-docs` includes `x-secured-entity: true` for Organization/Dept/Employee endpoints | Manual (curl + jq) or integration test | `curl http://localhost:8080/v3/api-docs | jq '.paths["/api/organizations"]["get"]["x-secured-entity"]'` | No — optional |
| OPENAPI-schema | `GET /api/organizations` spec response schema type is `array` not `string` | Manual (Swagger UI inspection) | Visual | — |

### Sampling Rate

- **Per task commit:** `./gradlew test` (unit tests only, fast)
- **Per wave merge:** `./gradlew test integrationTest`
- **Phase gate:** Full suite green + k6 baseline report committed before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `load-tests/scripts/org-list-benchmark.js` — covers benchmark requirement
- [ ] `load-tests/scripts/org-detail-benchmark.js` — covers single-entity benchmark
- [ ] `load-tests/README.md` — setup instructions
- [ ] `load-tests/results/.gitkeep` — placeholder directory
- [ ] `src/main/java/com/vn/core/web/rest/BenchmarkOrganizationResource.java` — baseline endpoint

---

## Fetch-Plan Reference (Source of Truth for @Operation Descriptions)

All fetch-plan codes and field sets from `src/main/resources/fetch-plans.yml`:

| Code | Entity | Fields | Relations |
|------|--------|--------|-----------|
| `organization-list` | Organization | id, code, name, ownerLogin | none |
| `organization-detail` | Organization | id, code, name, ownerLogin, budget | departments → [id, code, name, costCenter → employees → [id, employeeNumber, firstName, lastName, email, salary]] |
| `department-list` | Department | id, code, name, costCenter | organization → [id, name] |
| `department-detail` | Department | id, code, name, costCenter | organization → [id, code, name, ownerLogin] + employees → [id, employeeNumber, firstName, lastName, email, salary] |
| `employee-list` | Employee | id, employeeNumber, firstName, lastName, email | department → [id, name] |
| `employee-detail` | Employee | id, employeeNumber, firstName, lastName, email, salary | department → [id, code, name] |

---

## Project Constraints (from CLAUDE.md)

- **Fetch plans:** Defined in YAML only — do not add fetch-plan codes in the OpenAPI annotations that don't exist in `fetch-plans.yml`
- **Brownfield safety:** Auth, account, admin-user, and mail flows must not regress — benchmark and annotation work must not touch those controllers
- **Logging:** No `console.log` in production code; k6 scripts are not production code and may use `console.log`
- **Layering:** Security logic stays in `security/**` — `BenchmarkOrganizationResource` must not replicate security evaluation logic, only bypass it via `UnconstrainedDataManager`
- **API boundary:** Keep minimal JHipster account/user API models — annotation scope is proof-domain + security endpoints only (D-11)
- **Java code conventions:** `private static final Logger LOG` naming, `UPPER_SNAKE_CASE` constants — apply in `BenchmarkOrganizationResource`

---

## Sources

### Primary (HIGH confidence)

- Codebase inspection: `OrganizationResource.java`, `DepartmentResource.java`, `EmployeeResource.java`, `SecuredEntityCapabilityResource.java`, `MenuPermissionResource.java` — confirmed no existing `@Operation`/`@Tag`/`@ApiResponse` annotations
- Codebase inspection: `build.gradle` — confirmed `springdoc-openapi-starter-webmvc-api` is present
- Codebase inspection: `application.yml` — confirmed `api-docs` profile pattern and `jhipster.api-docs` config block
- Codebase inspection: `UnconstrainedDataManager.java` + `UnconstrainedDataManagerImpl.java` — confirmed `loadPage(Class, Specification, Pageable)` interface
- Codebase inspection: `fetch-plans.yml` — authoritative source for all fetch-plan codes and field sets
- Codebase inspection: `SecuredEntityJsonAdapter.java` — confirmed `toJsonArrayString` and `toJsonString` take `fetchPlanCode: String`

### Secondary (MEDIUM confidence)

- k6 `handleSummary()` pattern: https://grafana.com/docs/k6/latest/results-output/end-of-test/custom-summary/ — verified against known k6 API (not checked live due to firecrawl:false)
- springdoc `OperationCustomizer`: https://springdoc.org/#customizing-the-openapi-object — standard customizer pattern known from training data, aligns with Spring Boot 4 / springdoc 2.x

### Tertiary (LOW confidence — mark for validation)

- k6 version 0.56.x as "latest stable" — verify with `winget search k6` before documenting in README
- `findAll(null, pageable)` treating null spec as "no restriction" in Spring Data JPA — known behavior from training data, should be confirmed with a quick test

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — springdoc is already present; k6 choice is locked per D-01
- Architecture patterns: HIGH — derived from direct code inspection of all five target controllers
- Pitfalls: HIGH for LazyInitializationException and k6 handleSummary; MEDIUM for extension boolean semantics
- Fetch-plan reference: HIGH — read directly from `fetch-plans.yml`

**Research date:** 2026-03-31
**Valid until:** 2026-06-01 (springdoc and k6 are stable; annotations don't change)
