# Feature Landscape

**Domain:** CI/CD, production deployment, permission optimization, and security validation for JHipster Security Platform
**Researched:** 2026-04-01
**Milestone:** v1.2 CI/CD & Production Validation

## Table Stakes

Features the v1.2 milestone must ship. Missing any of these means the platform lacks operational maturity and cannot prove production readiness.

| Feature | Why Expected | Complexity | Dependencies | Notes |
|---------|--------------|------------|--------------|-------|
| GitHub Actions CI workflow for backend (Gradle + Java 25) | JHipster explicitly documents CI/CD as a first-class concern because of the dual-stack build requirement. Without automated backend CI, regressions slip through on every push. | Low | None - Gradle build already works locally | JHipster recommends `jhipster ci-cd` generator but the generated workflow needs customization for Java 25 + separate frontend. Standard pattern: checkout, setup-java, setup-gradle, `./gradlew -Pprod check`, Testcontainers for integration tests. |
| GitHub Actions CI workflow for frontend (Angular 21 + Node 24) | The frontend is a standalone app in `frontend/` with its own build toolchain. It must be validated independently because backend changes do not trigger frontend failures and vice versa. | Low | None - `npm run build` already works | Standard pattern: checkout, setup-node with npm cache, `npm ci`, `npm run lint`, `npm run test -- --watch=false`, `npm run build -- --configuration production`. |
| Jib-based Docker image build for backend | The project already has `jhipster.docker-conventions.gradle` with Jib configured targeting `jhipster-sec:latest` on `eclipse-temurin:25-jre-noble`. This is the JHipster-standard production image pattern. Must be wired into CI. | Low | Backend CI workflow | Already configured: `./gradlew -Pprod bootJar jibDockerBuild`. Just needs CI integration. |
| Docker Compose production stack (backend + PostgreSQL + frontend) | JHipster generates `src/main/docker/app.yml` as the production entry point. The project has `postgresql.yml` and `services.yml` but no `app.yml` yet (it exists in `angapp/` reference). Production proof requires a working multi-container stack. | Medium | Jib image build, frontend production build | Need: `app.yml` with backend (prod profile), PostgreSQL (with auth), and nginx serving frontend static assets. The `entrypoint.sh` already supports Docker secrets via `file_env`. |
| Spring Boot `prod` profile hardening | The existing `application-prod.yml` has hardcoded credentials (`password: admin123`) and a remote database URL. Production deployment must use environment variables or Docker secrets for all sensitive config. | Low | None | The `entrypoint.sh` already handles `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` via `file_env`. Just need to update `application-prod.yml` to use `${...}` placeholders. |
| Frontend production build with nginx container | The standalone `frontend/` app needs a production container. Standard pattern is multi-stage Dockerfile: Node build stage then nginx serve stage. The Angular CLI `--configuration production` flag handles AOT, tree-shaking, and minification. | Medium | None | Need `frontend/Dockerfile` and `frontend/nginx.conf` with API reverse proxy to backend service. |
| Permission matrix pre-processing into indexed lookup | The current `PermissionMatrix` uses `Set<String>` with string-key lookups (`"ENTITY:TARGET:ACTION"`). This is already O(1) via `HashSet.contains()`. However, `isAttributePermitted()` does string splitting on every call (`attrTarget.split("\\.")`) and checks up to 4 keys. At 200 entities with 10+ attributes each, the per-attribute overhead adds up across serialization. | Medium | Existing `PermissionMatrix`, `RequestPermissionSnapshot` | Jmix pattern: `ResourcePoliciesIndex` pre-groups policies into `Map<String, Map<String, List<ResourcePolicy>>>` (type -> resource -> policies) at construction time. The current project's `PermissionMatrix` is simpler (flat `Set<String>`) but should adopt the same principle: pre-split attribute targets at construction, pre-build entity-name groupings for attribute wildcards. |
| Request-path optimization audit | Phase 11 achieved PERF-04 (p95 overhead < 10%). v1.2 must verify this holds under production conditions and identify any remaining per-request overhead. The `SecureDataManagerImpl` pipeline has 6 steps: catalog lookup, CRUD check, specification build, load, fetch-plan resolve, serialize. Each should be profiled. | Medium | k6 infrastructure (already exists), production Docker stack | The catalog already caches entries in `MetamodelSecuredEntityCatalog.cachedEntries` (constructed once at boot). Fetch-plan resolution uses `YamlFetchPlanRepository`. Both are already efficient. Focus should be on serialization overhead under production JVM settings. |
| Security regression tests under production conditions | The security pipeline must be proven correct under production profile, not just dev/test. This means: JWT auth works end-to-end, permission enforcement blocks unauthorized access, attribute filtering strips fields, cache eviction propagates. | High | Production Docker stack, test data seeding | This is the core validation deliverable. Must cover: (1) authenticated CRUD allowed/denied, (2) attribute visibility filtering, (3) permission change propagation via Hazelcast cache eviction, (4) multi-role union-of-ALLOW semantics. |
| Production deployment proof | A documented, reproducible `docker compose up` that starts the full stack and passes a smoke test suite. This is the milestone's exit criterion. | Medium | All above | Combines Docker Compose stack + security regression + k6 benchmarks running against the production containers. |

