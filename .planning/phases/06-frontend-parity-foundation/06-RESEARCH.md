# Phase 6: Frontend Parity Foundation - Research

**Researched:** 2026-03-25
**Domain:** Angular 21 standalone frontend parity, JHipster translation runtime, and shared admin foundation migration
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
### Donor and support-file scope
- **D-01:** Keep `aef-main` as the structural template for standalone Angular bootstrap, router/layout patterns, and PrimeNG shell behavior, but use `angapp` as the canonical donor for JHipster-specific support files, translations, and admin/user-management runtime pieces.
- **D-02:** Phase 6 migrates only the minimum in-scope donor pack needed for later shell and admin work: translation config/runtime, language assets, alert/notification plumbing, shared request and pagination helpers, authority display/gating helpers, and the preserved admin user-management model/service/route foundation.
- **D-03:** Copied donor pieces must be adapted to the standalone Angular and PrimeNG shell already established in `frontend/`; do not reintroduce broad NgModule-era shared-module structure or copy out-of-scope ops/admin pages.

### Language and translation baseline
- **D-04:** Vietnamese (`vi`) is the default language on first load; English (`en`) is the secondary language.
- **D-05:** The selected locale is persisted through the existing frontend storage flow, and the persisted choice overrides the default language on later visits.
- **D-06:** Phase 6 must include `vi` and `en` translation assets for all in-scope shared shell, admin, account, and user-management foundations so later phases do not build on missing-key gaps.

### Alerts and shared feedback
- **D-07:** Replace console-only success-notification handling with a user-visible, translated alert foundation so backend `app-alert` headers surface inside the frontend runtime.
- **D-08:** Keep the feedback foundation compatible with the PrimeNG shell: translated inline or route-safe alerts are the baseline, with planners free to use toast presentation where appropriate for transient success states.
- **D-09:** Port the JHipster alert/support helpers from `angapp` only as far as needed to restore shared feedback behavior; do not copy the full legacy Bootstrap-centric alert stack wholesale.

### User-management groundwork
- **D-10:** Phase 6 migrates the preserved admin user-management foundation now: user model, service contract, route skeleton or resolver, request models, pagination helpers, and directly supporting shared components or directives.
- **D-11:** Full user-management screens and CRUD interactions remain Phase 8 work; Phase 6 only prepares the shared runtime and translation groundwork those screens will depend on.
- **D-12:** Shared list, filter, and pagination helpers should be brought over only when they directly support in-scope admin or shell groundwork; unrelated legacy admin utilities remain out of scope.

### Claude's Discretion
- Exact file-by-file migration list within the approved donor pack
- Whether helper utilities are direct ports or lightweight standalone-adapted rewrites
- Exact alert presentation component structure and whether success feedback is inline, toast, or mixed by context
- Whether a language switcher UI appears in Phase 6 or Phase 7, as long as the `vi`/`en` runtime and persisted locale behavior are in place

### Deferred Ideas (OUT OF SCOPE)
- Backend-driven navigation and permission-aware menu loading - Phase 7
- Full admin user-management browse, detail, create, edit, and delete UI - Phase 8
- Broader enterprise responsiveness and performance hardening - Phase 9
- Frontend regression coverage expansion - Phase 10
- Legacy ops/admin pages such as health, metrics, logs, configuration, and docs - still out of scope for this milestone unless a later phase explicitly pulls shared pieces from them
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| I18N-01 | Required JHipster support files from `angapp/` for in-scope frontend features are migrated into `frontend/` instead of being reimplemented incompletely. | Prescribes the donor pack, target folders, standalone adaptation rules, and the shared services/directives/helpers to port directly. |
| I18N-02 | Migrated admin, user-management, and shared shell flows can render translated UI strings and preserve language-aware behavior using copied JHipster translation assets. | Prescribes the translation bootstrap, `vi`/`en` asset layout, locale precedence, route-title/menu translation strategy, alert translation path, and required tests. |
</phase_requirements>

## Summary

Phase 6 is an integration phase, not a library-selection phase. The required stack is already present in `frontend/package.json`: Angular 21 standalone bootstrap, `@ngx-translate/core`, `@ngx-translate/http-loader`, PrimeNG, and the existing auth/request seams. The planning risk is not "which package," it is "how to adapt the `angapp` donor behavior into the current standalone shell without recreating JHipster's old NgModule and webpack assumptions."

