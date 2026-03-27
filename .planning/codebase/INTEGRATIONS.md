# External Integrations

**Analysis Date:** 2026-03-27

## APIs & External Services

**First-party HTTP APIs:**
- Root Spring REST API - consumed by the standalone Angular app through `frontend/proxy.conf.json`, `frontend/src/app/core/config/application-config.service.ts`, and controllers under `src/main/java/com/vn/core/web/rest/**`.
  - SDK/Client: Angular `HttpClient` plus interceptors in `frontend/src/app/core/interceptor/*.ts`
  - Auth: Bearer JWTs issued by `src/main/java/com/vn/core/web/rest/AuthenticateController.java` and attached by `frontend/src/app/core/interceptor/auth.interceptor.ts`
- Security and admin API surface - the frontend currently integrates with account, admin user, security catalog, roles, permissions, row policies, menu definitions, menu permissions, and secured entity endpoints exposed from `src/main/java/com/vn/core/web/rest/**` and `src/main/java/com/vn/core/web/rest/admin/security/**`.
  - SDK/Client: service wrappers under `frontend/src/app/pages/admin/**/service/*.ts`, `frontend/src/app/pages/entities/**/service/*.ts`, and `frontend/src/app/layout/navigation/navigation.service.ts`
  - Auth: route guards and token lifecycle are handled in `frontend/src/app/core/auth/*.ts` and `frontend/src/app/core/interceptor/*.ts`
- Frontend-to-backend development proxy - the Angular dev server proxies `/api`, `/services`, `/management`, `/v3/api-docs`, `/h2-console`, and `/health` to the backend in `frontend/proxy.conf.json`.
  - SDK/Client: Angular dev server in `frontend/angular.json`
  - Auth: same-origin proxy for local development; application auth still uses JWT

**Email/SMTP:**
- SMTP server - outbound activation, creation, and reset-password mail is sent by `src/main/java/com/vn/core/service/MailService.java` using templates in `src/main/resources/templates/mail/*.html`.
  - SDK/Client: Spring `JavaMailSender`
  - Auth: Spring Mail properties from `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`

**Monitoring and admin tooling:**
- Prometheus - metrics are exposed at `/management/prometheus` through `src/main/resources/config/application.yml` and scraped by `src/main/docker/prometheus/prometheus.yml`.
  - SDK/Client: `io.micrometer:micrometer-registry-prometheus-simpleclient`
  - Auth: `/management/prometheus` is permitted by `src/main/java/com/vn/core/config/SecurityConfiguration.java`
- Grafana - optional local dashboards are provisioned from `src/main/docker/grafana/provisioning/**`.
  - SDK/Client: Docker image `grafana/grafana:12.4.1` in `src/main/docker/monitoring.yml`
  - Auth: local Grafana defaults configured in provisioning files under `src/main/docker/grafana/provisioning/**`
- Hazelcast Management Center - optional local cache inspection is defined in `src/main/docker/hazelcast-management-center.yml`.
  - SDK/Client: Docker image `hazelcast/management-center:5.10.0`
  - Auth: local container configuration only
- JHipster Control Center - optional local admin console is defined in `src/main/docker/jhipster-control-center.yml`.
  - SDK/Client: Docker image `jhipster/jhipster-control-center:v0.5.0`
  - Auth: Spring environment variables defined in `src/main/docker/jhipster-control-center.yml`
- SonarQube - optional local code-quality server is defined in `src/main/docker/sonar.yml` and configured by `sonar-project.properties`.
  - SDK/Client: Docker image `sonarqube:26.3.0.120487-community`
  - Auth: local Sonar credentials referenced in `README.md` and `sonar-project.properties`

**Messaging:**
- Not detected - no Kafka, RabbitMQ, JMS, AMQP, or other queue/stream client is referenced in `build.gradle`, `src/main/java/com/vn/core/**`, or `frontend/src/**`.

## Data Storage

**Databases:**
- PostgreSQL - the primary relational store for users, authorities, security rules, and secured entities is configured in `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `gradle/liquibase.gradle`, and `src/main/docker/postgresql.yml`.
  - Connection: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_LIQUIBASE_URL` appear in `src/main/docker/app.yml`; equivalent Spring properties are defined in `src/main/resources/config/application-*.yml`
  - Client: Spring Data JPA and Hibernate repositories under `src/main/java/com/vn/core/repository/**` and `src/main/java/com/vn/core/security/repository/**`