## Differentiators

Features that go beyond table stakes and demonstrate enterprise-grade operational maturity. Not strictly required but high-value for the project's goals.

| Feature | Value Proposition | Complexity | Dependencies | Notes |
|---------|-------------------|------------|--------------|-------|
| Jmix-informed `ResourcePoliciesIndex`-style permission structure | Replace flat `Set<String>` in `PermissionMatrix` with a two-level `Map<TargetType, Map<String, Set<String>>>` that groups policies by type and target at construction time. Eliminates per-call string splitting and wildcard fallback iteration. | Medium | Understanding of Jmix `ResourceRole.ResourcePoliciesIndex` (researched) | Jmix source confirms: policies are grouped into `Map<String, Map<String, List<ResourcePolicy>>>` keyed by `(policyType, resource)` at role-set time. Construction cost is O(n) once; lookups are O(1) HashMap get. The current project's `HashSet<String>` approach is already fast but the attribute path does `split(".")` on every call. Pre-indexing attribute permissions by entity name at construction eliminates this. |
| CI matrix for multiple environments (dev + prod profiles) | Run backend tests under both `testdev` and `testprod` Spring profiles in CI. The project already defines both (`springTestProfiles` in `spring-boot.gradle`). Running both catches profile-specific config bugs before deployment. | Low | Backend CI workflow | Already supported by Gradle: `./gradlew test` uses `test,testdev`, `./gradlew -Pprod test` uses `test,testprod`. Just add a matrix job. |
| k6 load tests in CI against Docker Compose stack | Run the existing k6 benchmark scripts (`load-tests/scripts/org-list-benchmark.js`) against the production Docker Compose stack in CI. Catches performance regressions automatically. | Medium | Production Docker stack in CI, k6 installed in runner | The k6 scripts already output markdown summaries and JSON. CI can archive these as artifacts and fail on threshold violations (the scripts already define `thresholds`). |
| Health check and readiness probe configuration | Spring Boot Actuator health endpoints (`/management/health`) already exist. Wire them into Docker Compose `healthcheck` and document for production use. The `angapp/src/main/docker/app.yml` already shows this pattern. | Low | Docker Compose stack | Standard JHipster pattern: `test: ['CMD', 'curl', '-f', 'http://localhost:8080/management/health']`. |
| Permission matrix Hazelcast serialization optimization | The `PermissionMatrix` is cached in Hazelcast but uses default Java serialization. For cross-request caching at scale, a compact serializer (Hazelcast `IdentifiedDataSerializable` or `Compact`) reduces network overhead and GC pressure. | Medium | Hazelcast configuration | Only matters at cluster scale. For single-node Docker Compose deployment this is premature. Flag for later if multi-node Hazelcast is needed. |
| Automated security test suite as reusable regression harness | Structure security validation tests so they can run as both: (1) Spring Boot integration tests (`@SpringBootTest` with Testcontainers) and (2) external HTTP tests against the Docker Compose stack. Same assertions, two execution modes. | High | Test infrastructure | The integration test mode already exists. The external mode needs a lightweight HTTP client test runner (could be k6 scripts with assertion-heavy checks, or a Gradle `integrationTest` task pointing at an external URL). |

## Anti-Features

