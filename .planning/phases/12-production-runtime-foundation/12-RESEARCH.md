# Phase 12: Production Runtime Foundation - Research

**Researched:** 2026-04-02
**Domain:** JHipster production-like Docker Compose runtime baseline
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
### Configuration source
- **D-01:** Production-like runtime configuration must come from committed compose assets plus environment-driven runtime values, not hardcoded machine-specific values.
- **D-02:** `src/main/resources/config/application-prod.yml` should stop carrying fixed database host, username, and password values and instead rely on environment-backed placeholders aligned with compose/runtime inputs.
- **D-03:** When environment-specific server details are required, ask the user for real server IP, proxy, port, or config information instead of inferring deployment values.

### Runtime topology
- **D-04:** Phase 12 baseline stack is backend + PostgreSQL + Mailpit using committed Docker Compose assets as the default production-like runtime shape.
- **D-05:** Keep frontend API usage backend-relative (`SERVER_API_URL` empty) so the runtime can work behind a later reverse-proxy or same-origin deployment shape without introducing extra frontend environment branching in this phase.
- **D-06:** Reuse the existing Jib-built backend image and compose manifests rather than inventing a second packaging path.

### Validation target
- **D-07:** Phase 12 must preserve and validate brownfield-safe auth, account, admin-user, mail, and secured-entity flows inside the production-like stack.
- **D-08:** The runtime baseline must expose the dependencies later phases need for benchmark and security-validation work, including database-backed startup, health checks, and operationally relevant configuration.

### Operational scope
- **D-09:** Keep health checks and basic metrics hooks that already fit the current stack, but do not expand this phase into full observability rollout.
- **D-10:** Defer TLS, real SMTP provider integration, external secret-manager wiring, and internet-exposed hardening beyond the minimum production-like baseline.

### Standards to follow
- **D-11:** Prefer JHipster production and Docker best practices as the default reference for runtime and deployment decisions unless project-specific constraints require otherwise.

### Claude's Discretion
- Exact compose file structure and whether the baseline is expressed as one primary stack file or a thin composition of existing files.
- Exact environment-variable naming alignment between compose, Spring Boot, and Jib entrypoint handling.
- Whether optional monitoring helpers remain separate compose files or are referenced only as deferred operational add-ons.

### Deferred Ideas (OUT OF SCOPE)
- Repository-native CI/CD and split verification lanes — Phase 13.
- Production-like benchmark dataset design, concurrency coverage, and stack-validation reporting — Phase 14.
- Permission-path optimization work — Phase 15.
- Final production security proof package — Phase 16.
- Real SMTP provider integration, TLS termination strategy, external secret manager integration, and full monitoring/log aggregation rollout — outside Phase 12 baseline unless required to make the stack realistically startable.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PROD-01 | The project can start a production-like application stack from committed configuration and compose assets without depending on dev-only shortcuts. | Externalize `application-prod.yml`, keep Compose as committed baseline, reuse Jib image path, require service health checks and env-driven runtime inputs. |
| PROD-02 | The production-like stack preserves brownfield-safe auth, account, admin-user, mail, and secured-entity behavior so validation can run against a realistic runtime shape. | Keep backend-relative frontend API behavior, preserve Mailpit and PostgreSQL in stack, validate with existing backend ITs and frontend Playwright specs that cover auth/account/admin-user/mail/security flows. |
</phase_requirements>

## Summary

Phase 12 should formalize the existing JHipster Docker assets into a real production-like baseline instead of creating a new runtime path. The repo already has the right building blocks: a Jib-built backend image (`jhipster-sec:latest`), a layered Compose setup (`app.yml`, `services.yml`, `postgresql.yml`, `mailpit.yml`), Spring Boot profile-driven config, Actuator health groups, and a backend/frontend regression suite that already targets the behaviors Phase 12 must preserve. The planning focus should therefore be normalization, not invention.

