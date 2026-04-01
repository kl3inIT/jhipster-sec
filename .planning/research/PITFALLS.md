# Domain Pitfalls

**Domain:** CI/CD, Docker Production Deployment, Permission Matrix Optimization, and Security Validation for JHipster Security Platform
**Researched:** 2026-04-01

## Critical Pitfalls

Mistakes that cause rewrites, security regressions, or production outages.

---

### Pitfall 1: PermissionMatrix is Not Serializable but Lives in Hazelcast IMap

**What goes wrong:** `PermissionMatrix` is stored in a Hazelcast `IMap` via `cache.computeIfAbsent(cacheKey, k -> buildMatrix(authorities))` in `RequestPermissionSnapshot`. However, `PermissionMatrix` does not implement `java.io.Serializable`. In dev with a single embedded Hazelcast node and no backup copies, this works because the object stays in local heap. In production with Hazelcast clustering (backup-count: 1 in `application-prod.yml`), Hazelcast must serialize the `PermissionMatrix` to replicate it to backup nodes, which will throw `HazelcastSerializationException` at runtime.

**Why it happens:** Dev testing uses a single Hazelcast instance with no cluster peers. The `computeIfAbsent` path stores the object locally and never serializes it. Production with `backup-count: 1` (or any multi-node deployment) triggers serialization that was never tested.

**Consequences:** Every secured request fails with a serialization exception. The permission cache becomes unusable. Since the security pipeline is fail-closed, all secured endpoints return 500 or 403 under production clustering.

**Prevention:**
1. Make `PermissionMatrix` implement `Serializable` (the `allowedKeys` field is already an immutable `Set<String>` which is serializable).
2. Alternatively, implement Hazelcast `IdentifiedDataSerializable` or `Portable` for better performance.
3. Add an integration test that exercises Hazelcast serialization round-trip: serialize a `PermissionMatrix` to bytes and deserialize it back, asserting equality.
4. Test with `backup-count: 1` in CI to catch this class of bug before production.

**Detection:** First secured API request in a multi-node Hazelcast cluster will fail. Look for `HazelcastSerializationException` in logs.

**Phase:** Must be addressed in the permission optimization phase, before Docker production deployment.

**Confidence:** HIGH -- verified by code inspection: `PermissionMatrix.java` has no `implements Serializable`, `CacheConfiguration` sets `backup-count` from config, and `application-prod.yml` sets `backup-count: 1`.

---

### Pitfall 2: Hardcoded Production Database Credentials in application-prod.yml

**What goes wrong:** `application-prod.yml` contains `url: jdbc:postgresql://157.230.42.136:5555/jhipster-sec`, `username: postgres`, `password: admin123` in plaintext. If this file is committed to the repository (it is), these credentials are exposed to anyone with repo access. In a CI/CD pipeline, this file ships into Docker images and is trivially extractable.

**Why it happens:** During initial development, hardcoded values are convenient. They are never replaced with environment variable references before committing.

**Consequences:** Database credentials leak. If the IP is publicly accessible, the database is compromised. The Docker image bakes these credentials into its layers permanently (even if later removed from source, old image layers retain them).

**Prevention:**
1. Replace hardcoded values with environment variable references: `url: ${SPRING_DATASOURCE_URL}`, `username: ${SPRING_DATASOURCE_USERNAME}`, `password: ${SPRING_DATASOURCE_PASSWORD}`.
2. The Jib entrypoint already supports `_FILE` suffixed env vars for Docker secrets -- use this mechanism.
3. Use GitHub Actions secrets for CI/CD, Docker Compose `env_file` or Docker secrets for production.
4. Rotate the exposed credentials immediately.
5. Consider adding `application-prod.yml` credential checks to CI as a linting step.

**Detection:** Grep for hardcoded passwords, IP addresses, or non-localhost hostnames in `application-*.yml` files.

**Phase:** Must be addressed as the very first task in the CI/CD phase, before any Docker image is built or pushed.

**Confidence:** HIGH -- verified by reading `application-prod.yml` lines 36-38.

---

### Pitfall 3: Cache Eviction Breaks When Optimizing PermissionMatrix Internal Data Structures

