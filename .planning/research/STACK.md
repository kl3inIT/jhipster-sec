# Technology Stack

**Project:** JHipster Security Platform
**Research type:** stack
**Researched:** 2026-04-01
**Milestone:** v1.2 CI/CD & Production Validation

## Recommended Stack Additions

| Area | Recommendation | Why | Integration Notes |
|------|----------------|-----|-------------------|
| Backend CI | GitHub Actions with Java 25 + Gradle caching | JHipster treats CI as a dual-stack problem | Run backend verification independently from frontend |
| Frontend CI | GitHub Actions with Node 24 + npm cache | Standalone `frontend/` has its own build/test lifecycle | Run `npm ci`, test, and prod build in `frontend/` |
| Backend image build | Reuse existing Jib setup | Already aligned with JHipster production container flow | Wire existing image build into CI |
| Frontend runtime | nginx-serving Angular production build | Needed for production-like Compose deployment | Keep frontend as a separate production container |
| Production config | Env/secrets-driven `application-prod.yml` | Current prod config contains concrete DB values | Harden before any deployment automation |
| Compose deployment | Minimal stack: frontend + app + PostgreSQL | Matches requested Docker Compose target | Add health/readiness checks |
| Permission lookup | Pre-index request-lifecycle permission data | Avoid repeated parsing/scans as matrix cardinality grows | Keep optimization behind existing permission seam |
| Security validation | Reuse existing k6 + integration tests | Need proof under production conditions, not only dev benchmarks | Combine deterministic integration coverage with Compose-targeted validation |

## Keep Using Existing Stack

- Java 25
- Spring Boot 4.0.3
- JHipster 9.0.0
- Gradle 9.4.0
- Angular 21.2.x
- PostgreSQL
- Hazelcast 5.5.0
- k6
- Docker Compose

## Do Not Add In v1.2

- Kubernetes / Helm
- New load-testing platform beyond k6
- New cache technology beyond Hazelcast
- Full browser E2E expansion (TEST-01 to TEST-03 stay deferred)
- Multi-node deployment scope

## Key Stack Findings

1. **Dual-stack CI is required.** Backend and frontend need separate workflows, then an integration gate.
2. **Production config hardening is first.** `application-prod.yml` currently contains concrete datasource host/password.
3. **Reuse existing Jib/Docker assets.** The repo already has a strong base for backend containerization.
4. **Permission optimization is about lookup shape.** The matrix path is a real performance target as entity/attribute count grows.
5. **Jmix’s useful lesson is pre-indexing.** Preprocess once, then do cheap lookups during authorization.

## Sources

- JHipster CI docs: https://www.jhipster.tech/setting-up-ci/
- JHipster production docs: https://www.jhipster.tech/production/
- Local config/build review: `build.gradle`, `buildSrc/**`, `src/main/resources/config/application-prod.yml`, `src/main/docker/*`
