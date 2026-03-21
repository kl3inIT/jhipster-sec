---
phase: 05-standalone-frontend-delivery
plan: 02
subsystem: frontend
tags: [angular, primeng, tailwind, auth, routing, layout]
dependency_graph:
  requires: []
  provides: [frontend-app-shell, auth-core, layout, login-page, error-pages, dashboard-stub]
  affects: [05-03, 05-04, 05-05]
tech_stack:
  added:
    - Angular 21 standalone app under frontend/
    - PrimeNG 21 with @primeuix/themes Aura preset
    - Tailwind CSS 3.4 with PostCSS
    - PrimeFlex 4, PrimeIcons 7
    - NgBootstrap 20
    - ngx-translate 17
    - dayjs 1.11
  patterns:
    - Vitest-based unit tests via @angular/build:unit-test
    - Signal-based LayoutService for dark mode and menu state
    - CanActivateFn route guard (UserRouteAccessService)
    - JWT auth via AuthServerProvider posting to /api/authenticate
    - HTTP interceptors for Bearer token injection, 401 expiry, error handling
    - Lazy-loaded routes with loadComponent/loadChildren
key_files:
  created:
    - frontend/package.json
    - frontend/angular.json
    - frontend/tsconfig.json
    - frontend/tsconfig.app.json
    - frontend/proxy.conf.json
    - frontend/tailwind.config.js
    - frontend/src/main.ts
    - frontend/src/app.config.ts
    - frontend/src/app.routes.ts
    - frontend/src/app/core/auth/account.model.ts
    - frontend/src/app/core/auth/account.service.ts
    - frontend/src/app/core/auth/auth-jwt.service.ts
    - frontend/src/app/core/auth/state-storage.service.ts
    - frontend/src/app/core/auth/user-route-access.service.ts
    - frontend/src/app/core/interceptor/auth.interceptor.ts
    - frontend/src/app/core/interceptor/auth-expired.interceptor.ts
    - frontend/src/app/core/interceptor/error-handler.interceptor.ts
    - frontend/src/app/core/interceptor/notification.interceptor.ts
    - frontend/src/app/core/interceptor/index.ts
    - frontend/src/app/core/config/application-config.service.ts
    - frontend/src/app/core/request/request-util.ts
    - frontend/src/app/config/authority.constants.ts
    - frontend/src/app/shared/sort/sort.service.ts
    - frontend/src/app/shared/sort/sort-state.ts
    - frontend/src/app/layout/service/layout.service.ts
    - frontend/src/app/layout/component/main/app.layout.ts
    - frontend/src/app/layout/component/menu/app.menu.ts
    - frontend/src/app/layout/component/menu/app.menuitem.ts
    - frontend/src/app/layout/component/menu/app.menu.spec.ts
    - frontend/src/app/layout/component/topbar/app.topbar.ts
    - frontend/src/app/layout/component/topbar/app.topbar.html
    - frontend/src/app/layout/component/sidebar/app.sidebar.ts
    - frontend/src/app/layout/component/footer/app.footer.ts
    - frontend/src/app/pages/login/login.component.ts
    - frontend/src/app/pages/login/login.component.html
    - frontend/src/app/pages/home/home.component.ts
    - frontend/src/app/pages/home/home.component.html
    - frontend/src/app/pages/error/access-denied.component.ts
    - frontend/src/app/pages/error/access-denied.component.html
    - frontend/src/app/pages/error/not-found.component.ts
    - frontend/src/app/pages/error/not-found.component.html
    - frontend/src/app/pages/error/error.component.ts
    - frontend/src/app/pages/error/error.component.html
    - frontend/src/app/pages/entities/entity.routes.ts
    - frontend/src/app/pages/admin/security/security.routes.ts
    - frontend/src/assets/styles.scss
    - frontend/src/assets/tailwind.css
    - frontend/src/assets/layout/ (Sakai SCSS layout from aef-main)
    - frontend/src/assets/app/ (app SCSS and flags from aef-main)
    - frontend/src/environments/environment.ts
    - frontend/src/environments/environment.development.ts
decisions:
  - Used vitest (not Karma/ChromeHeadless) for tests — Angular 21 CLI generates vitest by default via @angular/build:unit-test
  - Simplified ErrorHandlerInterceptor and NotificationInterceptor — removed EventManager/AlertService dependencies that are aef-main-specific; will be wired in future plans
  - Topbar built from scratch (not copied from aef-main) — aef-main topbar had dependencies on LoginService, SharedModule, AppConfigurator, LANGUAGES that don't exist yet
  - Page components created in Task 2 (not Task 3) to allow routes to compile incrementally
  - AppMenu.buildMenu() is public (not private) to allow spec access for model inspection
metrics:
  duration: 11 minutes
  completed: 2026-03-21
  tasks_completed: 3
  files_created: 53
---