The main blocker to a trustworthy baseline is configuration drift. `application-prod.yml` currently hardcodes a public IP, database username, and password, while `src/main/docker/postgresql.yml` still uses `POSTGRES_HOST_AUTH_METHOD=trust`, and `src/main/docker/app.yml` carries localhost-biased defaults like `JHIPSTER_MAIL_BASE_URL=http://localhost:4200` and `SPRING_PROFILES_ACTIVE=prod,api-docs`. Those choices are acceptable for generated local scaffolding, but they are not acceptable as the milestone's production-like source of truth.

**Primary recommendation:** Plan Phase 12 around a single committed production-like Compose entrypoint that reuses the existing Jib image and helper manifests, moves all environment-specific values to env-driven placeholders, preserves Mailpit/PostgreSQL/Actuator readiness behavior, and proves brownfield-safe auth/account/admin-user/mail/secured-entity flows using existing IT and Playwright coverage.

## Project Constraints (from CLAUDE.md)

- Preserve functional security capabilities from `angapp` for CRUD, attribute permissions, secure merge, and fetch-plan-driven reads.
- Row-policy behavior is retired and must not drive scope.
- Active UI is `frontend/`; `aef-main/aef-main` is reference-only.
- Use PrimeNG-first patterns when UI/runtime docs touch frontend behavior.
- Fetch plans must remain YAML or code-builder based; never move them to database storage.
- Brownfield-safe auth, account, admin-user, and mail flows must not regress.
- Keep minimal JHipster account/user API contracts where required.
- Keep performance-conscious runtime behavior; avoid excessive chatter and eager loading assumptions.
- Use typed configuration patterns via Spring/ApplicationProperties where code configuration changes are needed.
- Preserve `jhipster-needle-*` markers.
- Treat `application-secret-samples.yml` and `application-tls.yml` as sensitive.
- Keep frontend API bases backend-relative via `ApplicationConfigService.getEndpointFor(...)` and current env conventions.
- Follow split-runtime architecture: Spring Boot backend plus standalone Angular frontend.
- Do not infer real deployment IP/proxy/port details; ask the user if those become necessary.

## Standard Stack

### Core
| Library / Tool | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Docker Compose | v2.34.0-desktop.1 (installed) | Start committed multi-service baseline | Official Compose supports dependency startup ordering plus `service_healthy`, matching the current repo structure. |
| Jib Gradle plugin + existing `jhipster.docker-conventions` | Repo-managed, image target `jhipster-sec:latest` | Build backend runtime image without a handwritten Dockerfile | JHipster already uses this path; Jib is the standard Java image build path here and preserves one packaging story. |
| Spring Boot externalized config | Boot 4.0.3 in project | Resolve runtime values from env vars / profile config | Official Boot precedence makes env vars the correct override mechanism for `application-prod.yml`. |
| PostgreSQL container | `postgres:18.3` | Production-like database dependency | Same major image is already used in Compose and Testcontainers, reducing drift between runtime and integration tests. |
| Mailpit container | `axllent/mailpit:v1.27` | Safe SMTP sink for validation | Already present in repo and aligned with brownfield-safe mail validation without requiring real SMTP. |
| Spring Boot Actuator health groups | Boot 4.0.3 in project | Readiness/liveness/health verification | Already enabled in `application.yml`; official probes support production-like startup validation. |

### Supporting
| Library / Tool | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Backend image entrypoint `_FILE` env support | Existing `src/main/docker/jib/entrypoint.sh` | Resolve direct env vars or file-backed secrets | Use when Compose or future runtime wants direct env values now and file/secret indirection later. |
| JHipster Docker asset layout | JHipster 9.0.0 project pattern | Keep `app.yml` plus service-specific manifests under `src/main/docker/` | Use as the default baseline structure instead of inventing a custom deployment layout. |
| Management health endpoints | Existing `/management/health` plus liveness/readiness groups | Gate service readiness and future validation | Use for app health checks, Compose waits, and later benchmark/security proof entry points. |
| Existing regression tests | Repo-local | Prove runtime safety of auth/account/admin-user/mail/secured entities | Use as the required validation surface for PROD-02. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Reusing Jib image path | Handwritten Dockerfile | Adds a second packaging path and configuration drift for no milestone benefit. |
| Layered Compose files under `src/main/docker/` | New monolithic runtime manifest elsewhere | Simpler on paper, but breaks existing JHipster conventions and duplicates service definitions. |
| Env placeholders in `application-prod.yml` | Hardcoded host/user/password values | Hardcoding is incompatible with D-01/D-02 and leaks machine-specific config into source control. |
| Mailpit baseline | Real SMTP provider | More realistic for production, but explicitly deferred and unnecessary for this milestone baseline. |

