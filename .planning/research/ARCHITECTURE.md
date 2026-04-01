# Architecture Patterns

**Domain:** CI/CD, Production Deployment, Permission Optimization, Security Validation
**Researched:** 2026-04-01

## Existing Architecture Baseline

Before defining new components, here is what already exists and must be preserved:

### Security Pipeline (Hot Path)

```
HTTP Request
  -> JWT Filter (Spring Security, authorities in token)
  -> Resource (REST endpoint)
  -> Service (transaction boundary)
  -> SecureDataManagerImpl
       -> SecuredEntityCatalog.findByCode/findByEntityClass (catalog lookup)
       -> DataManagerImpl.checkCrud / loadOne / loadPage
            -> AccessManagerImpl.applyRegisteredConstraints (D-11: sorted once at construction)
                 -> CrudEntityConstraint
                      -> RolePermissionServiceDbImpl.isEntityOpPermitted
                           -> RequestPermissionSnapshot.getMatrix() (request-scoped)
                                -> Hazelcast IMap<String, PermissionMatrix>.computeIfAbsent (cross-request, D-01/D-04)
                                     -> PermissionMatrix.isEntityPermitted (HashSet.contains, O(1))
       -> SecureEntitySerializerImpl.serialize (per-property fetch plan walk)
            -> AttributePermissionEvaluatorImpl.canView (per attribute)
                 -> PermissionMatrix.isAttributePermitted (HashSet.contains, O(1))
       -> FetchPlanResolver.resolve (D-08: single resolution per response)
  -> Repository (JPA query)
```

### Key Existing Components

| Component | Location | State |
|-----------|----------|-------|
| `SecureDataManagerImpl` | `security/data/` | Stable, 6 optimizations shipped in Phase 11 |
| `RequestPermissionSnapshot` | `security/permission/` | Request-scoped bean, Hazelcast-backed |
| `PermissionMatrix` | `security/permission/` | Immutable `Set<String>` of ALLOW keys |
| `AccessManagerImpl` | `security/access/` | D-11: constructor-time constraint sorting |
| `SecPermissionService` | `service/security/` | Owns cache eviction on writes (D-02/D-03) |
| Jib Docker config | `buildSrc/.../docker-conventions.gradle` | `eclipse-temurin:25-jre-noble`, ports 8080+5701 |
| Docker Compose (app) | `src/main/docker/app.yml` | App + PostgreSQL + Mailpit, health checks present |
| k6 load tests | `load-tests/scripts/` | Auth helper, org-list, org-detail benchmarks |
| Gradle build | `build.gradle` + `gradle/*.gradle` | Testcontainers PostgreSQL, `-Pprod` profile flag |

### Build System

- **Backend:** Gradle 9.4.0, `./gradlew build -Pprod` triggers prod profile, Jib for Docker images (`jhipster-sec:latest`)
- **Frontend:** `npm run build` in `frontend/`, Angular 21 with `@angular/build:application` builder
- **Tests:** Unit tests excluded from `*IT*`/`*IntTest*` patterns; integration tests via `integrationTest` task; Testcontainers PostgreSQL for DB tests
- **Docker:** Jib plugin builds to local Docker daemon; `app.yml` compose file orchestrates app+postgres+mailpit

---

## Recommended Architecture

### 1. GitHub Actions CI/CD

#### Workflow Structure

Use two workflows, not one monolith:

**`ci.yml`** -- runs on every push/PR to main:

```
Job: backend
  - setup-java@v4 (temurin 25)
  - setup-gradle@v4 (automatic caching)
  - ./gradlew check -Pprod (unit + integration tests with Testcontainers)
  - ./gradlew jacocoTestReport (coverage)
  - Upload test reports as artifacts

Job: frontend (parallel with backend)
  - setup-node@v4 (node 24, npm cache)
  - npm ci --prefix frontend
  - npm run build --prefix frontend (production build)
  - npm test --prefix frontend (Vitest)

Job: docker (depends on backend + frontend)
  - setup-java@v4 (temurin 25)
  - setup-gradle@v4
  - ./gradlew jibDockerBuild -Pprod (builds local image)
  - docker compose -f src/main/docker/app.yml up -d
  - Wait for health check
  - Smoke test: curl /management/health
  - docker compose down
```