**What goes wrong:** The current `SecPermissionService` uses `@CacheEvict(cacheNames = PERMISSION_MATRIX_CACHE, allEntries = true)` on every write operation. This works because Spring's `@CacheEvict` delegates to `HazelcastCacheManager` which calls `IMap.clear()`. If during optimization the caching strategy changes (e.g., moving to finer-grained eviction, switching from Spring Cache abstraction to direct Hazelcast API, or adding a second-level cache), the eviction path silently breaks while reads continue to serve stale permission data.

**Why it happens:** The eviction contract is implicit -- it depends on `@CacheEvict` correctly mapping to the Hazelcast map name. Optimizations that touch caching layers (like adding a local near-cache, introducing a pre-processed map structure alongside the existing one, or refactoring the cache key) can accidentally create a second cache that is not covered by the eviction annotation.

**Consequences:** Permission changes stop taking effect. Users granted or revoked access see no change until the 3600s TTL expires. This is a silent security regression: access that should be denied remains allowed.

**Prevention:**
1. Write a dedicated integration test: save a new `SecPermission` via `SecPermissionService`, then verify the next `RequestPermissionSnapshot.getMatrix()` call reflects the change.
2. If refactoring cache internals, keep the single eviction seam in `SecPermissionService` and add a test that asserts `IMap.size() == 0` after every write method.
3. Never add a second permission cache (local, near-cache, or otherwise) without extending the eviction path to cover it.
4. Keep the `allEntries = true` strategy unless profiling proves it is a bottleneck -- partial eviction with permission matrices is extremely error-prone.

**Detection:** Permission changes take effect after TTL (3600s) instead of immediately. Automated test: change a permission, immediately check enforcement, assert it reflects the new state.

**Phase:** Permission optimization phase. Every optimization PR must include cache-eviction regression tests.

**Confidence:** HIGH -- verified by code inspection of `SecPermissionService` and `CacheConfiguration`.

---

### Pitfall 4: Testcontainers Requires Docker-in-Docker in GitHub Actions

**What goes wrong:** The project's `@IntegrationTest` annotation uses `@ImportTestcontainers(DatabaseTestcontainer.class)` for PostgreSQL. In GitHub Actions, Testcontainers needs a working Docker daemon. The default `ubuntu-latest` runners have Docker pre-installed, but specific configurations can break this: using container-based jobs (`container:` directive in workflow YAML) creates a Docker-in-Docker situation where Testcontainers cannot find the Docker socket.

**Why it happens:** JHipster integration tests depend on Testcontainers for database lifecycle. The CI configuration must provide a compatible Docker environment, but this is not obvious from the test code alone.

**Consequences:** All integration tests fail in CI with `Could not find a valid Docker environment` or socket connection errors. The CI pipeline appears completely broken on first setup.

**Prevention:**
1. Use GitHub Actions runner directly (not inside a `container:` block) for integration test jobs.
2. If container jobs are required, mount the Docker socket: `options: --privileged -v /var/run/docker.sock:/var/run/docker.sock`.
3. Set `TESTCONTAINERS_RYUK_DISABLED=true` if using GitHub Actions service containers as an alternative to Testcontainers-managed containers.
4. Alternatively, use GitHub Actions `services:` block for PostgreSQL instead of Testcontainers, but this requires test configuration changes and loses parity with local dev.
5. Add a CI smoke step that runs `docker info` to verify Docker availability before test execution.

**Detection:** Integration tests fail with Docker socket errors on first CI run.

**Phase:** CI/CD pipeline setup phase.

**Confidence:** HIGH -- Testcontainers Docker requirement is well-documented; the project provably uses `@ImportTestcontainers`.

---

### Pitfall 5: JVM Container Memory Limits Mismatch with Docker Compose Settings

**What goes wrong:** The existing `app.yml` sets `_JAVA_OPTIONS=-Xmx512m -Xms256m`, but the Docker Compose file has no container-level `mem_limit` or `deploy.resources.limits.memory`. The JVM respects the `-Xmx` flag but total JVM memory consumption includes metaspace, thread stacks, native memory, Hazelcast off-heap, and direct buffers -- typically 1.5-2x the heap. Without a container memory limit, the container can consume unbounded host memory. With a too-tight container limit (e.g., setting `mem_limit: 512m` to match `-Xmx512m`), the OOM killer terminates the JVM without a clean shutdown.

