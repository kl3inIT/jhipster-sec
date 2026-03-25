---
status: resolved
trigger: "Investigate issue: app-page-title-strategy-provider-missing"
created: 2026-03-25T10:25:11.4117123+07:00
updated: 2026-03-25T10:33:36.1917921+07:00
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: Confirmed. The startup failure was caused by the title-strategy DI mismatch plus intercepted translation loading.
test: Human UAT retested the cold-start flow.
expecting: Resolved.
next_action: none

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: Starting the frontend should load `/login` and the rest of the shell without DI errors.
actual: App startup fails before login with `NG0201: No provider found for _AppPageTitleStrategy`.
errors: `ERROR NotFound: NG0201: No provider found for _AppPageTitleStrategy. Source: Standalone[_App].`
reproduction: Start the frontend and load the app; the DI error appears immediately in the browser console.
started: First surfaced during phase 6 UAT on 2026-03-25 after the translated page-title strategy wiring landed.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: A full `appConfig.providers` TestBed is the best regression harness for this bug.
  evidence: It fails earlier with `NG0200: Circular dependency detected for Router`, caused by unrelated router error-handler wiring in the test environment.
  timestamp: 2026-03-25T10:33:20.0000000+07:00

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-03-25T00:00:00+07:00
  checked: `frontend/src/app.config.ts`
  found: The app config originally registered `{ provide: TitleStrategy, useClass: AppPageTitleStrategy }`.
  implication: Angular DI had a provider for `TitleStrategy`, not automatically for the concrete `AppPageTitleStrategy` token.

- timestamp: 2026-03-25T00:00:00+07:00
  checked: `frontend/src/app/app.ts`
  found: `App` calls `inject(AppPageTitleStrategy)` during bootstrap and uses it in `applyLanguageState`.
  implication: Startup fails if no provider exists for the concrete strategy class.

- timestamp: 2026-03-25T10:35:10.0000000+07:00
  checked: focused provider-token regression in `frontend/src/app/app.spec.ts`
  found: Creating `App` with `{ provide: TitleStrategy, useClass: AppPageTitleStrategy }` fails with `NG0201: No provider found for _AppPageTitleStrategy`.
  implication: The startup failure is caused by the token mismatch, not by a missing registration for `TitleStrategy`.

- timestamp: 2026-03-25T10:37:45.0000000+07:00
  checked: `frontend/src/app.config.ts`
  found: `AppPageTitleStrategy` is now registered directly and `TitleStrategy` points at the same instance with `useExisting`.
  implication: The concrete strategy token requested by `App` and the router strategy token now resolve from one shared provider.

- timestamp: 2026-03-25T10:37:45.0000000+07:00
  checked: `frontend/src/app/config/translation.config.ts` plus live browser translation-loader inspection
  found: Translation requests were routed through `HTTP_INTERCEPTORS`, where `NotificationInterceptor -> AlertService -> TranslateService` created `NG0200: Circular dependency detected for InjectionToken HTTP_INTERCEPTORS`. Setting `useHttpBackend: true` on the static i18n loader bypasses interceptors for translation JSON.
  implication: Translation bundles now load during bootstrap without entering the alert/translate cycle.

- timestamp: 2026-03-25T10:37:45.0000000+07:00
  checked: browser verification at `http://127.0.0.1:4200/login`
  found: The app booted into the translated login screen without NG0201; the only console error was the expected logged-out `401` on `/api/account`.
  implication: The cold-start blocker is fixed and the app can proceed to human re-verification.

- timestamp: 2026-03-25T10:37:45.0000000+07:00
  checked: `npm --prefix frontend exec ng test -- --watch=false --include src/app/config/translation.config.spec.ts --include src/app/app.spec.ts --include src/app/app-page-title-strategy.spec.ts`
  found: All 3 test files passed (7 tests total).
  implication: The patched bootstrap and translation config hold under the targeted regression suite.

- timestamp: 2026-03-25T10:33:36.1917921+07:00
  checked: phase 6 human UAT retest for the cold-start smoke path
  found: User reported `pass` after restarting and reopening the application.
  implication: The original startup blocker is resolved in the real user flow, not just in local self-verification.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: Two bootstrap defects stacked together. First, `frontend/src/app.config.ts` only bound `AppPageTitleStrategy` behind the `TitleStrategy` token while `App` requested the concrete class token, causing NG0201 on startup. Second, the ngx-translate HTTP loader used the normal intercepted `HttpClient`, and `NotificationInterceptor -> AlertService -> TranslateService` created a circular dependency during translation bootstrap, leaving the app on `translation-not-found[...]` keys.
fix: Registered `AppPageTitleStrategy` directly and mapped `TitleStrategy` to the same instance with `useExisting`. Reworked the translation loader wiring so `provideTranslateService` gets an explicit `TranslateHttpLoader` provider plus its config token, and set `useHttpBackend: true` so static i18n requests bypass application interceptors.
verification: Browser smoke verification at `http://127.0.0.1:4200/login` showed translated login UI with no NG0201 crash, targeted Angular tests passed for `translation.config.spec.ts`, `app.spec.ts`, and `app-page-title-strategy.spec.ts`, and the user confirmed the cold-start smoke retest passed.
files_changed:
  - frontend/src/app.config.ts
  - frontend/src/app/config/translation.config.ts
  - frontend/src/app/config/translation.config.spec.ts