**`deploy.yml`** -- manual trigger or tag-based:

```
Job: build-and-push
  - Build Jib image with registry target
  - Push frontend build to hosting/registry
```

#### Caching Strategy

| Cache Target | Mechanism | Why |
|-------------|-----------|-----|
| Gradle dependencies + build cache | `gradle/actions/setup-gradle@v4` (automatic) | Handles `~/.gradle/caches`, `~/.gradle/wrapper`, build cache out of the box. No manual `actions/cache` needed. |
| npm dependencies | `actions/setup-node@v4` with `cache: 'npm'` and `cache-dependency-path: 'frontend/package-lock.json'` | Caches `~/.npm` based on lockfile hash. |
| Testcontainers images | Not cached in CI | Testcontainers pulls are fast enough; Docker layer caching adds complexity without significant benefit for single-DB setups. |
| Gradle daemon | Disabled in CI (`--no-daemon` or `GRADLE_OPTS=-Dorg.gradle.daemon=false`) | Daemon provides no benefit in ephemeral CI runners; wastes memory. |

**Critical detail:** The `setup-gradle@v4` action from `gradle/actions` automatically caches both the dependency cache and the build cache. Do NOT add a separate `actions/cache` step for Gradle -- it will conflict.

#### Integration with Existing Build

The project already separates unit and integration tests via `spring-boot.gradle`:
- Unit tests: `test` task (excludes `*IT*`, `*IntTest*`)
- Integration tests: `integrationTest` task (includes `*IT*`, `*IntTest*`)
- `check.dependsOn(integrationTest)` -- so `./gradlew check` runs both

This means CI just needs `./gradlew check -Pprod` for full backend verification. No custom test splitting needed.

### 2. Docker Compose Production Environment

#### Container Architecture

```
                    [nginx:alpine]
                     port 80/443
                    /           \
              /api/*           /*
                |               |
        [jhipster-sec]    [frontend dist]
         port 8080        (served by nginx)
              |
        [postgresql:18]
         port 5432
              |
        [mailpit] (dev/staging only)
         port 1025
```

#### Why Nginx, Not a Separate Frontend Container

The Angular SPA is static files. Serve them from Nginx which also reverse-proxies `/api/*` to the backend. This is the standard JHipster production pattern and avoids an unnecessary container.

#### Compose File Design

Extend the existing `src/main/docker/app.yml` pattern. Create a new `docker-compose.yml` at the project root (or `src/main/docker/prod.yml`) that:

1. **Keeps existing service definitions** -- `postgresql` extends `postgresql.yml`, existing health checks preserved
2. **Adds Nginx** -- serves Angular production build, proxies API requests
3. **Parameterizes credentials** -- environment variables or `.env` file, not hardcoded passwords (the current `application-prod.yml` has hardcoded credentials that must be fixed)

#### Spring Profile Configuration

The existing `app.yml` already uses `SPRING_PROFILES_ACTIVE=prod,api-docs`. For the production compose:

- Use `prod` profile (no `api-docs` in production)
- Override datasource via environment variables (already supported via Spring externalized config)
- The `entrypoint.sh` already supports `_FILE` suffix for Docker secrets

#### Health Checks

Already present in `app.yml`:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/management/health"]
  interval: 5s
  timeout: 5s
  retries: 40
```

The backend already exposes liveness and readiness probes:
- Liveness: `/management/health/liveness` (includes `livenessState`)
- Readiness: `/management/health/readiness` (includes `readinessState,db`)

For Nginx, add a simple health check on the static content endpoint.

#### Frontend Build Integration

The frontend must be built before the Docker Compose deployment:

```bash
# Build frontend
cd frontend && npm ci && npm run build