**Why it happens:** The `-Xmx` flag only controls heap. Hazelcast uses additional memory for its map structures and networking. Spring Boot's embedded Tomcat allocates thread stacks. Liquibase startup can spike memory temporarily.

**Consequences:** In production: either unbounded memory growth (no container limit) or random OOM kills (container limit too close to Xmx). OOM kills during Liquibase migration can leave the database in a partially-migrated state.

**Prevention:**
1. Set container memory limit to at least 2x Xmx: if `Xmx=512m`, set `mem_limit: 1024m`.
2. Use `-XX:MaxRAMPercentage=50` instead of fixed `-Xmx` so the JVM automatically sizes heap to half the container limit.
3. Add `-XX:+UseContainerSupport` (default in modern JVMs but worth being explicit).
4. Set `-XX:MaxMetaspaceSize=256m` to cap metaspace growth.
5. Monitor actual memory consumption under load before finalizing production limits.
6. Add a `deploy.resources.limits.memory` to the Docker Compose production file.

**Detection:** Container restarts with exit code 137 (OOM killed). `docker inspect` shows OOMKilled: true.

**Phase:** Docker production deployment phase.

**Confidence:** HIGH -- standard JVM-in-container issue; verified that current `app.yml` has no container memory limit.

---

### Pitfall 6: Frontend Serving Gap -- Angular SPA Not in Docker Compose Stack

**What goes wrong:** The project has a standalone Angular frontend under `frontend/` that is built and deployed separately. The existing `app.yml` Docker Compose only defines the Java backend, PostgreSQL, and Mailpit. There is no nginx or other container serving the frontend static assets. In production, the frontend needs to be served with proper SPA routing (all non-API paths return `index.html`), CORS headers, and proxy configuration to the backend API.

**Why it happens:** JHipster's default approach bundles frontend assets into the Spring Boot JAR. This project explicitly uses a standalone frontend, so the JHipster-generated Docker Compose does not account for it.

**Consequences:** Production deployment has no frontend. Or, if someone tries to serve from the backend JAR, the frontend is missing because it was never bundled. If a separate nginx is added without SPA routing, deep links and page refreshes return 404.

**Prevention:**
1. Add an nginx container to Docker Compose that serves the `frontend/dist/` build output with `try_files $uri $uri/ /index.html`.
2. Configure nginx to proxy `/api/*`, `/management/*`, and `/v3/api-docs/*` to the backend container.
3. Build the frontend in CI with `npm run build -- --configuration production` and copy the output into the nginx image.
4. Use multi-stage Docker build: Node stage builds, nginx stage serves.
5. Ensure the `ApplicationConfigService.getEndpointFor(...)` in the frontend resolves correctly in the containerized environment (relative paths or environment-injected base URL).

**Detection:** No frontend accessible at the expected URL after `docker-compose up`.

**Phase:** Docker production deployment phase.

**Confidence:** HIGH -- verified that `app.yml` has no frontend container, and CLAUDE.md confirms standalone `frontend/` deployment.

---

## Moderate Pitfalls

---

### Pitfall 7: Gradle Build Cache Poisoning in CI

**What goes wrong:** Gradle's build cache (`~/.gradle/caches`) and the Gradle wrapper (`~/.gradle/wrapper`) are commonly cached in GitHub Actions. If the cache key does not include the `gradle/libs.versions.toml` or `buildSrc` contents, stale dependencies or compiled buildSrc classes persist across CI runs, causing cryptic build failures after dependency updates.

**Prevention:**
1. Use `gradle/actions/setup-gradle@v4` which handles caching correctly, or use `actions/cache` with a key that includes hashes of `gradle/libs.versions.toml`, `buildSrc/**/*.gradle`, and `gradle/wrapper/gradle-wrapper.properties`.
2. Include `buildSrc` hash in the cache key since the project uses `jhipster.docker-conventions` and `jhipster.code-quality-conventions` convention plugins.
3. Separate the npm/node_modules cache from the Gradle cache -- they have different invalidation patterns.
4. Use `--no-daemon` in CI to avoid daemon-related cache corruption.