The biggest technical constraint is the translation asset shape. `angapp` stores translations as many namespace files under `i18n/en/*.json` and `i18n/vi/*.json`, while current Angular 21 guidance and ngx-translate's HTTP loader expect static files served from `public/`, typically one file per language path. Because this repo does not have JHipster's old webpack merge step, Phase 6 should normalize the donor namespaces into `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` while preserving the donor key hierarchy such as `global.*`, `login.*`, and `userManagement.*`. Do not spend the phase rebuilding a custom translation loader or webpack merger.

The second major constraint is that much of the current shell text is hardcoded in TypeScript, not templates. `frontend/src/app/layout/component/menu/app.menu.ts`, route `title` fields, `login.component.html`, and topbar aria labels will not become translated just because JSON files exist. Phase 6 must establish the runtime pattern: root translation providers in `app.config.ts`, route titles treated as translation keys via `AppPageTitleStrategy`, PrimeNG menu models recomputed on language change, and JHipster alert headers converted into translated inline or toast feedback through a shared alert service.

**Primary recommendation:** Plan Phase 6 around a direct donor migration of JHipster support files plus a standalone-native i18n bootstrap in `app.config.ts`, using merged `public/i18n/{lang}.json` assets and PrimeNG-based alert presentation instead of porting `angapp`'s NgModule and Bootstrap alert rendering wholesale.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `@angular/core` | `21.2.x` | Standalone Angular runtime, router, DI, test builder | Already pinned in `frontend/`; official Angular 21 docs define `app.config.ts`, `public/`, and builder behavior this phase must follow. |
| `@ngx-translate/core` | `17.0.0` | Runtime translation service, language switching, pipes/directives | Official current ngx-translate guidance for standalone apps uses provider functions in `app.config.ts`. |
| `@ngx-translate/http-loader` | `17.0.0` | Loads static translation JSON from `public/i18n` | Official loader for ngx-translate JSON assets; avoids custom translation fetch code. |
| `primeng` | `21.1.x` | Shell-aligned inline and toast feedback, menus, form messaging | Existing UI shell already uses PrimeNG; alerts and language-driven menus should stay inside that system. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@primeuix/themes` | `2.0.3` | PrimeNG theme tokens used by the current shell | Keep alert/message presentation visually aligned with the existing Sakai-style shell. |
| `dayjs` | `1.11.x` | Date locale switching for translated UI surfaces | Update locale on `TranslateService.onLangChange` for user-management dates and shared formatted timestamps. |
| `@ng-bootstrap/ng-bootstrap` | `20.0.0` | Existing date adapter support already referenced by `aef-main` patterns | Keep for existing datepicker/date-adapter usage only; do not use it as the new alert presentation layer. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Root merged `public/i18n/en.json` and `vi.json` | Feature-by-feature `provideChildTranslateService()` loaders | Useful later for very large lazy features, but Phase 6 is simpler and lower-risk with one merged file per language. |
| PrimeNG `Message` / `Toast` presentation | `@ng-bootstrap/ng-bootstrap` alert component from donor | Works mechanically, but fights the PrimeNG shell and violates D-09's "no full Bootstrap alert stack" rule. |
| Existing DI interceptors with `withInterceptorsFromDi()` | Functional interceptors | Angular supports them, but refactoring the interceptor model adds churn unrelated to parity goals. |

**Installation:**
```bash
# No new package is required if frontend/package.json stays on the current baseline
npm install
```

**Version verification:** Latest registry versions checked on 2026-03-25:

- `@angular/core` `21.2.5` - published 2026-03-18
- `@ngx-translate/core` `17.0.0` - published 2025-07-28
- `@ngx-translate/http-loader` `17.0.0` - published 2025-07-28
- `primeng` `21.1.3` - published 2026-03-04
- `@primeuix/themes` `2.0.3` - published 2026-01-19
- `@ng-bootstrap/ng-bootstrap` `20.0.0` - published 2025-12-12
- `dayjs` `1.11.20` - published 2026-03-12

## Architecture Patterns

### Recommended Project Structure
```text
frontend/
  public/
    i18n/
      en.json          # merged donor namespaces for shared shell/account/admin foundation
      vi.json
  src/app/
    app.config.ts
    app-page-title-strategy.ts
    config/
      dayjs.ts
      language.constants.ts
      translation.config.ts
    core/
      auth/
      interceptor/
      request/
      util/
        alert.service.ts
    shared/
      alert/
      auth/
      language/
      pagination/
    pages/
      admin/
        user-management/
          service/
          user-management.model.ts
          user-management.routes.ts
