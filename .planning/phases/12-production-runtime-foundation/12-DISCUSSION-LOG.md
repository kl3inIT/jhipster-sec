# Phase 12: Production Runtime Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `12-CONTEXT.md`.

**Date:** 2026-04-02
**Phase:** 12-production-runtime-foundation
**Mode:** discuss

## Boundary Reviewed

- Phase 12 establishes a production-like runtime stack from committed compose assets and runtime configuration.
- Scope is limited to the runtime foundation needed for milestone validation.
- CI automation, benchmark baseline design, optimization work, and final proof remain later phases.

## Codebase Signals Used

- Existing compose assets already exist in `src/main/docker/app.yml`, `src/main/docker/services.yml`, `src/main/docker/postgresql.yml`, and `src/main/docker/mailpit.yml`.
- Backend image packaging already exists via Jib in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- `src/main/docker/jib/entrypoint.sh` already supports env/file-based runtime value injection.
- `src/main/resources/config/application-prod.yml` currently contains hardcoded datasource host and credentials, making configuration-source policy a key decision.
- Frontend production API base remains relative in `frontend/src/environments/environment.ts`.

## Decisions Chosen

### Configuration source
- Use committed compose assets with environment-driven values.
- Remove machine-specific production datasource values from `application-prod.yml`.
- Ask the user for real server IP/proxy/config details when deployment-specific values are needed.

### Runtime topology
- Default production-like stack = backend + PostgreSQL + Mailpit.
- Keep frontend API usage backend-relative.
- Reuse existing Jib packaging and compose assets.

### Validation target
- Preserve and validate auth, account, admin-user, mail, and secured-entity flows in the runtime stack.
- Keep the stack suitable for later benchmark and security-validation phases.

### Operational scope
- Keep health checks and basic metrics hooks.
- Defer TLS, real SMTP, external secret manager, and full monitoring hardening.

### Standards
- Follow JHipster docs and best practices by default.

## User Guidance Captured

- User approved Claude choosing the defaults for this phase.
- User asked that JHipster docs/best practices be the default guide.
- User asked to be consulted for server-specific details such as IP or config when needed.

## Deferred Ideas

- CI lane separation and automation — Phase 13.
- Benchmark baseline and production-like validation evidence — Phase 14.
- Permission optimization — Phase 15.
- Final production security proof — Phase 16.
