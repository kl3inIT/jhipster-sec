---
phase: 3
slug: secure-enforcement-core
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) + Mockito + AssertJ |
| **Config file** | `build.gradle` (via Spring Boot 4.0.3 BOM; `integrationTest` task in `gradle/spring-boot.gradle`) |
| **Quick run command** | `./gradlew test` |
| **Full suite command** | `./gradlew test integrationTest` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test`
- **After every plan wave:** Run `./gradlew test integrationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 3-W0-01 | Wave 0 | 0 | DATA-01 | unit | `./gradlew test --tests "*.SecureDataManagerImplTest"` | ❌ W0 | ⬜ pending |
| 3-W0-02 | Wave 0 | 0 | DATA-01 | unit | `./gradlew test --tests "*.UnconstrainedDataManagerImplTest"` | ❌ W0 | ⬜ pending |
| 3-W0-03 | Wave 0 | 0 | DATA-04 | unit | `./gradlew test --tests "*.SecureMergeServiceImplTest"` | ❌ W0 | ⬜ pending |
| 3-W0-04 | Wave 0 | 0 | DATA-05 | unit | `./gradlew test --tests "*.RowLevelSpecificationBuilderTest"` | ❌ W0 | ⬜ pending |
| 3-W0-05 | Wave 0 | 0 | DATA-05 | unit | `./gradlew test --tests "*.RowLevelPolicyProviderDbImplTest"` | ❌ W0 | ⬜ pending |
| 3-W0-06 | Wave 0 | 0 | DATA-01..05 | integration | `./gradlew integrationTest --tests "*.SecureDataManagerIT"` | ❌ W0 | ⬜ pending |
| 3-W0-07 | Wave 0 | 0 | DATA-02 | unit | `./gradlew test --tests "*.YamlFetchPlanRepositoryTest"` | ❌ W0 (port) | ⬜ pending |
| 3-W0-08 | Wave 0 | 0 | DATA-02 | unit | `./gradlew test --tests "*.FetchPlanBuilderTest"` | ❌ W0 (port) | ⬜ pending |
| 3-W0-09 | Wave 0 | 0 | DATA-03 | unit | `./gradlew test --tests "*.SecureEntitySerializerImplTest"` | ❌ W0 (port) | ⬜ pending |
| 3-W0-10 | Wave 0 | 0 | DATA-03 | unit | `./gradlew test --tests "*.AttributePermissionEvaluatorImplTest"` | ❌ W0 (port) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java` — stubs for DATA-01 enforcement order
- [ ] `src/test/java/com/vn/core/security/data/UnconstrainedDataManagerImplTest.java` — stubs for DATA-01 bypass
- [ ] `src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java` — stubs for DATA-04
- [ ] `src/test/java/com/vn/core/security/row/RowLevelSpecificationBuilderTest.java` — stubs for DATA-05 composition
- [ ] `src/test/java/com/vn/core/security/row/RowLevelPolicyProviderDbImplTest.java` — stubs for DATA-05 fail-closed
- [ ] `src/test/java/com/vn/core/security/data/SecureDataManagerIT.java` — integration stubs for DATA-01 through DATA-05 on PostgreSQL
- [ ] Port `angapp/src/test/.../fetch/YamlFetchPlanRepositoryTest.java` → `src/test/java/com/vn/core/security/fetch/`
- [ ] Port `angapp/src/test/.../fetch/FetchPlanBuilderTest.java` → `src/test/java/com/vn/core/security/fetch/`
- [ ] Port `angapp/src/test/.../serialize/SecureEntitySerializerImplTest.java` → `src/test/java/com/vn/core/security/serialize/`
- [ ] Port `angapp/src/test/.../permission/AttributePermissionEvaluatorImplTest.java` → `src/test/java/com/vn/core/security/permission/`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Row policy expression with `{login}` token resolves to the actual logged-in user in a live HTTP context | DATA-05 | Requires a full request context with an authenticated JWT; Testcontainer IT covers the query path but not the token interpolation via the HTTP layer | Start app with dev profile, authenticate via `/api/authenticate`, call a secured entity list endpoint, verify response excludes records not matching the policy expression |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
