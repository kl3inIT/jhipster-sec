---
status: resolved
trigger: "Registration activation link opens a 401 page instead of activating the new user."
created: 2026-03-28T03:00:00Z
updated: 2026-03-28T03:15:00Z
symptoms_prefilled: true
goal: find_and_fix
---

## Current Focus

hypothesis: Confirmed. The standalone frontend never ported the old activation page, but the backend mail template still linked to the legacy frontend path.
test: Focused code trace plus frontend and backend regression slices.
expecting: Resolved.
next_action: user retry against the updated activation email link

## Symptoms

expected: Clicking the activation link from the registration email should open the public frontend activation page and activate the account.
actual: The email opened `127.0.0.1:8080/account/activate?...` and the browser showed HTTP 401.
errors: Browser screenshot showed `HTTP ERROR 401` on the activation link.
reproduction: Register a new account, open the Mailpit activation email, click the generated activation link.
started: Reported during Phase 08.3 UAT on 2026-03-28.

## Evidence

- `src/main/resources/templates/mail/activationEmail.html` generated `${baseUrl}/account/activate?key=...`, which matched the old Angular app structure.
- The new standalone frontend exposed `/register` but had no `/activate` route or activation component.
- `src/main/java/com/vn/core/config/SecurityConfiguration.java` permits `/api/activate`, not `/account/activate`, so the emailed path hit a protected backend route and returned 401.
- The dev mail base URL in `src/main/resources/config/application-dev.yml` pointed at the backend origin (`127.0.0.1:8080`) instead of the standalone frontend origin.

## Resolution

root_cause: |
  Phase 08.3 added self-registration in the standalone Angular frontend, but it did not port the
  public activation page from the legacy Angular app. The backend activation email template kept
  generating the legacy `/account/activate` URL using a backend-origin base URL, so clicking the
  email link bypassed the frontend entirely and landed on a protected backend path, which returned
  HTTP 401.

fix: |
  Added a standalone frontend activation route and page under `frontend/src/app/pages/activate/`
  that calls `GET /api/activate` through the existing proxied API path. Updated the activation
  email template to generate `${baseUrl}/activate?key=...` instead of `/account/activate`, and
  changed the dev mail base URL to default to `http://localhost:4200` so local emails target the
  frontend origin. Also made the prod mail base URL override-friendly and wired the Dockerized app
  stack to pass `JHIPSTER_MAIL_BASE_URL` for local container runs.

verification: |
  - `cd frontend && npx ng test --watch=false --include="src/app/pages/activate/activate.component.spec.ts" --include="src/app/pages/activate/activate.service.spec.ts" --include="src/app/pages/register/register.component.spec.ts"`
  - `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 .\gradlew.bat integrationTest -x test --tests "com.vn.core.service.MailServiceIT"`

files_changed:
  - frontend/src/app/app.routes.ts
  - frontend/src/app.routes.ts
  - frontend/src/app/pages/activate/activate.component.ts
  - frontend/src/app/pages/activate/activate.component.html
  - frontend/src/app/pages/activate/activate.component.spec.ts
  - frontend/src/app/pages/activate/activate.service.ts
  - frontend/src/app/pages/activate/activate.service.spec.ts
  - src/main/resources/templates/mail/activationEmail.html
  - src/test/resources/templates/mail/activationEmail.html
  - src/main/resources/config/application-dev.yml
  - src/main/resources/config/application-prod.yml
  - src/main/docker/app.yml
  - src/test/java/com/vn/core/service/MailServiceIT.java
