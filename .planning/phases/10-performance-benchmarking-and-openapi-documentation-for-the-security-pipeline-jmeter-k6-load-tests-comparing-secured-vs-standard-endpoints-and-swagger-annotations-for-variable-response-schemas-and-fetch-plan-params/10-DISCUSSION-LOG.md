
# Phase 10: Performance Benchmarking and OpenAPI Documentation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-31
**Phase:** 10 - Performance Benchmarking and OpenAPI Documentation for the Security Pipeline
**Areas discussed:** Load test tooling, OpenAPI schema strategy for secured endpoints, Fetch-plan param documentation, Benchmark scope and pass/fail threshold

---

## Load Test Tooling

| Option | Description | Selected |
|--------|-------------|----------|
| k6 | JavaScript-based, runs on Windows without JVM, clean CLI output, easy to read scripts. No Gradle plugin needed — scripts live in a test/ or load-tests/ directory. | ✓ |
| JMeter | Java-based, has Gradle plugins, XML .jmx files. Heavyweight but familiar to Java teams. | |
| Gatling | Scala DSL, has a Gradle plugin, good for JVM teams. | |

**User's choice:** k6

| Script Location | Description | Selected |
|-----------------|-------------|----------|
| src/test/load/ | Keeps load tests alongside other test types | |
| load-tests/ at project root | Separate top-level directory — clearer isolation from unit/integration tests | ✓ |
| You decide | Claude picks | |

**User's choice:** `load-tests/` at project root

| k6 Output | Description | Selected |
|-----------|-------------|----------|
| Markdown report in load-tests/results/ | --summary-export JSON + summary.md per run, committed as baseline snapshot | ✓ |
| Console output only | Lightweight but no baseline for comparison | |
| You decide | Claude picks | |

**User's choice:** Markdown report in `load-tests/results/`

---

## Benchmark Scope and Pass/Fail Threshold

| Endpoints | Description | Selected |
|-----------|-------------|----------|
| Organization GET list + GET single | Top-level proof-domain entity, full pipeline coverage | ✓ |
| Organization + Employee GET list | Two entities at different hierarchy levels | |
| You decide | Claude picks | |

**User's choice:** Organization GET list + GET single

| Concurrency Levels | Description | Selected |
|--------------------|-------------|----------|
| 1, 10, 50 VUs | Covers baseline, moderate, and stress load | ✓ |
| 1, 10, 50, 100 VUs | Adds high-stress level | |
| 1, 10 VUs only | Minimal | |

**User's choice:** 1, 10, 50 virtual users

| Threshold | Description | Selected |
|-----------|-------------|----------|
| <20% p95 overhead | Todo's suggested target | |
| <10% p95 overhead | Stricter target — near-zero overhead guarantee | ✓ |
| No threshold — document delta only | Observability only | |

**User's choice:** <10% p95 latency overhead (tightened from the original todo suggestion of <20%)

| Test Setup | Description | Selected |
|------------|-------------|----------|
| README in load-tests/ with setup steps | Manual execution, no CI integration | ✓ |
| Gradle task to run k6 | Wire k6 as Gradle exec task | |
| You decide | Claude picks | |

**User's choice:** `load-tests/README.md` with setup steps — manual execution only

---

## OpenAPI Schema Strategy for Secured Endpoints

| Schema Strategy | Description | Selected |
|-----------------|-------------|----------|
| Full schema + permission note | Document all fields, note attribute filtering in @Operation description | ✓ |
| Custom x-secured-entity extension | Add x-secured-entity: true extension tag (can be combined) | |
| One schema per role | Per-role @ApiResponse variants — hard to maintain | |

**User's choice:** Full schema + permission note

| Endpoint Marking | Description | Selected |
|------------------|-------------|----------|
| Yes — x-secured-entity extension tag | Add x-secured-entity: true to @SecuredEntity-backed operation metadata | ✓ |
| No — operation description is enough | Just note in @Operation text | |

**User's choice:** Both full schema + permission note AND custom `x-secured-entity: true` extension

| Annotation Scope | Description | Selected |
|------------------|-------------|----------|
| Security + proof-domain endpoints only | /api/security/**, /api/organizations/**, /api/departments/**, /api/employees/** | ✓ |
| All project-custom endpoints | Everything added by this project including user/authority/menu resources | |
| Security endpoints only | Only /api/security/**, proof-domain deferred | |

**User's choice:** Security + proof-domain endpoints only

---

## Fetch-Plan Param Documentation

| Approach | Description | Selected |
|----------|-------------|----------|
| @Operation description only | Note fetch-plan code in endpoint description, no API change | ✓ |
| Expose ?fetchPlan query param | Let callers override fetch plan, document valid values | |
| You decide | Claude picks | |

**User's choice:** `@Operation` description only — no API contract change

---

## Claude's Discretion

- Exact k6 script structure (shared auth setup, thresholds format, sleep intervals)
- Whether baseline endpoint uses existing `UnconstrainedDataManager` path or needs a test-only bypass
- Exact `@ApiResponse` content type for `ResponseEntity<String>` endpoints
- springdoc configuration for `x-secured-entity` extension in generated spec output

## Deferred Ideas

- `?fetchPlan=` query parameter exposure — deferred, no API change in Phase 10
- CI integration for k6 — deferred, manual only
- Annotation coverage for legacy JHipster admin/user/account endpoints — out of scope
- Per-role schema variants — rejected in favor of full-schema + permission note approach