Features to explicitly NOT build in v1.2.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Kubernetes / Helm deployment | Premature for this milestone. Docker Compose is the production target. K8s adds orchestration complexity that provides no value until multi-replica scaling is needed. | Stick with Docker Compose. The Jib images are K8s-compatible when the time comes. |
| Full Playwright E2E in CI | The milestone explicitly defers TEST-01/02/03 to v1.3. Running Playwright against the Docker stack would be valuable but is out of scope. | Keep Playwright as local-only for now. v1.3 will add it to CI. |
| Multi-node Hazelcast cluster in Docker Compose | Single-node Hazelcast is sufficient for production validation. Multi-node adds service discovery complexity and is only needed for HA deployment. | Use embedded Hazelcast in the single backend container. The `5701/udp` port in Jib config is already exposed for future clustering. |
| Database-backed fetch plan storage | Explicitly out of scope per project constraints. Fetch plans stay YAML-only. | Continue using `fetch-plans.yml` loaded by `YamlFetchPlanRepository`. |
| Custom Docker registry / artifact publishing | Publishing to DockerHub or a private registry is not needed for production validation. Local `jibDockerBuild` produces a local image that Docker Compose can reference. | Build and use local images. Add registry push as a future CI enhancement if needed. |
| Grafana/Prometheus monitoring stack in CI | The project has `monitoring.yml`, `prometheus.yml`, and Grafana dashboards but these are observability tools, not validation requirements. | Keep monitoring YAML files as opt-in local development tools. |
| Frontend SSR or pre-rendering | Angular standalone SPA served from nginx is the correct pattern. SSR adds server complexity for no security-platform benefit. | Stick with static `ng build` output served by nginx. |

## Feature Dependencies

```
GitHub Actions Backend CI --> Jib Docker Image Build --> Docker Compose Stack
GitHub Actions Frontend CI --> Frontend Dockerfile (nginx) --> Docker Compose Stack
Docker Compose Stack --> Security Regression Tests --> Production Deployment Proof
Docker Compose Stack --> k6 Load Tests in Production --> Performance Validation

PermissionMatrix Optimization --> Benchmark Validation (k6 against Docker stack)
PermissionMatrix Optimization (independent of CI/Docker, can parallel)

Spring Boot prod profile hardening --> Docker Compose Stack (needs env-var config)
```

Dependency ordering:
1. **CI workflows** and **prod profile hardening** can start immediately (no dependencies)
2. **Permission matrix optimization** can start immediately (code-only, no infra dependency)
3. **Docker Compose stack** requires CI image build and prod profile work
4. **Security regression tests** require the Docker Compose stack to be running
5. **Production deployment proof** is the final integration of everything

## MVP Recommendation

Prioritize in this order:

1. **GitHub Actions CI for both stacks** - Immediate regression safety net for all subsequent work. Backend workflow with Gradle + Testcontainers + Java 25, frontend workflow with Node 24 + Angular build + Vitest.

2. **Spring Boot prod profile hardening** - Remove hardcoded credentials from `application-prod.yml`, switch to environment variable placeholders. Required before Docker Compose production stack.

3. **Docker Compose production stack** - `app.yml` with backend (Jib image, prod profile), PostgreSQL (with password auth), nginx (serving frontend build, proxying API). This is the production environment that all validation runs against.

4. **Permission matrix pre-processing** - Adopt Jmix `ResourcePoliciesIndex` pattern: pre-group permissions by type and target at construction time. Eliminate per-call `String.split()` in `isAttributePermitted()`. Benchmark at 200-entity scale.

5. **Security regression validation** - Comprehensive test suite proving auth, CRUD enforcement, attribute filtering, and cache eviction work under production conditions.

6. **Production deployment proof** - Final integration: `docker compose up`, seed data, run security tests and k6 benchmarks, document results.

Defer to v1.3: Playwright E2E in CI (TEST-01/02/03), multi-node Hazelcast, Kubernetes.

## Jmix Authorization Pattern Analysis

The Jmix framework's approach to permission evaluation was researched by reading the actual source code from the `jmix-framework/jmix` GitHub repository. Key findings relevant to v1.2 optimization:

### How Jmix Indexes Permissions (HIGH confidence - source code verified)

Jmix's `ResourceRole.ResourcePoliciesIndex` is the critical optimization structure:

