---
phase: 10
slug: performance-benchmarking-and-openapi-documentation-for-the-security-pipeline-jmeter-k6-load-tests-comparing-secured-vs-standard-endpoints-and-swagger-annotations-for-variable-response-schemas-and-fetch-plan-params
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-31
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Testcontainers (automated) + k6 + curl/jq (manual) |
| **Config file** | `src/test/java/com/vn/core/IntegrationTest.java` |
| **Quick run command** | `./gradlew test -x integrationTest` |
| **Full suite command** | `./gradlew test integrationTest` |
| **Estimated runtime** | ~3 minutes (unit) / ~8 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test -x integrationTest`
- **After every plan wave:** Run `./gradlew test integrationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds (unit), 480 seconds (full)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 10-k6-setup | 01 | 1 | D-01/D-02 | manual | `ls load-tests/scripts/*.js` | ❌ W0 | ⬜ pending |
| 10-k6-benchmark | 01 | 2 | D-05/D-06/D-07 | manual | `k6 run load-tests/scripts/org-list-benchmark.js` | ❌ W0 | ⬜ pending |
| 10-k6-report | 01 | 2 | D-03/D-08 | manual | `ls load-tests/results/summary.md` | ❌ W0 | ⬜ pending |
| 10-openapi-orgs | 02 | 1 | D-09/D-10/D-11 | integration | `./gradlew integrationTest -Dtest=OrganizationResourceIT` | ✅ | ⬜ pending |
| 10-openapi-depts | 02 | 1 | D-11 | integration | `./gradlew integrationTest -Dtest=DepartmentResourceIT` | ✅ | ⬜ pending |
| 10-openapi-emps | 02 | 1 | D-11 | integration | `./gradlew integrationTest -Dtest=EmployeeResourceIT` | ✅ | ⬜ pending |
| 10-openapi-security | 02 | 2 | D-11 | integration | `./gradlew integrationTest -Dtest=SecuredEntityCapabilityResourceIT` | ✅ | ⬜ pending |
| 10-x-secured-ext | 02 | 2 | D-10 | manual | `curl http://localhost:8080/v3/api-docs \| jq '.paths["/api/organizations"]["get"]["x-secured-entity"]'` | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `load-tests/scripts/org-list-benchmark.js` — k6 benchmark script stub
- [ ] `load-tests/README.md` — setup/run instructions
- [ ] `load-tests/results/.gitkeep` — results directory

*k6 install: `winget install k6` — must be documented in README, not automated.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| k6 p95 overhead < 10% | D-08 | Load test requires running backend + seeded DB | Start backend with `api-docs` profile, seed data, run `k6 run load-tests/scripts/org-list-benchmark.js` |
| Swagger UI shows `x-secured-entity` badge | D-10 | Visual inspection of rendered spec | Open `/swagger-ui.html`, check Organization GET endpoints show extension |
| `GET /api/organizations` response schema is `array` | D-09 | Swagger UI or curl spec check | `curl .../v3/api-docs \| jq '.paths["/api/organizations"]["get"].responses["200"].content["application/json"].schema.type'` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 480s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
