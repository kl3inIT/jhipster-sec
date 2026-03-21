---
phase: 1
slug: identity-and-authority-baseline
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + MockMvc + Testcontainers (PostgreSQL) |
| **Config file** | `src/test/java/com/vn/core/config/DatabaseTestcontainer.java` |
| **Quick run command** | `./gradlew test --tests "com.vn.core.security.*"` |
| **Full suite command** | `./gradlew test integrationTest` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "com.vn.core.security.*"`
- **After every plan wave:** Run `./gradlew test integrationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 0 | AUTH-02 | integration | `./gradlew integrationTest --tests "*.AccountResourceIT"` | ✅ existing | ⬜ pending |
| 1-01-02 | 01 | 0 | AUTH-02 | integration | `./gradlew integrationTest --tests "*.AuthenticateControllerIT"` | ✅ existing | ⬜ pending |
| 1-01-03 | 01 | 0 | AUTH-03 | integration | `./gradlew integrationTest --tests "*.UserResourceIT"` | ✅ existing | ⬜ pending |
| 1-01-04 | 01 | 0 | AUTH-03 | integration | `./gradlew integrationTest --tests "*.AuthorityResourceIT"` | ❌ W0 | ⬜ pending |
| 1-02-01 | 02 | 1 | SEC-04 | unit | `./gradlew test --tests "*.JHipsterSecurityContextBridgeTest"` | ❌ W0 | ⬜ pending |
| 1-02-02 | 02 | 1 | SEC-04 | integration | `./gradlew integrationTest --tests "*.SecurityContextBridgeWiringIT"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java` — integration tests for authority listing endpoint (AUTH-03)
- [ ] `src/test/java/com/vn/core/security/bridge/JHipsterSecurityContextBridgeTest.java` — unit tests stub for SecurityContextBridge (SEC-04)
- [ ] `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` — Spring wiring integration test stub (SEC-04)
- [ ] Extend `AccountResourceIT` with wrong-old-password test case (AUTH-02 gap identified in research)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Mail delivery on account activation/password reset | AUTH-02 | Email sending requires SMTP mock or manual inspection | Start app with dev profile, trigger activation email, verify receipt |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