# Copy dist to nginx volume or build a multi-stage Dockerfile
# The Angular output goes to frontend/dist/frontend/browser/
```

Two options for serving:
1. **Volume mount** (simpler): Mount `frontend/dist/frontend/browser/` into Nginx
2. **Multi-stage Dockerfile** (self-contained): Build Angular in a Node stage, copy to Nginx stage

Recommendation: Multi-stage Dockerfile for the frontend/nginx container because it produces a self-contained, reproducible image.

### 3. Permission Matrix Optimization

This is the most architecturally significant change. The current `PermissionMatrix` already uses `HashSet<String>.contains()` which is O(1). The optimization targets are elsewhere in the pipeline.

#### Current Performance Profile

After Phase 11 optimizations, the security pipeline overhead is +0.2% (list) and -0.8% (detail). The PERF-04 target is met. PERF-05 through PERF-07 target further optimization for scale at ~200 secured entities.

#### Where Optimization Should Happen

**Layer 1: PermissionMatrix Construction (cache miss path)**

Current `PermissionMatrix` constructor:
```java
public PermissionMatrix(List<SecPermission> permissions) {
    Set<String> allowed = new HashSet<>();
    for (SecPermission p : permissions) {
        if ("ALLOW".equals(p.getEffect())) {
            String key = p.getTargetType().name() + ":" + p.getTarget() + ":" + p.getAction();
            allowed.add(key);
        }
    }
    this.allowedKeys = Set.copyOf(allowed);
}
```

This is already O(n) and only runs on Hazelcast cache miss. Not a meaningful optimization target.

**Layer 2: PermissionMatrix Lookup (hot path, per-request)**

Current lookups are already O(1) HashSet contains. The `isEntityPermitted` checks 2 keys (direct + wildcard), `isAttributePermitted` checks up to 4 keys (direct + wildcard + edit-implies-view variants). This is already optimal.

**Layer 3: Serializer Attribute Iteration (per-entity, per-property)**

`SecureEntitySerializerImpl.serialize()` iterates fetch plan properties and calls `attributePermissionEvaluator.canView()` per property. Each call goes through:
1. `RequestPermissionSnapshot.isRequestScopeActive()` -- static check
2. `requestPermissionSnapshot.getMatrix()` -- returns cached instance (already loaded)
3. `PermissionMatrix.isAttributePermitted()` -- O(1) HashSet lookup

This is already efficient per-call, but at 200 entities x 10 properties = 2000 calls per list response. The overhead is in the method call chain, not the lookup itself.

**Optimization Opportunity: Pre-compute Attribute Visibility per Entity Type**

Instead of checking permissions per-property per-entity-instance, pre-compute a "visible property set" once per entity type per request:

```
New Component: FetchPlanPermissionFilter
  Input: FetchPlan + PermissionMatrix + entityClass
  Output: FilteredFetchPlan (only properties the user can see)
  Cache: Per request-scoped snapshot (same matrix = same filtered plan)
```

This eliminates N attribute permission checks per entity instance and replaces them with a single pre-filtered fetch plan. The serializer then iterates only visible properties with no per-property permission check.

**Where it fits in the pipeline:**

```
SecureDataManagerImpl.serialize()
  -> FetchPlanPermissionFilter.filter(fetchPlan, matrix, entityClass)  // NEW: once per response
  -> SecureEntitySerializerImpl.serialize(entity, filteredFetchPlan)    // MODIFIED: no permission checks
```

This is the single highest-impact optimization for the serialization path at scale.

**Layer 4: Full Request Path Optimization (PERF-06)**

The full secured request path from entry to response:

1. JWT filter (Spring Security, no DB) -- already optimized (D-05/D-06)
2. Catalog lookup -- `Map.get()` in `MetamodelSecuredEntityCatalog`, O(1)
3. CRUD check -- matrix lookup, O(1) via D-01
4. JPA query -- DB-bound, not a security optimization target
5. Serialization -- per-entity property walk (target of Layer 3 above)
6. FetchPlan resolution -- single resolution per response (D-08)

The remaining optimization surface is almost entirely in Layer 3 (serialization). After the FetchPlanPermissionFilter optimization, the security pipeline becomes:
- 1 Hazelcast cache check (per request)
- 1 CRUD permission check (per entity type per request)
- 1 fetch plan filter (per entity type per response)
- 0 per-property permission checks during serialization

**Impact on RequestPermissionSnapshot:**
- No structural changes. The snapshot already caches the `PermissionMatrix` for the request duration.
- The new `FetchPlanPermissionFilter` consumes the matrix from the snapshot but does not modify it.

**Impact on Hazelcast Cache:**
- No structural changes. The `PermissionMatrix` cache key (sorted authority set) and eviction strategy (write-path in `SecPermissionService`) remain unchanged.
- The `FetchPlanPermissionFilter` output could optionally be cached in the snapshot as a `Map<String, FilteredFetchPlan>` keyed by `entityClass + fetchPlanCode`, but this is a minor optimization since filtering is cheap (iterate properties, check HashSet membership).

#### Component Boundaries for Optimization

| Component | Change Type | What Changes |
|-----------|------------|--------------|
| `FetchPlanPermissionFilter` | **NEW** | Pre-filters fetch plan properties against PermissionMatrix |
| `SecureDataManagerImpl.serialize()` | **MODIFIED** | Calls filter before serializer |
| `SecureEntitySerializerImpl` | **MODIFIED** | Accepts pre-filtered fetch plan, removes per-property permission checks |
| `AttributePermissionEvaluatorImpl` | **UNCHANGED** | Still available for non-serialization paths (e.g., merge write checks) |
| `RequestPermissionSnapshot` | **UNCHANGED** | Matrix caching unchanged |
| `PermissionMatrix` | **UNCHANGED** | Lookup methods unchanged |

### 4. Security Validation Tests

#### Test Architecture

Security validation under production conditions requires integration tests that run against the Docker Compose environment, not just Testcontainers unit tests.

**Three test levels:**

| Level | Tool | What It Validates | When |
|-------|------|-------------------|------|
| Unit | JUnit + Mockito | Component logic (existing `SecureDataManagerImplTest`) | Every build |
| Integration | Spring Boot Test + Testcontainers | Full pipeline with real DB (existing `*IT` tests) | Every build |
| Production validation | k6 + Docker Compose | Auth refresh, permission enforcement, load behavior | CI docker job, manual validation |

#### Test Patterns

**Auth Refresh Validation (k6):**
```
1. Authenticate as admin -> get JWT
2. Hit secured endpoint -> verify 200
3. (Backend: revoke a permission via API)
4. Hit secured endpoint with same JWT -> verify still 200 (JWT not expired, D-05/D-06)
5. Authenticate again -> get new JWT
6. Hit secured endpoint -> verify 403 (new JWT reflects revoked permission)
```

This validates the JWT direct-trust model (D-05/D-06): permission changes take effect on next JWT issuance, not retroactively.

**Permission Enforcement Validation (k6):**
```
1. Authenticate as user with known role
2. Hit secured endpoint for allowed entity -> verify 200
3. Hit secured endpoint for denied entity -> verify 403
4. Modify permissions via admin API
5. Re-authenticate
6. Verify new permission state is enforced
```

**Secured-Entity Flow Under Load (k6):**
Extend existing `org-list-benchmark.js` and `org-detail-benchmark.js`:
```
1. Multiple VUs with different roles (admin, limited-user, no-permission-user)
2. Verify each VU gets correct allow/deny responses under concurrent load
3. Verify no permission leakage between concurrent requests (different RequestPermissionSnapshot instances)
4. Assert p95 overhead remains under 10% target
```

**Integration Test for Cache Eviction (JUnit):**
```java
// Verify that SecPermissionService write evicts Hazelcast cache
1. Load PermissionMatrix (populates cache)
2. Create new SecPermission via SecPermissionService.save()
3. Verify Hazelcast map is cleared
4. Load PermissionMatrix again -> includes new permission
```

#### New vs Modified Test Components

| Component | Type | What |
|-----------|------|------|
| `load-tests/scripts/auth-refresh-validation.js` | **NEW** | k6 script for auth refresh correctness |
| `load-tests/scripts/permission-enforcement.js` | **NEW** | k6 script for multi-role permission validation |
| `load-tests/scripts/concurrent-security.js` | **NEW** | k6 script for concurrent-request isolation proof |
| Existing `org-list-benchmark.js` | **MODIFIED** | Add multi-role VU scenarios |
| Existing `org-detail-benchmark.js` | **MODIFIED** | Add multi-role VU scenarios |
| `PermissionCacheEvictionIT.java` | **NEW** | Integration test for Hazelcast cache eviction correctness |

---

## Data Flow

### CI/CD Build Flow

```
Push to main
  -> GitHub Actions
       -> [backend job]  ./gradlew check -Pprod (unit + integration)
       -> [frontend job] npm ci && npm run build && npm test
       -> [docker job]   (depends on backend + frontend)
            -> ./gradlew jibDockerBuild -Pprod
            -> Build frontend nginx image
            -> docker compose up
            -> Health check + smoke test
            -> docker compose down