```

### Pattern 1: Standalone Root Translation Bootstrap
**What:** Configure translations in `frontend/src/app/app.config.ts` using standalone provider functions, not a ported NgModule wrapper.
**When to use:** Always for the root app in Phase 6.
**Example:**
```typescript
// Source: https://ngx-translate.org/reference/configuration
// Source: https://ngx-translate.org/getting-started/translation-files/
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  provideMissingTranslationHandler,
  provideTranslateService,
} from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { MissingTranslationHandlerImpl } from './config/translation.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: './i18n/', suffix: '.json' }),
      fallbackLang: 'en',
      missingTranslationHandler: provideMissingTranslationHandler(MissingTranslationHandlerImpl),
    }),
  ],
};
```

### Pattern 2: Locale Precedence and Startup Behavior
**What:** Resolve language in this order: stored locale from `StateStorageService` -> Phase 6 default `vi` -> account preference after `/api/account`, but only when no stored locale exists.
**When to use:** App startup, login completion, and account refresh.
**Example:**
```typescript
// Source: project-local donor pattern in angapp/src/main/webapp/app/core/auth/account.service.ts
const storedLocale = this.stateStorageService.getLocale();
const initialLocale = storedLocale ?? 'vi';

this.translateService.use(initialLocale).subscribe(() => {
  dayjs.locale(initialLocale);
  document.documentElement.lang = initialLocale;
});

this.accountService.identity().subscribe(account => {
  if (account && !storedLocale) {
    this.translateService.use(account.langKey);
  }
});
```

### Pattern 3: Treat Route Titles as Translation Keys
**What:** Route `title` values should be translation keys such as `global.menu.home` or `userManagement.home.title`, not literal English strings.
**When to use:** Every route added or touched in Phase 6 and later phases.
**Example:**
```typescript
// Source: project-local donor pattern in aef-main/aef-main/src/app-page-title-strategy.ts
import { Injectable, inject } from '@angular/core';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class AppPageTitleStrategy extends TitleStrategy {
  private readonly translateService = inject(TranslateService);

  override updateTitle(routerState: RouterStateSnapshot): void {
    const key = this.buildTitle(routerState) ?? 'global.title';
    this.translateService.get(key).subscribe(title => {
      document.title = title;
    });
  }
}
```

### Pattern 4: Recompute PrimeNG Menu Models on Language Change
**What:** PrimeNG `MenuItem` labels must be created from translation keys in reactive code, because they are not template text.
**When to use:** `AppMenu`, topbar account menus, and any TS-built PrimeNG action model.
**Example:**
```typescript
// Source: project-local reference in aef-main/aef-main/src/app/layout/component/topbar/app.topbar.ts
languageMenuItems = computed(() => {
  this.currentLang();
  return this.languages.map(lang => ({
    label: this.findLanguageFromKeyPipe.transform(lang),
    data: lang,
    command: e => this.changeLanguage(String(e.item?.data ?? '')),
  }));
});

menubarModel = computed(() => {
  this.currentLang();
  const t = (key: string) => this.translateService.instant(key);
  return [
    { label: t('global.menu.language'), items: this.languageMenuItems() },
    { label: t('global.menu.account.main'), items: this.accountMenuItems() },
  ];
});
```

### Pattern 5: Adapt JHipster Alert Semantics to PrimeNG Presentation
**What:** Keep the donor alert contract and interceptor behavior, but render alerts with PrimeNG inline `p-message` or `p-toast`.
**When to use:** Backend success headers, shared error display, and route-safe alerts.
**Example:**
```typescript
// Source: project-local donor pattern in angapp notification interceptor
// Source: https://primeng.org/toast
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { tap } from 'rxjs/operators';

import { AlertService } from 'app/core/util/alert.service';

@Injectable()
export class NotificationInterceptor implements HttpInterceptor {
  private readonly alertService = inject(AlertService);