**Installation / baseline commands:**
```bash
npm run java:docker:prod
npm run app:up
```

**Version verification:**
- Installed locally: Docker Compose `v2.34.0-desktop.1`, Node `v24.14.0`, npm `11.9.0`, Gradle wrapper `9.4.0`, Java `25.0.2`.
- Verified npm package versions and publish dates:
  - `generator-jhipster` `9.0.0` — published 2026-03-11
  - `@playwright/test` `1.59.1` — latest as of 2026-04-01; repo currently uses `^1.58.2`
  - `vitest` `4.1.2` — latest as of 2026-03-26; repo currently uses `^4.0.8`
- Container image tags verified from repo assets:
  - PostgreSQL `18.3`
  - Mailpit `v1.27`

## Architecture Patterns

### Recommended Project Structure
```text
src/main/docker/
├── app.yml              # Primary production-like stack entrypoint (app + dependencies)
├── services.yml         # Dependency-only startup for tests/CI helpers
├── postgresql.yml       # Database service definition with real password env, healthcheck, volume policy
├── mailpit.yml          # Mail sink service and healthcheck
└── jib/
    └── entrypoint.sh    # Existing env/_FILE resolution for backend container

src/main/resources/config/
├── application.yml      # Shared management/actuator/runtime defaults
└── application-prod.yml # Profile-specific overrides using env placeholders only
```

### Pattern 1: Environment-driven prod profile with packaged defaults
**What:** Keep `application-prod.yml` committed, but replace machine-specific values with `${...}` placeholders and sensible defaults only where a default is safe.
**When to use:** For datasource, Liquibase, mail host/port, base URL, management toggles, and any runtime values that differ across environments.
**Example:**
```yaml
# Source: https://docs.spring.io/spring-boot/reference/features/external-config.html
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
```

### Pattern 2: Compose waits on service health, not just container start
**What:** Use explicit `healthcheck` blocks and `depends_on.condition: service_healthy` for app dependencies.
**When to use:** For PostgreSQL and Mailpit before the backend starts.
**Example:**
```yaml
# Source: https://docs.docker.com/compose/how-tos/startup-order/
services:
  app:
    depends_on:
      db:
        condition: service_healthy
```

### Pattern 3: Keep one backend packaging path
**What:** Build `jhipster-sec:latest` with the existing Gradle/Jib convention and run that image from Compose.
**When to use:** Always for this phase; do not introduce Dockerfile-based duplication.
**Example:**
```groovy
// Source: D:\jhipster\.claude\worktrees\agent-ab757e2a\buildSrc\src\main\groovy\jhipster.docker-conventions.gradle
jib {
  to {
    image = 'jhipster-sec:latest'
  }
}
```

### Pattern 4: Production-like health endpoints on the main app port
**What:** Keep Actuator health groups and expose readiness on the app port used by Compose health checks.
**When to use:** For backend startup orchestration and later validation phases.
**Example:**
```yaml
# Source: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        readiness:
          include: readinessState,db
```

### Anti-Patterns to Avoid
- **Hardcoded infrastructure in `application-prod.yml`:** Current fixed DB host, username, and password violate D-01/D-02 and make the stack non-portable.
- **Second image build path:** Adding Dockerfiles or bespoke shell packaging splits the runtime contract.
- **`POSTGRES_HOST_AUTH_METHOD=trust` as baseline security:** Good for throwaway local scaffolding, not for a production-like milestone baseline.
- **Frontend absolute API base branches:** `SERVER_API_URL` is already empty in both frontend env files; preserve that rather than adding per-environment frontend branching now.
- **Treating `/management/health` as enough proof alone:** Readiness should include DB, and preserved business behavior still needs app/mail/security regression checks.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Java container packaging | Custom Dockerfile pipeline | Existing Jib Gradle setup | JHipster already standardizes this; extra packaging paths drift fast. |
| Secret file resolution | New shell/env adapter | Existing `_FILE` support in `src/main/docker/jib/entrypoint.sh` | Already handles direct env and file-backed values cleanly. |
| Service startup orchestration | Ad hoc wait scripts | Compose healthchecks + `depends_on: service_healthy` | Official Compose behavior is clearer and easier to validate. |
| SMTP simulation | Fake mail code or bypassed mail paths | Mailpit | Preserves actual SMTP-ish behavior without external provider coupling. |
| Runtime config injection | Repo-local machine-specific constants | Spring Boot externalized config placeholders | Official precedence model supports env overrides cleanly. |

