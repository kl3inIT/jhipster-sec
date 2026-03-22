---
phase: 05-standalone-frontend-delivery
plan: 11
subsystem: ui
tags: [angular, auth, capability-cache, logout, session]

requires:
  - phase: 05-standalone-frontend-delivery
    provides: Angular frontend with AccountService, AuthServerProvider, SecuredEntityCapabilityService

provides:
  - Direct /login navigation on logout without intermediate 401 round-trips
  - Auth-aware capability cache that invalidates on every auth state change

affects: [05-12]

tech-stack:
  added: []
  patterns:
    - Subscribe AccountService.getAuthenticationState() in root-scoped service constructors to react to auth lifecycle events
    - Navigate to explicit /login path on logout rather than the guarded home route

key-files:
  created: []
  modified:
    - frontend/src/app/layout/component/topbar/app.topbar.ts
    - frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts

key-decisions:
  - "Navigate to /login explicitly in logout() to skip two 401 round-trips caused by navigating to guarded home route"
  - "Subscribe to getAuthenticationState() in SecuredEntityCapabilityService constructor to clear cachedCapabilities$ on every auth state change"

patterns-established:
  - "Root-scoped services that hold per-user caches must subscribe to AccountService.getAuthenticationState() and reset their cache on each emission"

requirements-completed: [AUTH-01, ENT-03]

duration: 8min
completed: 2026-03-23
---

# Phase 05 Plan 11: Logout Nav Fix and Auth-Aware Capability Cache

**Fixed logout 401 lag (direct /login routing) and stale capability cache across user sessions (reset on auth state change).**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-23T11:44:53Z
- **Completed:** 2026-03-23T11:52:00Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments

### Fix A: Logout Navigation Lag

`AppTopbar.logout()` previously navigated to `['']` (guarded home), causing two sequential 401 round-trips before the user landed on `/login`. Changed to `router.navigate(['/login'])` for direct, immediate navigation after clearing auth.

### Fix D: Stale Capability Cache Across User Sessions

`SecuredEntityCapabilityService` used `shareReplay(1)` but never invalidated the cache across user sessions. Added `AccountService` injection and a constructor subscription to `getAuthenticationState()` that sets `cachedCapabilities$ = undefined` on every emission (login, logout, identity change). The next `query()` call after any auth state change fetches fresh data for the current user.

## Commits

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Fix logout nav and capability cache reset | 1acb732 | app.topbar.ts, secured-entity-capability.service.ts |

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

- `frontend/src/app/layout/component/topbar/app.topbar.ts`: modified, contains `router.navigate(['/login'])`
- `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`: modified, contains `getAuthenticationState().subscribe`
- Commit `1acb732` exists in git log
- Production build passed (no compilation errors)