- Test PostgreSQL - integration tests use a containerized PostgreSQL instance in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
  - Connection: dynamic properties are registered from Testcontainers in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`
  - Client: Spring Boot integration tests under `src/test/java/com/vn/core/**`

**File Storage:**
- Local filesystem and classpath only - frontend static assets live in `frontend/public/**`, mail templates live in `src/main/resources/templates/**`, and fetch-plan definitions live in `src/main/resources/fetch-plans.yml`.

**Caching:**
- Embedded Hazelcast - application cache and Hibernate second-level cache are configured in `src/main/java/com/vn/core/config/CacheConfiguration.java` and `src/main/resources/config/application.yml`.
- Browser session/local storage - the frontend stores JWTs, cached menu permissions, and cached secured entity capabilities in `frontend/src/app/core/auth/state-storage.service.ts`, `frontend/src/app/layout/navigation/navigation.service.ts`, and `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`.

## Authentication & Identity

**Auth Provider:**
- Custom username/email + password login backed by the local `User` and `Authority` tables - authentication is implemented in `src/main/java/com/vn/core/security/DomainUserDetailsService.java`, token issuance in `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, token validation in `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`, and request enforcement in `src/main/java/com/vn/core/config/SecurityConfiguration.java`.
  - Implementation: Spring Security authenticates against PostgreSQL, issues HMAC-signed JWTs, and the Angular client stores and sends the token through `frontend/src/app/core/auth/auth-jwt.service.ts`, `frontend/src/app/core/interceptor/auth.interceptor.ts`, and `frontend/src/app/core/interceptor/auth-expired.interceptor.ts`
- Current-user merged authority resolution - runtime permission checks validate JWT authority names against persisted authority rows in `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java`.
  - Implementation: permission, row-policy, menu-permission, and secure-data services resolve access from DB-backed security repositories under `src/main/java/com/vn/core/security/repository/**`
- External identity provider - not detected; there is no Keycloak, LDAP, external OIDC client, or SSO connector wired in `src/main/java/com/vn/core/**` or `frontend/src/**`.

## Monitoring & Observability

**Error Tracking:**
- None detected - no Sentry, Rollbar, New Relic, Bugsnag, or similar SDK is referenced in `src/main/java/com/vn/core/**` or `frontend/src/**`.

**Logs:**
- Backend logging uses Logback with optional JSON and Logstash socket forwarding via `src/main/java/com/vn/core/config/LoggingConfiguration.java`, `src/main/resources/logback-spring.xml`, `src/main/resources/config/application-dev.yml`, and `src/main/resources/config/application-prod.yml`.
- Frontend logging is minimal browser-side warning/error output from interceptors such as `frontend/src/app/core/interceptor/error-handler.interceptor.ts`.

**Metrics:**
- Micrometer + Prometheus metrics are enabled in `src/main/resources/config/application.yml`; optional local dashboarding is defined in `src/main/docker/monitoring.yml` and `src/main/docker/grafana/provisioning/**`.

## CI/CD & Deployment

**Hosting:**
- Backend JVM and container deployment - Gradle `bootJar` and `bootWar` packaging is exposed through `package.json` and `README.md`, while OCI images are built by Jib from `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- Frontend standalone SPA deployment - the Angular app is built separately from `frontend/` using `ng build` in `frontend/package.json`; local dev traffic proxies to the backend through `frontend/proxy.conf.json`.
- Local Docker runtime stacks - `src/main/docker/app.yml` composes the backend image with PostgreSQL, `src/main/docker/services.yml` and `src/main/docker/postgresql.yml` bring up the database, `src/main/docker/monitoring.yml` brings up Prometheus and Grafana, and `src/main/docker/jhipster-control-center.yml` / `src/main/docker/sonar.yml` provide optional local tooling.

**CI Pipeline:**
- Not detected - no GitHub Actions, GitLab CI, CircleCI, Azure Pipelines, or other repository-native CI definition was found during this scan.

## Environment Configuration

**Required env vars:**
- `SPRING_PROFILES_ACTIVE` - used by the backend container in `src/main/docker/app.yml` and by the optional control center in `src/main/docker/jhipster-control-center.yml`.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` - primary database connection keys used in `src/main/docker/app.yml` and mirrored by Spring config in `src/main/resources/config/application-*.yml`.
- `SPRING_LIQUIBASE_URL` - Liquibase connection override used in `src/main/docker/app.yml`.
- `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET` - JWT signing secret consumed by `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.
- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, and `JHIPSTER_MAIL_BASE_URL` - mail delivery and link-generation settings implied by `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.
- `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED` - container-side metrics toggle set in `src/main/docker/app.yml`.
- `LOGGING_LOGSTASH_ENABLED`, `LOGGING_LOGSTASH_HOST`, and `LOGGING_LOGSTASH_PORT` - optional log shipping settings mapped from `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.
- `SERVER_API_URL` - frontend API prefix configured in `frontend/src/environments/environment.ts` and `frontend/src/environments/environment.development.ts`.

**Secrets location:**
- Sensitive values are currently stored directly in tracked Spring, Docker, and Gradle config files such as `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, `src/main/resources/config/application-tls.yml`, `gradle/liquibase.gradle`, `src/main/docker/app.yml`, and `src/main/docker/jhipster-control-center.yml`.
- No `.env`, `.env.*`, or other root dotenv files were detected in this repository scan.

## Webhooks & Callbacks

**Incoming:**
- None detected - the backend exposes REST endpoints under `src/main/java/com/vn/core/web/rest/**`, but no webhook-specific callback controller or signature verifier is present.

**Outgoing:**
- None detected - the application sends email through SMTP from `src/main/java/com/vn/core/service/MailService.java`, but no outbound webhook or event-delivery client is wired.

---

*Integration audit: 2026-03-27*