**Detection:** CI build fails after a dependency version bump with errors that do not reproduce locally.

**Phase:** CI/CD pipeline setup phase.

**Confidence:** MEDIUM -- standard Gradle CI practice; specific cache key composition depends on GitHub Actions version used.

---

### Pitfall 8: Integration Tests and Unit Tests Run Sequentially but Testcontainers Starts Fresh Each Time

**What goes wrong:** The Gradle configuration has `integrationTest.dependsOn(test)` and `check.dependsOn(integrationTest)`. Unit tests run first, then integration tests. Each integration test class starts a Testcontainers PostgreSQL instance. In CI, this can mean 30-60 seconds of container startup per test class, making the CI pipeline very slow.

**Prevention:**
1. Use Testcontainers' singleton container pattern (already likely via `DatabaseTestcontainer` class) to reuse one PostgreSQL container across all integration tests in a single Gradle task.
2. Enable Gradle test parallelism carefully: `maxParallelForks` > 1 requires each fork to have its own Testcontainers instance or shared container with connection pooling.
3. Consider splitting unit tests and integration tests into parallel GitHub Actions jobs to reduce wall-clock time.
4. Use GitHub Actions' `services:` block for PostgreSQL as an alternative for faster CI, with a separate profile that skips Testcontainers.

**Detection:** CI pipeline takes 15+ minutes; integration test phase dominates the time.

**Phase:** CI/CD pipeline setup phase.

**Confidence:** MEDIUM -- depends on how `DatabaseTestcontainer` is implemented (singleton vs. per-class).

---

### Pitfall 9: Permission Optimization Breaks Edit-Implies-View or Wildcard Cascades

**What goes wrong:** `PermissionMatrix` implements two Jmix-style cascades: entity wildcard (`ENTITY:*:action`) and edit-implies-view (`ATTRIBUTE:target:EDIT` implies `VIEW`). When refactoring the internal data structure (e.g., replacing `HashSet<String>` with a `Map<String, Map<String, Set<String>>>` for faster lookup), it is easy to break these cascade paths because they require checking multiple keys per permission query.

**Why it happens:** The current `HashSet<String>` approach makes cascades explicit in the `isEntityPermitted` and `isAttributePermitted` methods. A restructured lookup map may not naturally express these secondary lookup paths, and the developer optimizing for primary-key speed forgets to replicate the cascade logic.

**Consequences:** Silent permission denial. Users with wildcard grants lose access to specific entities. Users with EDIT permission lose implicit VIEW access on attributes. Both are security-model regressions that are hard to detect without comprehensive permission tests.

**Prevention:**
1. Before optimizing, extract the current `PermissionMatrix` behavior into a comprehensive test suite covering: direct match, entity wildcard, attribute wildcard, edit-implies-view direct, edit-implies-view wildcard, and combinations.
2. Run the existing test suite against both old and new implementations (adapter pattern or parallel verification).
3. Add property-based tests: for any permission set, the optimized matrix must return identical results to the original for all possible query inputs.
4. Consider pre-computing cascade expansions at matrix build time rather than query time -- this is both an optimization and a correctness simplification.

**Detection:** Specific permission patterns stop working. Requires tests that cover all cascade paths.

**Phase:** Permission optimization phase.

**Confidence:** HIGH -- verified by reading the cascade logic in `PermissionMatrix.java`.

---

### Pitfall 10: Security Regression Tests Pass in Test but Fail in Production Due to Profile Differences

**What goes wrong:** The test profile uses `spring.profiles.active=test,testprod` for prod-like tests. But the actual production profile has different Hazelcast configuration (backup-count, TTL), different connection pool settings, and potentially different Spring Security filter chain ordering. Security tests that pass under the test profile may fail under the real production profile because of timing differences (Hazelcast replication lag), different cache behavior, or different thread pool sizes.

**Why it happens:** JHipster maintains separate `application-dev.yml`, `application-prod.yml`, and `application-test.yml` profiles. The test profile is intentionally simpler. Production-specific behaviors (clustering, replication, connection limits) are not exercised in tests.