  intercept(request: HttpRequest<unknown>, next: HttpHandler) {
    return next.handle(request).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          let alert: string | null = null;
          let alertParams: string | null = null;

          for (const headerKey of event.headers.keys()) {
            if (headerKey.toLowerCase().endsWith('app-alert')) {
              alert = event.headers.get(headerKey);
            } else if (headerKey.toLowerCase().endsWith('app-params')) {
              alertParams = decodeURIComponent(event.headers.get(headerKey) ?? '');
            }
          }

          if (alert) {
            this.alertService.addAlert({
              type: 'success',
              translationKey: alert,
              translationParams: { param: alertParams },
            });
          }
        }
      }),
    );
  }
}
```

### Anti-Patterns to Avoid
- **Porting `TranslationModule` unchanged from `angapp`:** That reproduces NgModule-era bootstrap side effects in a standalone app and ignores current ngx-translate provider guidance.
- **Copying `angapp/src/main/webapp/i18n/{lang}/*.json` into `frontend/public/i18n/{lang}/` without changing the loader:** the official HTTP loader shown by ngx-translate resolves one path per language, not a folder tree of namespace files.
- **Keeping literal English route titles and menu labels:** copied translation assets become dead weight if routes, PrimeNG menus, and aria labels stay hardcoded.
- **Letting `/api/account` overwrite a user-selected locale every time:** this breaks D-05; stored locale must win over account preference.
- **Using Bootstrap alert UI as the new baseline:** the donor alert rendering is a behavior reference only; the shell is PrimeNG.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Translation runtime wiring | A custom i18n framework or ad hoc JSON fetch service | `@ngx-translate/core` + `@ngx-translate/http-loader` configured in `app.config.ts` | Official standalone pattern already covers provider setup, fallback language, and HTTP loading. |
| Multi-surface notification rendering | A bespoke event bus plus handcrafted overlays | Adapt donor `AlertService` semantics to PrimeNG `Message` / `Toast` | PrimeNG already provides inline and overlay messaging with accessibility behavior. |
| Authority-based template gating | Repeated `@if (accountService.hasAnyAuthority(...))` fragments everywhere | Donor `HasAnyAuthorityDirective` backed by existing `AccountService` | Keeps auth checks centralized and consistent across future admin screens. |
| Query-string building for admin lists | Per-service manual `HttpParams` string assembly | Shared `request.model.ts` + `createRequestOption()` | Avoids drift in pagination/sort/search contracts across admin list services. |
| Route-title translation logic | Manual `document.title = ...` calls in components | Shared `AppPageTitleStrategy` keyed by translation IDs | Prevents title drift and keeps updates aligned with router navigation and language changes. |

**Key insight:** the risky work in this domain is not the donor file copy itself; it is rebuilding solved runtime plumbing piecemeal. Keep the JHipster behaviors, adapt the presentation and provider shape, and avoid inventing new loaders, alert buses, or authority helpers.

## Common Pitfalls

### Pitfall 1: Donor Translation Files Are Copied But Never Load
**What goes wrong:** `frontend/public/i18n/en/*.json` and `vi/*.json` exist, but the app still renders translation keys.
**Why it happens:** `angapp` relied on JHipster's build step to merge many namespace files into a single `i18n/en.json` and `i18n/vi.json`; current `frontend/` does not have that merge.
**How to avoid:** Normalize donor namespaces into one `en.json` and one `vi.json` for Phase 6, keeping nested namespaces intact.
**Warning signs:** `translation-not-found[...]` output, 404s for `/i18n/vi.json`, or missing-key fallbacks in login/menu text.

### Pitfall 2: Language Switching Only Affects Template Pipes
**What goes wrong:** Template text updates, but route titles, menu labels, and topbar account actions stay English.
**Why it happens:** PrimeNG `MenuItem` models and route `title` values are plain strings created in TypeScript.
**How to avoid:** Treat route titles as translation keys and rebuild PrimeNG menu models from `TranslateService` on `onLangChange`.
**Warning signs:** translated login form next to an English sidebar or stale browser tab title after switching languages.

### Pitfall 3: Stored Locale Loses to Account Preference
**What goes wrong:** user selects `en`, refreshes, and the app flips back to `vi` or the account's `langKey`.
**Why it happens:** startup code calls `translate.use(account.langKey)` unconditionally after `/api/account`.
**How to avoid:** follow the donor rule exactly: only apply `account.langKey` when `StateStorageService.getLocale()` is empty.
**Warning signs:** locale toggles work until the next authenticated refresh.

### Pitfall 4: Alert Headers Are Parsed but Never Reach the User
**What goes wrong:** backend sends `app-alert` and `app-params`, but the frontend still only writes to `console.warn`.
**Why it happens:** current `frontend/src/app/core/interceptor/notification.interceptor.ts` logs instead of publishing to a shared alert UI.
**How to avoid:** keep the header parsing, replace console logging with a shared alert service, and mount a visible alert outlet in the shell.
**Warning signs:** successful create/update/delete calls have alert headers in dev tools but no visible confirmation.

### Pitfall 5: Accessibility and Locale Metadata Stay Stale
**What goes wrong:** translated text changes, but `html[lang]` and date locale do not.
**Why it happens:** i18n wiring stops at `TranslateService.use(...)`.
**How to avoid:** subscribe to `onLangChange` once in the app shell and update `document.documentElement.lang` and `dayjs.locale(...)`.
**Warning signs:** screen readers announce the wrong language or date formatting stays English under Vietnamese UI.

### Pitfall 6: Scope Expands Into Full User Management UI
**What goes wrong:** Phase 6 balloons into list/detail/update screens.
**Why it happens:** the donor user-management route file references list/detail/update components, which tempts implementers to keep copying.
**How to avoid:** stop at model, service, request helpers, translation assets, and route/resolver foundation; full screens are Phase 8.
**Warning signs:** plans start including CRUD forms, modal flows, or end-to-end user-management interaction tasks.

## Code Examples

Verified patterns from official and project-local sources:

### Root Translation Provider with Static JSON Assets
```typescript
// Source: https://ngx-translate.org/getting-started/translation-files/
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: './i18n/', suffix: '.json' }),
      fallbackLang: 'en',
      lang: 'vi',
    }),
  ],
};
```

### PrimeNG Toast for Translated Success Feedback
```typescript
// Source: https://primeng.org/toast
// Source: translated alert semantics from angapp donor interceptor + alert service
import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({ providedIn: 'root' })
export class AlertPresenter {
  private readonly messageService = inject(MessageService);

  success(detail: string): void {
    this.messageService.add({
      severity: 'success',
      detail,
      life: 3000,
    });
  }
}
```

### Update the Shell on Language Change
```typescript
// Source: project-local donor pattern in angapp/src/main/webapp/app/layouts/main/main.component.ts
this.translateService.onLangChange.subscribe(event => {
  this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
  dayjs.locale(event.lang);
  document.documentElement.lang = event.lang;
});
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NgModule-centric `TranslateModule.forRoot()` wrapper classes | Standalone `provideTranslateService()` and provider functions in `app.config.ts` | ngx-translate v17 | Phase 6 should configure translation at the root provider level, not port donor NgModules unchanged. |
| `src/assets/i18n/*` as the default translation location | `public/i18n/*` for Angular 18+ style workspaces | Angular 18+ / current Angular 21 docs | Plan translation assets under `frontend/public/i18n`, not under `src/assets`. |
| Donor webpack merge of many namespace files into one language bundle | Static merged `public/i18n/{lang}.json` per language in the standalone app | JHipster old client build no longer exists in this repo | Phase 6 must normalize donor assets explicitly during migration. |
| Bootstrap alert widgets as the default JHipster UI | PrimeNG `Message` / `Toast` inside a PrimeNG shell | Existing `frontend/` shell baseline | Keep JHipster alert behavior, replace only the visual rendering layer. |

**Deprecated/outdated:**
- `defaultLanguage` and `useDefaultLang` in ngx-translate config: deprecated in current docs; use `fallbackLang`.
- Manual `new TranslateHttpLoader(http, prefix, suffix)` factory setup: legacy-friendly but superseded in v17 docs by `provideTranslateHttpLoader({ prefix, suffix })`.
- Porting `src/assets/i18n` assumptions from older Angular examples: current Angular workspace guidance treats `public/` as the static asset root.

## Open Questions

1. **Should "persisted locale" remain session-scoped or become cross-browser persistent?**
   - What we know: both the current `frontend` and donor `angapp` store locale in `StateStorageService` using `sessionStorage`.
   - What's unclear: D-05 says the choice should override the default on later visits, which could be interpreted more broadly than same-session revisits.
   - Recommendation: preserve `sessionStorage` for Phase 6 unless the user explicitly wants stronger persistence; otherwise this phase silently changes auth/storage semantics.

2. **Does Phase 6 need a visible language switcher, or only runtime support?**
   - What we know: the discretion section explicitly allows either Phase 6 or Phase 7, as long as runtime `vi`/`en` support and persisted locale behavior are in place.
   - What's unclear: whether planner should budget topbar/menu UI work now.
   - Recommendation: treat the switcher as optional in this phase; prioritize runtime bootstrap, storage, and translated shell/admin foundations first.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Angular test builder with `vitest` runner (default) |
| Config file | `none` for runner config; builder lives in `frontend/angular.json`, TypeScript test config in `frontend/tsconfig.spec.json` |
| Quick run command | `npm exec ng test frontend -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts` |
| Full suite command | `npm test -- --watch=false` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| I18N-01 | Root i18n bootstrap, donor support helpers, and alert/runtime wiring are present and connected in `frontend/` | unit | `npm exec ng test frontend -- --watch=false --include src/app/config/translation.config.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/core/util/alert.service.spec.ts` | no - Wave 0 |
| I18N-02 | Shell, login, and user-management foundations render translation keys correctly and preserve locale precedence/update behavior | component | `npm exec ng test frontend -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts --include src/app/app-page-title-strategy.spec.ts` | no - Wave 0 |

### Sampling Rate
- **Per task commit:** `npm exec ng test frontend -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts`
- **Per wave merge:** `npm test -- --watch=false`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `frontend/src/app/config/translation.config.spec.ts` - covers root loader path, fallback language, and missing-translation handler behavior for I18N-01
- [ ] `frontend/src/app/core/auth/account.service.spec.ts` - covers locale precedence (`stored locale` vs `account.langKey`) for I18N-02
- [ ] `frontend/src/app/core/interceptor/notification.interceptor.spec.ts` - covers `app-alert` / `app-params` header handling for I18N-01
- [ ] `frontend/src/app/core/util/alert.service.spec.ts` - covers translated message resolution and dismissal behavior for I18N-01
- [ ] `frontend/src/app/layout/component/menu/app.menu.spec.ts` - extend current menu tests to verify translated labels and recomputation on language change for I18N-02
- [ ] `frontend/src/app/app-page-title-strategy.spec.ts` - covers route-title translation and `html[lang]` updates for I18N-02

## Sources

### Primary (HIGH confidence)
- Project-local donor and target references from `.planning/phases/06-frontend-parity-foundation/06-CONTEXT.md`
- ngx-translate Configuration: https://ngx-translate.org/reference/configuration
- ngx-translate Installation: https://ngx-translate.org/getting-started/installation
- ngx-translate Loading Translation Files: https://ngx-translate.org/getting-started/translation-files/
- ngx-translate Fix Translation Loading Glitches: https://ngx-translate.org/recipes/fix-translation-loading-glitches/
- Angular workspace file structure: https://angular.dev/reference/configs/file-structure
- Angular workspace configuration: https://angular.dev/reference/configs/workspace-config
- PrimeNG Toast: https://primeng.org/toast
- PrimeNG Message: https://v20.primeng.org/message
- npm registry version checks via `npm view` on 2026-03-25

### Secondary (MEDIUM confidence)
- JHipster Internationalization docs: https://www.jhipster.tech/installing-new-languages/ (useful for legacy donor asset conventions, but not authoritative for current standalone Angular runtime setup)

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - verified against current `frontend/package.json`, npm registry, and official Angular/ngx-translate docs
- Architecture: HIGH - based on official standalone/provider guidance plus direct inspection of `angapp`, `aef-main`, and current `frontend/` seams
- Pitfalls: HIGH - derived from current repo gaps (`app.config.ts`, `app.routes.ts`, hardcoded labels, missing `public/i18n`, console-only notification interceptor) and official docs

**Research date:** 2026-03-25
**Valid until:** 2026-04-08
