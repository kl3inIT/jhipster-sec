---
phase: 2
slug: security-metadata-management
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-21
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Testcontainers |
| **Config file** | `build.gradle` / `src/test/resources/config/application.yml` |
| **Quick run command** | `./gradlew test --tests "com.vn.core.*" -x integrationTest` |
| **Full suite command** | `./gradlew test integrationTest` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "com.vn.core.*" -x integrationTest`
- **After every plan wave:** Run `./gradlew test integrationTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 2-01-T1 | 01 | 1 | SEC-01/02/03 | compile | `grep -c "20260321000" src/main/resources/config/liquibase/master.xml && ./gradlew compileJava 2>&1 \| tail -5` | pending |
| 2-01-T2 | 01 | 1 | SEC-01 | compile | `grep -c "displayName\|RoleType" src/main/java/com/vn/core/domain/Authority.java 2>/dev/null` | pending |
| 2-01-T3 | 01 | 1 | SEC-01/02/03 | unit | `./gradlew test --tests "com.vn.core.TechnicalStructureTest" 2>&1 \| tail -5` | pending |
| 2-02-T1 | 02 | 2 | SEC-01/02/03 | compile | `./gradlew compileJava 2>&1 \| tail -5` | pending |
| 2-02-T2 | 02 | 2 | SEC-01 | compile | `./gradlew compileJava 2>&1 \| tail -5` | pending |
| 2-02-T3 | 02 | 2 | SEC-01 | unit+IT | `./gradlew test --tests "com.vn.core.security.bridge.*" 2>&1 \| tail -10` | pending |
| 2-03-T1 | 03 | 3 | SEC-01 | compile | `./gradlew compileJava 2>&1 \| tail -5` | pending |
| 2-03-T2 | 03 | 3 | SEC-01/02/03 | compile+arch | `./gradlew compileJava 2>&1 \| tail -5 && ./gradlew test --tests "com.vn.core.TechnicalStructureTest" 2>&1 \| tail -5` | pending |
| 2-04-T1 | 04 | 4 | SEC-01 | integration | `./gradlew integrationTest --tests "com.vn.core.web.rest.admin.security.SecRoleAdminResourceIT" 2>&1 \| tail -10` | pending |
| 2-04-T2 | 04 | 4 | SEC-02/03 | integration | `./gradlew test integrationTest 2>&1 \| tail -15` | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

All automated verify commands use existing test infrastructure or compile checks. No Wave 0 test stubs are needed because:

- Plans 01-03 verify via `./gradlew compileJava` and `TechnicalStructureTest` (both pre-existing)
- Plan 02 Task 3 creates `MergedSecurityContextBridgeTest` and updates `SecurityContextBridgeWiringIT` (test creation IS the task)
- Plan 04 creates `SecRoleAdminResourceIT`, `SecPermissionAdminResourceIT`, `SecRowPolicyAdminResourceIT` (test creation IS the task, verified by running them)

No external test scaffolds need to be created before execution begins.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Liquibase migration runs on fresh DB | SEC-01/02/03 | Schema evolution requires DB start | Start app with `./gradlew bootRun -Pdev`, verify tables `sec_permission`, `sec_row_policy` created, `display_name`/`type` added to `jhi_authority` |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify commands
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] No Wave 0 test stubs needed (compile checks and test-creation tasks cover all)
- [x] No watch-mode flags
- [x] Feedback latency < 120s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready
