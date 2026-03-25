---
status: diagnosed
trigger: "Investigate why the logout button lags and does not complete logout without a page reload in the Angular frontend."
created: 2026-03-23T00:00:00Z
updated: 2026-03-23T00:00:00Z
symptoms_prefilled: true
goal: find_root_cause_only
---

## Current Focus

hypothesis: CONFIRMED — two interlocking bugs: (1) accountCache$ with shareReplay() replays the stale authenticated account after logout clears userIdentity in-memory, causing UserRouteAccessService to pass the cached account to the route guard; (2) router.navigate(['']) after logout lands on the home route which has canActivate: [UserRouteAccessService], that guard calls accountService.identity(), which returns the still-live accountCache$ (because authenticate(null) only nulls the field but shareReplay keeps the old emission in the multicasted buffer)
test: code-read trace of the logout call chain
expecting: logout hangs/re-authenticates because the guard sees a cached account and allows access
next_action: root cause confirmed — deliver diagnosis

## Symptoms

expected: Clicking logout clears session, navigates to login, completes without requiring page reload
actual: Logout button lags (hangs), session is not cleared — user must manually reload the page
errors: No explicit error message reported; silent hang
reproduction: Click logout button in the Angular frontend
started: Unknown — reported as current behavior

## Eliminated

- hypothesis: Observable in AuthServerProvider.logout() never completes
  evidence: AuthServerProvider.logout() creates a new Observable that immediately calls observer.complete() — it always completes synchronously
  timestamp: 2026-03-23

- hypothesis: JWT token not cleared before navigation
  evidence: stateStorageService.clearAuthenticationToken() is called inside the logout() Observable before observer.complete(), so the token IS cleared from storage before the subscriber's callback runs
  timestamp: 2026-03-23

- hypothesis: AuthExpiredInterceptor causes a re-navigation loop
  evidence: interceptor only fires on HTTP 401 errors; no HTTP request is made during the logout navigation path, so it is not involved
  timestamp: 2026-03-23

## Evidence

- timestamp: 2026-03-23
  checked: frontend/src/app/layout/component/topbar/app.topbar.ts lines 38-43
  found: logout() calls authServerProvider.logout().subscribe(() => { accountService.authenticate(null); router.navigate(['']); })
  implication: After clearing the token, it calls authenticate(null) which sets userIdentity signal to null and sets accountCache$ = null (line 26-27 of account.service.ts). Then it navigates to route ''.

- timestamp: 2026-03-23
  checked: frontend/src/app.routes.ts lines 10-17
  found: Route '' (home) has canActivate: [UserRouteAccessService]
  implication: Navigating to '' after logout immediately triggers the route guard.

- timestamp: 2026-03-23
  checked: frontend/src/app/core/auth/user-route-access.service.ts lines 12-13
  found: Guard calls accountService.identity() unconditionally
  implication: This triggers identity() on every route activation.

- timestamp: 2026-03-23
  checked: frontend/src/app/core/auth/account.service.ts lines 45-56
  found: identity() checks (!this.accountCache$ || force). authenticate(null) sets accountCache$ = null (line 26). So after logout, accountCache$ is null, and a new fetch() call is made to api/account.
  implication: The guard re-fetches /api/account even though the JWT token is gone. The request goes out WITHOUT the token (clearAuthenticationToken already ran), so the backend returns HTTP 401.

- timestamp: 2026-03-23
  checked: frontend/src/app/core/interceptor/auth-expired.interceptor.ts lines 17-22
  found: On 401 from any URL that is NOT api/account, the interceptor calls stateStorageService.storeUrl(currentUrl) and router.navigate(['/login']).
  implication: The 401 response from api/account IS excluded from the interceptor's handler (the condition checks !err.url.includes('api/account')). So the interceptor does NOT navigate to login for this call.

- timestamp: 2026-03-23
  checked: frontend/src/app/core/auth/account.service.ts line 55
  found: identity() returns accountCache$.pipe(catchError(() => of(null))). On a 401 the fetch() errors, catchError converts it to of(null).
  implication: The guard receives null from identity(), so it falls through to the unauthenticated branch (lines 28-30 of user-route-access.service.ts): storeUrl + navigate(['/login']). This IS correct behavior — but it is SLOW because it waits for a full HTTP round-trip to /api/account before redirecting.

- timestamp: 2026-03-23
  checked: frontend/src/app/pages/login/login.component.ts lines 46-51
  found: LoginComponent.ngOnInit() calls accountService.identity().subscribe(). At this point accountCache$ is null again (cleared on the error path), so it fires ANOTHER request to /api/account, gets 401, catchError -> null, isAuthenticated() is false, does not redirect. This is fine but adds another round-trip.
  implication: The lag is caused by two sequential HTTP requests to /api/account, both of which 401 and take server round-trip time before the UI settles. The total visible lag = two HTTP requests to backend.

- timestamp: 2026-03-23
  checked: frontend/src/app/core/auth/account.service.ts lines 46-56 (shareReplay behavior)
  found: shareReplay() with no bufferSize argument defaults to bufferSize=infinity and refCount=false in RxJS. However accountCache$ is set to null by authenticate(null) before the navigation, so the old shared observable is dereferenced. The NEW call to identity() creates a fresh fetch. So shareReplay is NOT the re-authentication bug here.
  implication: The stale-cache re-auth hypothesis is eliminated. The bug is purely the unnecessary HTTP request to /api/account during the guard on the post-logout navigation.

## Resolution

root_cause: |
  After logout, the topbar navigates to route '' (home). That route has canActivate: [UserRouteAccessService].
  The guard calls accountService.identity(). Because accountCache$ was just nulled by authenticate(null),
  identity() makes a fresh HTTP GET /api/account request. The request carries no JWT token (it was
  already cleared). The backend responds with HTTP 401. Because auth-expired.interceptor.ts excludes
  'api/account' from its 401 handler, the interceptor does NOT navigate to /login. The identity()
  observable's catchError converts the 401 to of(null). The guard then sees null and calls
  router.navigate(['/login']). This is eventually correct but forces a full server round-trip before
  the user sees the login page — that round-trip is the "lag".

  The secondary contributor is that after arriving at LoginComponent, ngOnInit calls
  accountService.identity() again (to auto-redirect if already logged in), firing a second /api/account
  request which also 401s. This second request adds additional lag.

  The root fix: navigate to '/login' directly from logout() instead of navigating to '' (home).
  Route '' is a guarded authenticated route — it is the wrong target after a logout. Navigating to
  '/login' skips the guard entirely, clears the lag, and leaves the user on the expected page
  immediately.

fix: |
  In frontend/src/app/layout/component/topbar/app.topbar.ts, change the logout() method:

  CURRENT (line 41):  this.router.navigate(['']);
  CORRECT:            this.router.navigate(['/login']);

  This single character change eliminates the unnecessary /api/account round-trip by bypassing the
  UserRouteAccessService guard entirely. The login page is the correct post-logout destination.

  Optional secondary fix: the LoginComponent.ngOnInit identity() call is a defensive "already
  logged in?" check. It will still fire a /api/account request but that is appropriate for that
  component's purpose. No change needed there.

verification:
files_changed:
  - frontend/src/app/layout/component/topbar/app.topbar.ts