**Consequences:** Security validation tests pass in CI but the application has permission enforcement bugs in production. False confidence in security correctness.

**Prevention:**
1. Run a subset of security regression tests against the actual Docker Compose production stack (not just Spring test context).
2. Create a `docker-compose.test.yml` that mirrors production topology: backend + PostgreSQL + frontend, with the `prod` Spring profile.
3. Use the existing k6 load tests as the security regression suite against the production stack, adding permission-specific scenarios.
4. Test permission enforcement with actual JWT tokens against the production-profile backend, not mocked security contexts.
5. Specifically test: Hazelcast cache eviction (modify a permission, verify next request reflects it), request-scoped snapshot behavior under concurrent requests, and JWT authority trust path.

**Detection:** Security bugs discovered in production that passed in test. Requires production-topology testing.

**Phase:** Security validation phase (after Docker production deployment is available).

**Confidence:** HIGH -- standard environment-drift issue; verified that prod and test profiles diverge significantly.

---

### Pitfall 11: PostgreSQL Container Has No Password and Uses trust Authentication

**What goes wrong:** The `postgresql.yml` Docker Compose file sets `POSTGRES_HOST_AUTH_METHOD=trust` with no password. In production, this means any network-reachable client can connect to PostgreSQL without authentication. Even in Docker Compose where the port is bound to `127.0.0.1`, container-to-container communication is unrestricted within the Docker network.

**Prevention:**
1. Set `POSTGRES_PASSWORD` via Docker secret or environment variable in the production Docker Compose.
2. Remove `POSTGRES_HOST_AUTH_METHOD=trust` in the production configuration.
3. Use `scram-sha-256` authentication method.
4. Match the password in `SPRING_DATASOURCE_PASSWORD` on the app container.
5. Keep the `trust` method only in the dev Docker Compose, not in any production-like configuration.

**Detection:** Security audit of Docker Compose files.

**Phase:** Docker production deployment phase.

**Confidence:** HIGH -- verified by reading `postgresql.yml`.

---

### Pitfall 12: Entrypoint Script Uses Deprecated -noverify JVM Flag

**What goes wrong:** The Jib entrypoint (`src/main/docker/jib/entrypoint.sh`) passes `-noverify` to the JVM. This flag disables bytecode verification, which is a security risk in production and has been deprecated since Java 13. On Java 25 with stricter enforcement, it may emit warnings or be ignored entirely, but it signals poor production hardening.

