# Phase 13: Split CI Verification Lanes - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md u2014 this log preserves the discussion.

**Date:** 2026-04-06
**Phase:** 13-split-ci-verification-lanes
**Mode:** discuss
**Areas discussed:** CI provider, Lane structure + E2E placement, Production-validation lane triggers, Path-based filtering

## Gray Areas Identified

| Area | Question | Options Presented |
|------|----------|------------------|
| CI provider | Which provider? | GitHub Actions (recommended), Local scripts only, Other |
| Lane structure | How many lanes and where does Playwright fit? | 2 lanes + prod-validation (recommended), 3 lanes + prod-validation |
| Prod-validation triggers | When does the compose-backed lane run? | Manual only (recommended), Scheduled + manual, Every push to main + manual |
| Path filtering | Skip irrelevant lanes on focused changes? | No filtering \u2014 always run both (recommended), Yes \u2014 path-filter each lane |

## Decisions Made

### CI Provider
- **Selected:** GitHub Actions (recommended)
- **Rationale:** Standard for GitHub repos; JHipster 9 docs reference GitHub Actions as CI baseline.

### Lane Structure
- **Selected:** 2 lanes + prod-validation
  - `backend.yml`: Gradle compile + unit/integration tests + code quality
  - `frontend.yml`: Angular build + ng test + Playwright E2E (starts backend jar + services.yml)
  - `prod-validation.yml`: Docker Compose stack + phase12:stack:smoke + phase12:backend:prodlike
- **Rationale:** Keeps YAML maintainable at 2 primary files; existing JHipster npm scripts already define the CI entry points.

### Production-Validation Triggers
- **Selected:** Manual only (workflow_dispatch)
- **Rationale:** Compose-stack validation takes minutes; keeping it off push/PR keeps developer feedback fast. Run on demand before milestone merges or releases.

### Path-Based Filtering
- **Selected:** No filtering u2014 always run both lanes
- **Rationale:** Simpler setup; catches cross-cutting regressions (e.g., API contract changes that break frontend). Can add filtering later if CI costs become a concern.

## Corrections Made

No corrections u2014 all recommended defaults accepted.

## Codebase Findings

- No `.github/` directory exists u2014 Phase 13 creates it from scratch.
- `package.json` already contains JHipster-generated `ci:*` npm scripts (`ci:backend:test`, `ci:e2e:package`, `ci:e2e:prepare:docker`, `ci:e2e:server:start`, `ci:e2e:teardown`) that map directly to lane steps.
- `scripts/phase12-stack-smoke.sh` and `scripts/phase12-prodlike-regression.mjs` are the committed production-validation entry points from Phase 12.
- Backend integration tests use Testcontainers PostgreSQL (requires Docker on CI runner).
- Playwright config targets `localhost:4200` with headless Chromium.
