---
phase: 4
slug: protected-entity-proof
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Testcontainers |
| **Config file** | `src/test/resources/config/application.yml` |
| **Quick run command** | `./gradlew test --tests "*.SecureEntity*"` |
| **Full suite command** | `./gradlew test integrationTest` |
| **Estimated runtime** | ~90 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "*.SecureEntity*"`
- **After every plan wave:** Run `./gradlew test integrationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | ENT-01 | integration | `./gradlew test --tests "*.SecureEntityIT"` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | ENT-01 | integration | `./gradlew test --tests "*.SecureEntityIT"` | ❌ W0 | ⬜ pending |
| 4-02-01 | 02 | 2 | ENT-02 | integration | `./gradlew test --tests "*.SecureEntityIT"` | ❌ W0 | ⬜ pending |
| 4-02-02 | 02 | 2 | ENT-02 | integration | `./gradlew test --tests "*.SecureEntityIT"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/security/SecureEntityIT.java` — stubs for ENT-01, ENT-02
- [ ] `src/test/java/com/vn/core/config/DatabaseTestcontainer.java` — already exists, reused
- [ ] Seed `jhi_authority` rows for all test roles used in `@WithMockUser` fixtures

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| API returns 403 for deny path | ENT-02 | HTTP status verification via MockMvc | Run `SecureEntityIT` deny-path tests and confirm 403 response |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