**Key insight:** The repo already contains nearly every required runtime primitive. Phase 12 should standardize and tighten them, not replace them.

## Common Pitfalls

### Pitfall 1: Leaving hardcoded datasource settings in `application-prod.yml`
**What goes wrong:** The committed prod profile only works for one machine or leaked server, and Compose/env inputs do not actually control runtime.
**Why it happens:** Generated or temporary values get checked in and never normalized.
**How to avoid:** Convert datasource and Liquibase settings to env placeholders; require the Compose stack to provide those values.
**Warning signs:** Public IPs, literal usernames/passwords, or mismatch between Compose env and Spring profile config.

### Pitfall 2: Calling the stack “production-like” while still using trust auth
**What goes wrong:** Database auth behavior is unrealistically permissive, weakening confidence in later validation phases.
**Why it happens:** `postgresql.yml` is still in a dev-scaffolded state with `POSTGRES_HOST_AUTH_METHOD=trust`.
**How to avoid:** Switch to explicit database name, user, and password env wiring and make the app consume the same values.
**Warning signs:** Empty `SPRING_DATASOURCE_PASSWORD`, trust auth, or docs warning that the file is for development only.

### Pitfall 3: Starting app before dependencies are actually ready
**What goes wrong:** Flaky boot, Liquibase failures, intermittent health failures, and misleading validation noise.
**Why it happens:** Running containers only to “started” instead of “healthy”.
**How to avoid:** Keep service healthchecks and use `depends_on.condition: service_healthy`; make app readiness include `db`.
**Warning signs:** Intermittent startup failures, retries, or first-boot-only errors.

### Pitfall 4: Preserving localhost-only assumptions in runtime links
**What goes wrong:** Mail links or browser-targeted URLs are invalid once the stack moves beyond a single workstation.
**Why it happens:** `JHIPSTER_MAIL_BASE_URL=http://localhost:4200` is left as baseline.
**How to avoid:** Keep backend-relative frontend API behavior, but move mail base URL to env-driven runtime input and ask the user for real topology if needed.
**Warning signs:** Emails linking to localhost from a non-local stack.

### Pitfall 5: Accidentally expanding Phase 12 into full production hardening
**What goes wrong:** Planning scope balloons into TLS, external secrets, monitoring, ingress, or SMTP integration.
**Why it happens:** “Production-like” gets misread as “internet-ready production.”
**How to avoid:** Keep scope to startable baseline runtime, realistic dependencies, and preserved validation targets; defer hardening per D-10.
**Warning signs:** Tasks about cert issuance, Nginx Proxy Manager specifics, public DNS, or full observability rollout.

## Code Examples

Verified patterns from official and repo sources:

### Externalize profile config via placeholders
```yaml
# Source: https://docs.spring.io/spring-boot/reference/features/external-config.html
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  mail:
    host: ${SPRING_MAIL_HOST:mailpit}
    port: ${SPRING_MAIL_PORT:1025}
```

### Gate backend startup on healthy dependencies
```yaml
# Source: https://docs.docker.com/compose/how-tos/startup-order/
services:
  app:
    depends_on:
      postgresql:
        condition: service_healthy
      mailpit:
        condition: service_healthy
```