**Prevention:**
1. Remove `-noverify` from the entrypoint script.
2. Replace with appropriate JVM tuning flags for containers: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=50`.
3. Review all `JAVA_OPTS` and `_JAVA_OPTIONS` for deprecated flags.

**Detection:** JVM startup warnings about deprecated flags; security audit.

**Phase:** Docker production deployment phase.

**Confidence:** HIGH -- verified by reading `entrypoint.sh` line 40.

---

### Pitfall 13: k6 Load Tests Only Run at Dev Scale -- Production Benchmarks Will Give Different Results

**What goes wrong:** The existing k6 tests (`org-list-benchmark.js`) run against `localhost:8080` with 1/10/50 VUs for 30s each. Phase 11 proved PERF-04 at this scale. Under production conditions (Docker containers with memory limits, PostgreSQL with network latency instead of localhost, Hazelcast with replication overhead), the same benchmarks will show different numbers. If the team assumes Phase 11 results carry over to production, they may miss production-specific bottlenecks.

**Prevention:**
1. Re-run k6 benchmarks against the Docker Compose production stack, not localhost dev.
2. Add a k6 scenario that tests permission cache warm-up cost (first request after cache eviction).
3. Test with realistic permission matrix sizes (~200 secured entities generating ~2000+ permission entries).
4. Add connection pool exhaustion scenarios: secured endpoints hold transactions longer due to permission queries.
5. Document the expected performance delta between dev and production-topology benchmarks.

**Detection:** Production p95 latencies significantly exceed Phase 11 benchmarks.

**Phase:** Security validation phase (after Docker production deployment is available).

**Confidence:** MEDIUM -- standard dev-vs-production performance gap; specific delta depends on Docker resource allocation.

---

## Minor Pitfalls

---

### Pitfall 14: GitHub Actions npm Cache Key Does Not Account for frontend/ Subdirectory

**What goes wrong:** The Angular frontend lives in `frontend/`, not the repository root. Standard `actions/cache` or `actions/setup-node` cache configurations assume `package-lock.json` is at the repo root. If the cache key uses `hashFiles('package-lock.json')`, it caches the root `package-lock.json` (which is for k6/backend tooling) instead of `frontend/package-lock.json`.

**Prevention:**
1. Use `hashFiles('frontend/package-lock.json')` for the frontend npm cache key.
2. Use separate cache steps for root-level and frontend-level npm dependencies.
3. Set `cache-dependency-path: frontend/package-lock.json` in `actions/setup-node`.

**Detection:** Frontend dependencies not cached; CI installs all npm packages every run.

**Phase:** CI/CD pipeline setup phase.

**Confidence:** HIGH -- verified that `frontend/` is a separate directory with its own `package-lock.json`.

---

### Pitfall 15: Gradle Wrapper Permission Issue on GitHub Actions

**What goes wrong:** The `gradlew` script may not have execute permission in the git repository. GitHub Actions runners on Linux require `chmod +x gradlew` before running Gradle commands. JHipster's own CI docs mention this as a known issue.

**Prevention:**
1. Ensure `gradlew` has execute permission in git: `git update-index --chmod=+x gradlew`.
2. Or add `chmod +x gradlew` as a CI step before any Gradle command.
3. Use `gradle/actions/setup-gradle@v4` which handles this automatically.

**Detection:** CI fails with "Permission denied" on first Gradle command.

**Phase:** CI/CD pipeline setup phase.

**Confidence:** HIGH -- explicitly mentioned in JHipster CI documentation.

---

### Pitfall 16: Health Check Timing Too Aggressive for Cold Start

**What goes wrong:** The existing `app.yml` health check uses `interval: 5s, timeout: 5s, retries: 40` (total 200s). Spring Boot with Liquibase migrations, Hazelcast startup, and JPA entity scanning on Java 25 in a resource-constrained container can take 60-120 seconds to start. This is within the retry window, but if the container has limited CPU (common in CI or small production VMs), startup can exceed 200s and Docker marks the container as unhealthy.

**Prevention:**
1. Add `start_period: 60s` to the health check to give the JVM time to start before counting retries.
2. Increase retries or interval for production: `interval: 10s, retries: 30, start_period: 90s`.
3. Use Spring Boot's `/management/health/readiness` endpoint instead of `/management/health` if available, to distinguish startup from steady-state health.
4. In CI, where runners have limited resources, use even more generous timeouts.

**Detection:** Container marked unhealthy during startup; dependent services fail to start.

**Phase:** Docker production deployment phase.

**Confidence:** MEDIUM -- depends on actual container resource allocation and migration complexity.

---

### Pitfall 17: Request-Scoped Bean Not Available in k6/Security Validation Tests

**What goes wrong:** `RequestPermissionSnapshot` is `@Scope("request")`. When running security validation tests that directly invoke service methods (not through HTTP), the request scope is not active. The fallback path in `RolePermissionServiceDbImpl.isEntityOpPermitted` handles this via `RequestPermissionSnapshot.isRequestScopeActive()`, but if security validation tests only exercise the HTTP path and do not test the fallback, batch jobs or scheduled tasks in production may silently bypass permission caching and hit the database for every permission check.

**Prevention:**
1. Include non-HTTP security validation scenarios: test permission enforcement from a `@Scheduled` method or async context.
2. Verify the `isRequestScopeActive()` fallback path returns correct results (it queries the DB directly, which is correct but slow).
3. Document which execution contexts use the cached vs. uncached permission path.

**Detection:** Batch jobs or async tasks show high database query volume for permission checks.

**Phase:** Security validation phase.

**Confidence:** MEDIUM -- the fallback path exists and works; risk is performance, not correctness.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| CI/CD Pipeline Setup | Testcontainers Docker socket not available in container-based CI jobs (Pitfall 4) | Use runner-level Docker, not container jobs, for integration tests |
| CI/CD Pipeline Setup | Gradle build cache poisoning across dependency updates (Pitfall 7) | Include `libs.versions.toml` and `buildSrc` in cache key hash |
| CI/CD Pipeline Setup | Frontend npm cache misses due to subdirectory layout (Pitfall 14) | Use `frontend/package-lock.json` in cache key |
| CI/CD Pipeline Setup | `gradlew` permission denied (Pitfall 15) | Use `gradle/actions/setup-gradle@v4` or `chmod +x` |
| Docker Production | Hardcoded credentials in `application-prod.yml` baked into image (Pitfall 2) | Replace with env vars before building any image |
| Docker Production | No Serializable on PermissionMatrix breaks Hazelcast clustering (Pitfall 1) | Add Serializable, test round-trip before deploying |
| Docker Production | JVM OOM kill from container memory mismatch (Pitfall 5) | Set container limit to 2x Xmx, use MaxRAMPercentage |
| Docker Production | No frontend container in Docker Compose (Pitfall 6) | Add nginx container with SPA routing |
| Docker Production | PostgreSQL trust auth in production (Pitfall 11) | Use password auth in production compose file |
| Docker Production | Deprecated `-noverify` flag (Pitfall 12) | Remove from entrypoint script |
| Docker Production | Health check too aggressive for cold start (Pitfall 16) | Add `start_period` to health check |
| Permission Optimization | Cache eviction silently stops working after refactoring (Pitfall 3) | Integration test: write permission, verify immediate enforcement |
| Permission Optimization | Cascade logic breaks in restructured data (Pitfall 9) | Comprehensive cascade test suite before refactoring |
| Security Validation | Test/prod profile drift hides security bugs (Pitfall 10) | Test against Docker Compose production stack |
| Security Validation | Dev-scale benchmarks do not predict production performance (Pitfall 13) | Re-run k6 against production-topology stack |
| Security Validation | Request-scoped bean absence in non-HTTP contexts (Pitfall 17) | Test permission enforcement in async/batch contexts |

## Ordering Implications

**Address first (blocking):**
1. Pitfall 2 (hardcoded credentials) -- must fix before any Docker image build
2. Pitfall 1 (PermissionMatrix Serializable) -- must fix before Hazelcast clustering in production
3. Pitfall 4 (Testcontainers in CI) -- must solve to get any CI working

**Address during implementation:**
4. Pitfalls 5, 6, 11, 12, 16 -- Docker production hardening (all in the same phase)
5. Pitfalls 3, 9 -- Permission optimization safety nets (before any optimization code changes)
6. Pitfalls 7, 8, 14, 15 -- CI performance and reliability (iterative improvement)

**Address last (validation):**
7. Pitfalls 10, 13, 17 -- Security validation against production topology (requires Docker stack to be running)

## Sources

- `src/main/java/com/vn/core/security/permission/PermissionMatrix.java` -- no `Serializable` implementation
- `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` -- Hazelcast IMap usage, request-scoped caching
- `src/main/java/com/vn/core/service/security/SecPermissionService.java` -- `@CacheEvict` eviction pattern
- `src/main/java/com/vn/core/config/CacheConfiguration.java` -- Hazelcast configuration, backup-count
- `src/main/resources/config/application-prod.yml` -- hardcoded credentials, Hazelcast prod settings
- `src/main/docker/app.yml` -- Docker Compose app configuration, health checks, JVM flags
- `src/main/docker/postgresql.yml` -- trust authentication, no password
- `src/main/docker/jib/entrypoint.sh` -- deprecated `-noverify` flag
- `src/test/java/com/vn/core/IntegrationTest.java` -- Testcontainers usage
- `gradle/spring-boot.gradle` -- test/integration test task configuration
- `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle` -- Jib Docker build configuration
- [JHipster CI/CD documentation](https://www.jhipster.tech/setting-up-ci/) -- wrapper permissions, CI setup guidance
- [JHipster Docker Compose documentation](https://www.jhipster.tech/docker-compose/) -- Jib build, JVM memory settings
- [JHipster Production documentation](https://www.jhipster.tech/production/) -- production profile, TLS, cache tuning
