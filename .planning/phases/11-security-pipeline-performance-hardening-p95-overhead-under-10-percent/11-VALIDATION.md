---
phase: 11
slug: security-pipeline-performance-hardening-p95-overhead-under-10-percent
status: draft
nyquist_compliant: false
wave_0_complete: false
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
| 11-01-01 | 01 | 0 | PERF-04 | unit | `./gradlew test --tests "*.RequestPermissionSnapshotTest"` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | PERF-04 | unit | `./gradlew test --tests "*.SecPermissionServiceTest"` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 1 | PERF-04 | unit | `./gradlew test --tests "*.AccessManagerImplTest"` | ❌ W0 | ⬜ pending |
| 11-03-01 | 03 | 1 | PERF-04 | unit | `./gradlew test --tests "*.SecureDataManagerImplTest"` | ✅ | ⬜ pending |
| 11-04-01 | 04 | 1 | PERF-04 | unit | `./gradlew test --tests "*.SecuredEntityJsonAdapterTest"` | ❌ W0 | ⬜ pending |
| 11-05-01 | 05 | 2 | PERF-04 | integration | `./gradlew test --tests "*.SecureEntitySerializerImplTest"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/security/RequestPermissionSnapshotTest.java` — stubs for PERF-04 permission matrix caching
- [ ] `src/test/java/com/vn/core/security/SecPermissionServiceTest.java` — stubs for cache eviction on write paths
- [ ] `src/test/java/com/vn/core/security/AccessManagerImplTest.java` — stubs for authority validation caching
- [ ] `src/test/java/com/vn/core/security/SecuredEntityJsonAdapterTest.java` — stubs for BeanWrapper → Jackson swap

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