# Phase 5 Plan 02: Bootstrap Angular Frontend App Shell Summary

Bootstrapped the standalone Angular frontend under `frontend/` with working auth core, sidebar layout, login page, error pages, dashboard stub, and route guards.

## What Was Built

Angular 21 app under `frontend/` with PrimeNG 21 Aura, Tailwind CSS 3, lazy-loaded routing with JWT auth guards, and authority-gated sidebar navigation.

## Tasks Completed

### Task 1: Scaffold Angular app, install deps, configure Tailwind and proxy

Scaffolded Angular 21 app with `npx @angular/cli@21 new frontend`. Installed primeng, @primeuix/themes, primeicons, primeflex, @ng-bootstrap/ng-bootstrap, @ngx-translate/core, @ngx-translate/http-loader, dayjs, tailwindcss, autoprefixer, postcss. Configured tailwind.config.js, proxy.conf.json routing /api/services/management to localhost:8080, angular.json with Sakai SCSS assets and environment file replacements, tsconfig.json with baseUrl paths (app/*, environments/*). Copied Sakai layout SCSS from aef-main/src/assets/layout/ and app SCSS.

**Commit:** `05916ad`

### Task 2: Copy auth core, interceptors, config, request utils, sort utils, and wire app bootstrap

Copied account.model.ts, account.service.ts (identity(), hasAnyAuthority(), getAuthenticationState()), auth-jwt.service.ts (AuthServerProvider posting to /api/authenticate), state-storage.service.ts, user-route-access.service.ts (CanActivateFn). Created auth.interceptor.ts, auth-expired.interceptor.ts, simplified error-handler.interceptor.ts and notification.interceptor.ts. Copied application-config.service.ts (getEndpointFor()), request-util.ts, sort.service.ts, sort-state.ts, authority.constants.ts. Created app.config.ts with providePrimeNG Aura, httpInterceptorProviders, navigation error handler. Created app.routes.ts with lazy layout shell, UserRouteAccessService guard, admin/security route with ROLE_ADMIN data.

**Commit:** `6ceb452`

### Task 3: Copy layout, build login page, error pages, dashboard stub, sidebar menu, and menu spec

Created LayoutService with toggleDarkMode() and signal-based layout config. Created AppLayout, AppTopbar, AppSidebar, AppFooter, AppMenu with project-specific navigation (Home/Dashboard, Entities with Organizations/Departments/Employees, Security Admin with Roles & Permissions/Row Policies gated behind ROLE_ADMIN check). Login component posts to /api/authenticate via AuthServerProvider. Error pages render "Access Denied", "Page Not Found", "Something Went Wrong". Dashboard stub with PrimeNG card. Menu spec tests authority-gated Security Admin section visibility.

**Commit:** `ffc254f`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Created page stubs in Task 2 (not Task 3)**
- **Found during:** Task 2 build — routes referenced page components that did not exist yet
- **Fix:** Created home, login, error page components as part of Task 2 to allow incremental compilation
- **Commit:** `6ceb452`

**2. [Rule 3 - Blocking] Simplified ErrorHandlerInterceptor and NotificationInterceptor**
- **Found during:** Task 2 — aef-main versions depend on EventManager (AlertService) which does not exist in this project yet
- **Fix:** ErrorHandlerInterceptor logs to console.warn; NotificationInterceptor logs app-alert headers — wiring to a full event/alert system is deferred
- **Files modified:** frontend/src/app/core/interceptor/error-handler.interceptor.ts, notification.interceptor.ts

**3. [Rule 3 - Blocking] Topbar built from scratch instead of copied from aef-main**
- **Found during:** Task 3 — aef-main AppTopbar depends on LoginService, SharedModule, AppConfigurator, LANGUAGES, FindLanguageFromKeyPipe which don't exist yet
- **Fix:** Created simplified AppTopbar with dark mode toggle, login/logout button and user display
- **Files modified:** frontend/src/app/layout/component/topbar/app.topbar.ts, app.topbar.html

**4. [Rule 1 - Bug] Made AppMenu.buildMenu() public for spec access**
- **Found during:** Task 3 — private buildMenu() cannot be called by spec for test isolation
- **Fix:** Changed from private to public visibility to allow TestBed-based spec to inspect model

## Known Stubs

- `frontend/src/app/pages/entities/entity.routes.ts` — empty Routes array, wired in future entity plans
- `frontend/src/app/pages/admin/security/security.routes.ts` — empty Routes array, wired in future security admin plans
- `frontend/src/app/layout/component/topbar/app.topbar.ts` — simplified topbar without language switcher and full account menu; full menu deferred to future plans
- `frontend/src/app/core/interceptor/error-handler.interceptor.ts` — logs to console.warn instead of using EventManager/AlertService; wiring to notification system deferred

## Self-Check: PASSED

All 22 key files verified present. All 3 task commits (05916ad, 6ceb452, ffc254f) verified in git log.