```

### Production Compose Request Flow

```
Browser -> Nginx:80
  -> /api/* -> proxy_pass http://app:8080
  -> /* -> serve /usr/share/nginx/html (Angular SPA)

app:8080 -> postgresql:5432 (JDBC)
app:8080 -> hazelcast (embedded, single node for now)
```

### Optimized Security Pipeline (After PERF-05)

```
SecureDataManagerImpl.loadList(entityCode, fetchPlanCode, pageable)
  1. resolveEntry(entityCode)                           // catalog Map lookup, O(1)
  2. loadListInternal(entityClass, pageable)
       -> DataManagerImpl.loadPage(entityClass, null, pageable, EntityOp.READ)
            -> checkCrud: PermissionMatrix.isEntityPermitted()    // O(1)
            -> unconstrainedDataManager.loadPage()                // DB query
  3. .map(entity -> serialize(entity, fetchPlanCode))
       -> fetchPlanResolver.resolve(entityClass, fetchPlanCode)   // D-08: once per response
       -> fetchPlanPermissionFilter.filter(fetchPlan, matrix)     // NEW: once per response
       -> secureEntitySerializer.serialize(entity, filteredPlan)  // No per-property checks
```

---

## Patterns to Follow

### Pattern 1: Parallel CI Jobs with Dependency Gates

**What:** Run backend and frontend builds in parallel; gate Docker build on both succeeding.
**When:** Every CI pipeline.
**Why:** Backend and frontend have zero build-time dependencies. Parallel execution cuts CI time nearly in half.

### Pattern 2: Pre-computed Permission Filtering

**What:** Filter fetch plan properties against the PermissionMatrix once per entity type per response, not once per property per entity instance.
**When:** Any serialization of secured entities.
**Why:** Eliminates O(entities x properties) permission checks, replacing them with O(properties) per entity type.

### Pattern 3: Docker Compose Extending Existing Service Definitions

**What:** New production compose file extends existing `postgresql.yml`, `mailpit.yml` definitions using `extends:`.
**When:** Creating the production Docker Compose environment.
**Why:** The project already defines PostgreSQL and Mailpit as reusable service fragments. Extending them avoids duplication and keeps service definitions in sync.

### Pattern 4: k6 Multi-Role Load Testing

**What:** Run concurrent VUs with different JWT tokens (different roles) to validate permission isolation under load.
**When:** Security regression validation.
**Why:** Single-role load tests do not catch permission leakage between concurrent requests. Multi-role tests prove that `RequestPermissionSnapshot` (request-scoped) correctly isolates each request's permission context.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Manual Gradle Cache Configuration in CI

**What:** Adding `actions/cache` steps for `~/.gradle/caches` alongside `setup-gradle`.
**Why bad:** `gradle/actions/setup-gradle@v4` manages its own cache. Manual cache steps conflict, causing cache corruption or missed hits.
**Instead:** Use `setup-gradle@v4` alone; it handles all Gradle caching automatically.

### Anti-Pattern 2: Separate Frontend Container

**What:** Running a Node.js server (e.g., `ng serve` or `http-server`) as a Docker container for the Angular SPA.
**Why bad:** The Angular SPA is static files after build. A runtime Node.js server adds attack surface, memory overhead, and a component to maintain. Nginx serves static files faster and handles reverse proxy in the same container.
**Instead:** Multi-stage Dockerfile: Node build stage -> Nginx serve stage.

### Anti-Pattern 3: Per-Entity Permission Evaluation in Serialization

**What:** The current `SecureEntitySerializerImpl` checks `attributePermissionEvaluator.canView()` per property per entity instance.
**Why bad:** At 200 entities x 10 properties, this is 2000 method invocations through the full evaluator chain per list response. The permission answer is identical for every entity of the same type within a single request.
**Instead:** Pre-filter the fetch plan once per entity type per request using `FetchPlanPermissionFilter`.

### Anti-Pattern 4: Testing Security Only in Unit Tests

**What:** Relying solely on mocked unit tests (`SecureDataManagerImplTest`) for security validation.
**Why bad:** Mocks verify component interaction logic but not actual permission enforcement end-to-end. A mock could pass while the real pipeline has a configuration gap.
**Instead:** Layer security tests: unit (mocked logic) + integration (Testcontainers, real DB) + production validation (k6, Docker Compose, real JWT flow).

---

## Scalability Considerations

| Concern | Current (50 entities) | At 200 entities | At 1000+ entities |
|---------|----------------------|-----------------|-------------------|
| PermissionMatrix size | ~100 ALLOW keys in HashSet | ~500 keys, still O(1) lookup | ~2500 keys, still O(1) lookup, ~100KB serialized in Hazelcast |
| Hazelcast cache entries | One entry per unique authority-set | Same; authority-set diversity is bounded by role count, not entity count | Same |
| Serialization overhead | ~50 attribute checks per list | ~2000 without optimization; ~1 with FetchPlanPermissionFilter | ~10000 without optimization; ~1 with FetchPlanPermissionFilter |
| Catalog lookup | HashMap, O(1) | Same | Same |
| CI build time | ~5 min backend + ~2 min frontend | Same (code size, not entity count) | Same |

The permission optimization (FetchPlanPermissionFilter) is the only change needed to scale from 50 to 200+ entities. All other components (PermissionMatrix, Hazelcast cache, catalog) are already O(1) or bounded by role count.

---

## Component Dependency Graph (Build Order)

```
Phase 1: CI/CD Pipeline (no code dependencies on other phases)
  -> GitHub Actions workflows
  -> Caching configuration
  -> Smoke test scripts

Phase 2: Docker Compose Production (depends on CI for image build)
  -> Frontend Nginx Dockerfile
  -> Production compose file
  -> Nginx config (reverse proxy + SPA fallback)
  -> Credential externalization (fix hardcoded passwords in application-prod.yml)

Phase 3: Permission Matrix Optimization (independent of CI/CD)
  -> FetchPlanPermissionFilter (new component)
  -> SecureDataManagerImpl.serialize() modification
  -> SecureEntitySerializerImpl simplification
  -> Benchmark at 200 entities (k6)

Phase 4: Security Validation (depends on Docker Compose for production-like testing)
  -> k6 multi-role scripts (auth refresh, permission enforcement, concurrent isolation)
  -> Integration test for cache eviction
  -> Run validation suite against Docker Compose environment
```

**Why this order:**
1. CI/CD first because it provides automated verification for all subsequent changes
2. Docker Compose second because security validation needs a production-like environment
3. Permission optimization third because it is self-contained backend work that can be benchmarked independently
4. Security validation last because it is the proof step that validates everything else works together

Phases 1+3 can run in parallel if staffing allows (zero code dependencies between them). Phase 4 depends on Phase 2.

---

## Sources

- Existing codebase: `SecureDataManagerImpl.java`, `RequestPermissionSnapshot.java`, `PermissionMatrix.java`, `RolePermissionServiceDbImpl.java`, `AttributePermissionEvaluatorImpl.java`, `SecureEntitySerializerImpl.java`
- Existing Docker config: `src/main/docker/app.yml`, `postgresql.yml`, `jib/entrypoint.sh`, `buildSrc/.../docker-conventions.gradle`
- Existing build: `build.gradle`, `gradle/spring-boot.gradle`, `gradle/profile_prod.gradle`
- Existing load tests: `load-tests/scripts/auth.js`, `org-list-benchmark.js`, `org-detail-benchmark.js`
- Gradle Actions: `gradle/actions/setup-gradle` (v4) -- automatic Gradle caching for CI
- JHipster Docker patterns: `src/main/docker/app.yml` already follows JHipster's compose conventions
- Spring Boot Actuator health probes: already configured in `application.yml` (liveness + readiness groups)
