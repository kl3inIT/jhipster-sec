# Phase 12: Production Runtime Foundation - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Establish a production-like runtime stack that can be started from committed compose assets and runtime configuration so milestone validation runs against a realistic environment instead of dev-only shortcuts. This phase covers the runtime foundation only: backend, database, mail sink, and the operational configuration needed to validate auth, account, admin-user, mail, and secured-entity behavior. It does not add CI automation, benchmark design, or production hardening beyond the baseline needed for realistic milestone validation.

</domain>

<decisions>
## Implementation Decisions

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

</decisions>

<specifics>
## Specific Ideas

- Follow JHipster docs and best practices for production/runtime decisions by default.
- If real deployment details become necessary during planning or implementation, ask the user for server-specific information such as IP, topology, proxy, ports, or config.
- Keep the runtime foundation realistic enough for later validation, but avoid turning Phase 12 into CI design or full production hardening.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase definition and milestone requirements
- `.planning/ROADMAP.md` — Phase 12 goal, dependencies, and success criteria.
- `.planning/REQUIREMENTS.md` — `PROD-01` and `PROD-02` plus the milestone-level production-validation scope.
- `.planning/PROJECT.md` — milestone goals, constraints, and key decisions that shape runtime and deployment choices.
- `.planning/STATE.md` — current milestone sequencing and pending concerns for v1.2.

### Existing runtime assets
- `src/main/docker/app.yml` — current full-stack compose entrypoint for backend + dependencies.
- `src/main/docker/services.yml` — existing dependency-only compose baseline.
- `src/main/docker/postgresql.yml` — database service definition that Phase 12 likely hardens or reshapes.
- `src/main/docker/mailpit.yml` — safe mail sink already wired into the current local stack.
- `src/main/docker/jib/entrypoint.sh` — current runtime env/file secret loading behavior for the backend container.
- `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle` — Jib image build conventions for the backend runtime image.

### Backend and frontend runtime configuration
- `src/main/resources/config/application.yml` — shared runtime defaults including cache/management behavior.
- `src/main/resources/config/application-prod.yml` — production profile overrides that currently include machine-specific datasource settings and must be normalized.
- `frontend/src/environments/environment.ts` — production frontend API base behavior.
- `frontend/src/environments/environment.development.ts` — development frontend API base behavior for comparison.

### Existing validation targets
- `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` — account flow regression target.
- `src/test/java/com/vn/core/web/rest/UserResourceIT.java` — admin-user flow regression target.
- `src/test/java/com/vn/core/web/rest/SecuredEntityCapabilityResourceIT.java` — secured-entity capability/runtime regression target.
- `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java` — secured-entity enforcement regression target.
- `src/test/java/com/vn/core/service/MailServiceIT.java` — mail behavior regression target.
- `frontend/e2e/security-comprehensive.spec.ts` — end-to-end security flow validation target.
- `frontend/e2e/proof-role-gating.spec.ts` — end-to-end permission gating validation target.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/docker/app.yml` and `src/main/docker/services.yml` already split full-stack vs dependency-only runtime patterns that Phase 12 can refine instead of replacing.
- `src/main/docker/mailpit.yml` already provides a safe non-production mail target suitable for milestone validation.
- `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle` already builds `jhipster-sec:latest` via Jib, so backend container packaging exists.
- `src/main/docker/jib/entrypoint.sh` already supports `_FILE`-based secret/env indirection for datasource and Liquibase values.
- Existing backend integration tests and frontend E2E specs already define the behavior that must keep working in the production-like runtime.

### Established Patterns
- Docker assets live under `src/main/docker/` and are composed through small specialized files rather than a single monolithic manifest.
- Spring Boot runtime config is profile-driven with `application.yml` + `application-prod.yml` overrides.
- Frontend production runtime currently assumes relative API calls, which fits reverse-proxy or same-origin deployment patterns.
- JHipster/Jib container packaging is already the backend image path, so Phase 12 should extend that instead of introducing another image build tool.

### Integration Points
- Backend runtime configuration: `src/main/resources/config/application-prod.yml`, possibly `src/main/resources/config/application.yml`.
- Compose/runtime manifests: `src/main/docker/app.yml`, `src/main/docker/services.yml`, `src/main/docker/postgresql.yml`, and related helper manifests.
- Container startup wiring: `src/main/docker/jib/entrypoint.sh` and `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- Frontend deployment assumptions: `frontend/src/environments/environment.ts` and any runtime URL expectations tied to mail links or reverse-proxy routing.
- Validation surface: backend ITs and frontend Playwright specs covering auth, account, admin-user, mail, and secured-entity flows.

</code_context>

<deferred>
## Deferred Ideas

- Repository-native CI/CD and split verification lanes — Phase 13.
- Production-like benchmark dataset design, concurrency coverage, and stack-validation reporting — Phase 14.
- Permission-path optimization work — Phase 15.
- Final production security proof package — Phase 16.
- Real SMTP provider integration, TLS termination strategy, external secret manager integration, and full monitoring/log aggregation rollout — outside Phase 12 baseline unless required to make the stack realistically startable.

</deferred>

---

*Phase: 12-production-runtime-foundation*
*Context gathered: 2026-04-02*