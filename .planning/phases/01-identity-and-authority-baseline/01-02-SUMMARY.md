---
phase: 01-identity-and-authority-baseline
plan: 02
subsystem: security
tags: [spring-security, security-bridge, integration-point, phase-seam, jhipster]

requires:
  - "01-01: Regression test baseline for existing auth flows"

provides:
  - "SecurityContextBridge interface at com.vn.core.security.bridge (SEC-04)"
  - "JHipsterSecurityContextBridge @Component default implementation backed by SecurityUtils"
  - "Phase 2 substitution seam: override by declaring a @Primary SecurityContextBridge bean"

affects:
  - "Phase 2: replaces JHipsterSecurityContextBridge with its own @Primary implementation"
  - "Any service needing current-user authorities can inject SecurityContextBridge instead of calling SecurityUtils directly"

tech-stack:
  added: []
  patterns:
    - "SecurityContextBridge pattern: interface + default @Component allows Phase 2 @Primary override without touching Phase 1 code"
    - "Bridge delegates to SecurityUtils.getCurrentUserLogin() and SecurityContextHolder for authority extraction"
    - "Unit tests use SecurityContextHolder.createEmptyContext() + UsernamePasswordAuthenticationToken (matches SecurityUtilsUnitTest pattern)"

key-files:
  created:
    - "src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java"
    - "src/main/java/com/vn/core/security/bridge/JHipsterSecurityContextBridge.java"
    - "src/test/java/com/vn/core/security/bridge/JHipsterSecurityContextBridgeTest.java"
    - "src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java"
  modified: []

key-decisions:
  - "JHipsterSecurityContextBridge uses @Component (not @Primary) so Phase 2 can provide @Primary override without modifying Phase 1 code"
  - "Bridge interface exposes Collection<String> raw authority names only — no typed AuthorityDescriptor per D-01/D-07"
  - "Bridge package com.vn.core.security.bridge is a sub-package of ..security.. which satisfies ArchUnit Security layer rule"

requirements-completed: [SEC-04]

duration: 4min
completed: 2026-03-21
---

# Phase 01 Plan 02: SecurityContextBridge Summary

**SecurityContextBridge interface and JHipsterSecurityContextBridge default @Component delivered as the Phase 2 engine substitution seam, backed by SecurityUtils delegation with 6 unit tests and a wiring IT passing green.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T07:39:50Z
- **Completed:** 2026-03-21T07:43:50Z
- **Tasks:** 2 of 2
- **Files created:** 4

## Accomplishments

- Created `SecurityContextBridge` interface with three methods: `getCurrentUserLogin()`, `getCurrentUserAuthorities()`, `isAuthenticated()`
- Created `JHipsterSecurityContextBridge` as a plain `@Component` (no `@Primary`) delegating to `SecurityUtils` and `SecurityContextHolder`
- 6 unit tests in `JHipsterSecurityContextBridgeTest` cover all three methods for both authenticated and unauthenticated states
- Wiring IT `SecurityContextBridgeWiringIT` proves the bean auto-wires in the full application context as the correct type
- Full test suite (`./gradlew test integrationTest`) passes green — no regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: SecurityContextBridge interface and JHipsterSecurityContextBridge implementation** - `50f0f45` (feat)
2. **Task 2: SecurityContextBridgeWiringIT integration test** - `1209891` (test)

## Files Created/Modified

- `src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java` - Interface with three methods
- `src/main/java/com/vn/core/security/bridge/JHipsterSecurityContextBridge.java` - Default @Component implementation
- `src/test/java/com/vn/core/security/bridge/JHipsterSecurityContextBridgeTest.java` - 6 unit tests (runs in `./gradlew test`)
- `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` - Wiring IT (runs in `./gradlew integrationTest`)

## Deviations from Plan

None — plan executed exactly as written.

Note: `./gradlew check -x webpack` was attempted but failed because (a) the `webpack` task doesn't exist in this backend-only project, and (b) `spotlessJavaCheck` has a pre-existing configuration error unrelated to this plan. Tests were run as `./gradlew test integrationTest` which covers the plan's acceptance criteria fully. The `spotlessJavaCheck` failure is out of scope for this plan (pre-existing issue, not caused by these changes).

## Known Stubs

None.

## Self-Check: PASSED

Files verified:
- `src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java` - FOUND
- `src/main/java/com/vn/core/security/bridge/JHipsterSecurityContextBridge.java` - FOUND
- `src/test/java/com/vn/core/security/bridge/JHipsterSecurityContextBridgeTest.java` - FOUND
- `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` - FOUND

Commits verified:
- `50f0f45` - FOUND
- `1209891` - FOUND