```java
// From jmix-security/security/src/main/java/io/jmix/security/model/ResourceRole.java
public static class ResourcePoliciesIndex implements Serializable {
    // type -> (resource -> List<ResourcePolicy>)
    private final Map<String, Map<String, List<ResourcePolicy>>> policiesByTypeAndResource;
    private final Map<String, List<ResourcePolicy>> policiesByType;

    ResourcePoliciesIndex(Collection<ResourcePolicy> policies) {
        policiesByType = policies.stream()
                .collect(Collectors.groupingBy(ResourcePolicy::getType));
        for (Map.Entry<String, List<ResourcePolicy>> entry : policiesByType.entrySet()) {
            Map<String, List<ResourcePolicy>> policiesByResource = entry.getValue().stream()
                    .collect(Collectors.groupingBy(ResourcePolicy::getResource));
            policiesByTypeAndResource.put(policyType, policiesByResource);
        }
    }
}
```

**Key design decisions:**
- Pre-groups at construction time (O(n) once)
- Two-level HashMap: `policyType -> resource -> List<Policy>` for O(1) lookups
- Built when policies are set on a role, not per-request
- Stored on the role object itself, not in a separate cache

### How Jmix Evaluates Entity Permissions (HIGH confidence - source code verified)

The `SecureOperationsImpl` evaluates entity CRUD by:
1. Getting policies from `PolicyStore.getEntityResourcePolicies(metaClass)` - which delegates to the indexed structure
2. Checking if any policy has ALLOW effect for the requested action
3. Falling back to wildcard `"*"` entity policies if no direct match

The `AuthenticationPolicyStore` iterates the user's `GrantedAuthority` entries, resolves each to a `ResourceRole` from the `ResourceRoleRepository`, and delegates to `ResourcePoliciesIndex.getPoliciesByTypeAndResource()`.

### Implications for This Project

The current `PermissionMatrix` already uses `Set<String>` for O(1) lookups, which is structurally similar to Jmix's indexed approach but flatter. The main optimization opportunities are:

1. **Eliminate `String.split(".")` in `isAttributePermitted()`** - Pre-extract entity names from attribute targets at construction time
2. **Pre-build wildcard key sets** - Currently wildcards are checked via separate `contains()` calls; pre-building a secondary index of entity-to-wildcard mappings would reduce redundant lookups
3. **Consider entity-grouped attribute sets** - Like Jmix's two-level map, group attribute permissions by entity name to enable bulk attribute checks without per-attribute key construction

## Sources

- JHipster CI/CD documentation: https://www.jhipster.tech/setting-up-ci/ (MEDIUM confidence - docs describe generator approach, actual workflow needs customization)
- JHipster Docker Compose documentation: https://www.jhipster.tech/docker-compose/ (MEDIUM confidence - standard JHipster patterns, confirmed Jib + Docker Compose flow)
- Jmix authorization documentation: https://docs.jmix.io/jmix/security/authorization.html (HIGH confidence - official docs)
- Jmix `ResourceRole.ResourcePoliciesIndex` source: `jmix-framework/jmix` GitHub repo, `jmix-security/security/src/main/java/io/jmix/security/model/ResourceRole.java` (HIGH confidence - read actual source code)
- Jmix `SecureOperationsImpl` source: `jmix-framework/jmix` GitHub repo, `jmix-security/security/src/main/java/io/jmix/security/impl/constraint/SecureOperationsImpl.java` (HIGH confidence - read actual source code)
- Jmix `AuthenticationPolicyStore` source: `jmix-framework/jmix` GitHub repo, `jmix-security/security/src/main/java/io/jmix/security/impl/constraint/AuthenticationPolicyStore.java` (HIGH confidence - read actual source code)
- Jmix `CrudEntityConstraint` source: `jmix-framework/jmix` GitHub repo (HIGH confidence)
- Project `PermissionMatrix.java`, `RequestPermissionSnapshot.java`, `SecureDataManagerImpl.java`, `MetamodelSecuredEntityCatalog.java` - direct code review (HIGH confidence)
- Project `build.gradle`, `jhipster.docker-conventions.gradle`, `application-prod.yml`, Docker infrastructure files - direct code review (HIGH confidence)
