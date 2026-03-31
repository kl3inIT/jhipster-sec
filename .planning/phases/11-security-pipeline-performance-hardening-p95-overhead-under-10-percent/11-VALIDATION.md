---
phase: 11
slug: security-pipeline-performance-hardening-p95-overhead-under-10-percent
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-31
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Testcontainers PostgreSQL |
| **Config file** | `src/test/java/com/vn/core/` |
| **Quick run command** | `./gradlew test --tests "com.vn.core.security.*" -x integrationTest` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~90 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "com.vn.core.security.*" -x integrationTest`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-01-00 | 01 | 0 | PERF-04 | unit | `./gradlew test --tests "com.vn.core.security.permission.RequestPermissionSnapshotTest" --tests "com.vn.core.service.security.SecPermissionServiceTest"` | ❌ W0 | ⬜ pending |
| 11-01-01 | 01 | 1 | PERF-04 | unit | `./gradlew test --tests "com.vn.core.security.permission.RequestPermissionSnapshotTest" --tests "com.vn.core.security.permission.RolePermissionServiceDbImplTest"` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | PERF-04 | integration | `./gradlew integrationTest --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"` | ✅ | ⬜ pending |
| 11-02-00 | 02 | 0 | PERF-04 | unit | `./gradlew test --tests "com.vn.core.security.access.AccessManagerImplTest" --tests "com.vn.core.security.web.SecuredEntityJsonAdapterTest"` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 1 | PERF-04 | unit+integration | `./gradlew test --tests "com.vn.core.security.web.SecuredEntityJsonAdapterTest" --tests "com.vn.core.security.serialize.SecureEntitySerializerImplTest" && ./gradlew integrationTest --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"` | ❌ W0 | ⬜ pending |
| 11-02-02 | 02 | 1 | PERF-04 | unit+benchmark | `./gradlew test --tests "com.vn.core.security.access.AccessManagerImplTest" && k6 run load-tests/scripts/org-list-benchmark.js && k6 run --env ORG_ID=1501 load-tests/scripts/org-detail-benchmark.js` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/security/permission/RequestPermissionSnapshotTest.java` — focused coverage for JWT-authority trust and PermissionMatrix cache reuse
- [ ] `src/test/java/com/vn/core/service/security/SecPermissionServiceTest.java` — focused coverage for service-layer cache eviction on create, update, and delete
- [ ] `src/test/java/com/vn/core/security/access/AccessManagerImplTest.java` — focused coverage for constructor-time constraint sorting preserving getOrder sequence
- [ ] `src/test/java/com/vn/core/security/web/SecuredEntityJsonAdapterTest.java` — focused coverage for one fetch-plan resolution per detail/list response call

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| p95 latency overhead < 10% vs baseline | PERF-04 | Requires JMeter/k6 load test run | Run load test against `/api/organizations` (secured) and compare p95 vs Phase 10 baseline measurements |
| Cache hit rate visible in actuator | PERF-04 | Metric inspection | Call `/actuator/caches` and verify `sec-permission-matrix` map exists with non-zero hit count |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