### Preserve file-backed secret support in container startup
```bash
# Source: D:\jhipster\.claude\worktrees\agent-ab757e2a\src\main\docker\jib\entrypoint.sh
file_env 'SPRING_DATASOURCE_URL'
file_env 'SPRING_DATASOURCE_USERNAME'
file_env 'SPRING_DATASOURCE_PASSWORD'
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Handwritten Dockerfile-centric Java image builds | Jib-based Java image builds | Established modern Java container workflow; repo already uses it in 2026 | Keep one image path and faster/reproducible builds. |
| “Container started” treated as ready | Compose `service_healthy` plus app readiness checks | Current Compose docs | Startup orchestration should depend on health, not mere process launch. |
| Machine-specific prod config in source | Externalized config via env/system/CLI precedence | Longstanding Spring Boot standard, current docs verified 2026 | Phase 12 should move fixed runtime values out of source and into env inputs. |
| Health only as generic app ping | Liveness/readiness groups with DB-aware readiness | Current Spring Boot Actuator docs | Better fit for production-like baseline and later benchmark/security phases. |

**Deprecated/outdated:**
- Hardcoded prod datasource host/user/password in committed `application-prod.yml`: outdated for this milestone baseline.
- `POSTGRES_HOST_AUTH_METHOD=trust` in the baseline database manifest: acceptable for local scaffolding, not for a production-like milestone default.
- Treating `prod,api-docs` as an unquestioned production-like default: operationally dubious unless Phase 12 explicitly needs docs exposed inside the validation stack.

## Open Questions

1. **What exact env variable contract should the stack publish?**
   - What we know: Spring Boot env placeholders, Compose env blocks, and `entrypoint.sh` `_FILE` support all exist now.
   - What's unclear: Final variable names/default policy for datasource, Liquibase, mail base URL, and optional metrics toggles.
   - Recommendation: Planner should define one canonical env contract and make Compose plus `application-prod.yml` use it consistently.

2. **Should the primary stack keep `api-docs` enabled?**
   - What we know: `app.yml` currently sets `SPRING_PROFILES_ACTIVE=prod,api-docs`.
   - What's unclear: Whether later validation phases actually require Swagger/OpenAPI in the runtime baseline.
   - Recommendation: Default to `prod` only unless a downstream validation step explicitly depends on API docs.

3. **What is the real browser-facing base URL for mail links in milestone validation?**
   - What we know: Frontend uses backend-relative API URLs; mail base URL is currently localhost-based; CLAUDE.md says ask the user for real deployment details when needed.
   - What's unclear: Whether Phase 12 validation runs only locally, behind Nginx Proxy Manager over VPN, or via another internal hostname.
   - Recommendation: Keep env-driven mail base URL and add a planning checkpoint to ask the user for actual host/proxy details before implementation if links must be realistic.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker | Compose runtime baseline | ✓ | 28.0.4 | — |
| Docker Compose | `app.yml` / `services.yml` startup | ✓ | v2.34.0-desktop.1 | — |
| Java | Gradle build and backend runtime checks | ✓ | 25.0.2 | — |
| Gradle wrapper | Backend build / Jib image build | ✓ | 9.4.0 | — |
| Node.js | npm scripts and frontend validation | ✓ | v24.14.0 | — |
| npm | Package scripts | ✓ | 11.9.0 | — |
| Bash | Existing Jib entrypoint script | ✓ | 5.2.37 | — |

**Missing dependencies with no fallback:**
- None found during audit.

**Missing dependencies with fallback:**
- None found during audit.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Spring Boot Test + MockMvc + Testcontainers PostgreSQL; Angular unit tests via Angular builder/Vitest globals; Playwright E2E 1.58.x in repo (`1.59.1` latest) |
| Config file | `D:\jhipster\.claude\worktrees\agent-ab757e2a\src\test\java\com\vn\core\IntegrationTest.java`, `D:\jhipster\.claude\worktrees\agent-ab757e2a\frontend\angular.json`, `D:\jhipster\.claude\worktrees\agent-ab757e2a\frontend\playwright.config.ts` |
| Quick run command | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT --tests com.vn.core.web.rest.UserResourceIT --tests com.vn.core.web.rest.SecuredEntityCapabilityResourceIT --tests com.vn.core.web.rest.SecuredEntityEnforcementIT --tests com.vn.core.service.MailServiceIT` |
| Full suite command | `npm run ci:backend:test && cd frontend && npm run test && npm run e2e` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PROD-01 | Committed production-like stack starts with DB and mail dependencies and app becomes healthy | smoke / stack | `npm run java:docker:prod && npm run app:up` | ✅ |
| PROD-01 | Prod profile remains externally configurable and DB-backed startup still works | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT` | ✅ |
| PROD-02 | Auth and account flows remain correct | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT` | ✅ |
| PROD-02 | Admin-user flows remain correct | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.UserResourceIT` | ✅ |
| PROD-02 | Secured-entity capability flow remains correct | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.SecuredEntityCapabilityResourceIT` | ✅ |
| PROD-02 | Secured-entity enforcement remains correct | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.web.rest.SecuredEntityEnforcementIT` | ✅ |
| PROD-02 | Mail behavior remains correct | integration | `D:/jhipster/.claude/worktrees/agent-ab757e2a/gradlew integrationTest --tests com.vn.core.service.MailServiceIT` | ✅ |
| PROD-02 | Browser-level security and role gating remain correct in realistic runtime shape | e2e | `cd frontend && npm run e2e -- security-comprehensive.spec.ts proof-role-gating.spec.ts` | ✅ |

### Sampling Rate
- **Per task commit:** Targeted backend IT command for the subsystem touched, plus `npm run app:up` when Compose/runtime files changed.
- **Per wave merge:** `npm run ci:backend:test` and the targeted Playwright specs for auth/security flows.
- **Phase gate:** Production-like stack starts from committed assets and targeted backend + browser validation stays green before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] Add one explicit stack smoke script/assertion that checks app container health against the production-like Compose baseline after `npm run app:up` — covers REQ-PROD-01.
- [ ] Add one repeatable validation command or doc wrapper for running Playwright against the production-like stack rather than only `localhost:4200` dev assumptions — covers REQ-PROD-02.
- [ ] Clarify whether `frontend` E2E in this phase targets a separately served SPA or a future same-origin deployment shape; current Playwright config assumes `http://localhost:4200`.

## Sources

### Primary (HIGH confidence)
- Repository evidence: `D:\jhipster\.claude\worktrees\agent-ab757e2a\src\main\docker\app.yml`, `services.yml`, `postgresql.yml`, `mailpit.yml`, `jib\entrypoint.sh`
- Repository evidence: `D:\jhipster\.claude\worktrees\agent-ab757e2a\src\main\resources\config\application.yml`, `application-prod.yml`
- Repository evidence: `D:\jhipster\.claude\worktrees\agent-ab757e2a\package.json`, `frontend\package.json`, `frontend\playwright.config.ts`, `build.gradle`, `frontend\angular.json`
- Spring Boot External Config reference - https://docs.spring.io/spring-boot/reference/features/external-config.html
- Spring Boot Actuator Endpoints reference - https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot Profiles reference - https://docs.spring.io/spring-boot/reference/features/profiles.html
- Docker Compose startup order docs - https://docs.docker.com/compose/how-tos/startup-order/
- JHipster Docker Compose docs - https://www.jhipster.tech/docker-compose/
- Jib official repository docs - https://github.com/GoogleContainerTools/jib

### Secondary (MEDIUM confidence)
- Mailpit docs landing page checked for health-check guidance - https://mailpit.axllent.org/docs/
- npm registry verification for package versions/publish dates via `npm view`

### Tertiary (LOW confidence)
- Mailpit health endpoint path implied by repo usage (`/mailpit readyz`) but not confirmed from current official docs page fetched here; keep validation on repo evidence until re-verified.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Direct repo evidence plus official Spring Boot, Docker Compose, JHipster, and Jib documentation agree on the baseline approach.
- Architecture: HIGH - Current repo structure already embodies the recommended pattern; planning work is normalization rather than speculative redesign.
- Pitfalls: HIGH - Most pitfalls are visible directly in current checked-in runtime files (`application-prod.yml`, `app.yml`, `postgresql.yml`) and reinforced by official docs.

**Research date:** 2026-04-02
**Valid until:** 2026-05-02
