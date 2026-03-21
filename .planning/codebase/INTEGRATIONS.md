# External Integrations

**Analysis Date:** 2026-03-21

## APIs & External Services

**Business APIs:**
- Not detected. The code under `src/main/java/com/vn/core/**` does not include SDK imports or outbound HTTP client integrations for Stripe, AWS, Twilio, SendGrid, Slack, or similar third-party business APIs.

**Infrastructure Services:**
- PostgreSQL - primary relational database for the application, wired through `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `gradle/liquibase.gradle`, `src/main/docker/postgresql.yml`, and `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
  - SDK/Client: `org.postgresql:postgresql`, Spring Data JPA, Hibernate, and HikariCP from `build.gradle`.
  - Auth: datasource username/password are configured in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`; container overrides are present in `src/main/docker/app.yml`.
- Hazelcast - embedded distributed cache and Hibernate second-level cache, configured in `src/main/java/com/vn/core/config/CacheConfiguration.java` and `src/main/resources/config/application.yml`.
  - SDK/Client: `com.hazelcast:hazelcast-spring` and `com.hazelcast:hazelcast-hibernate53` from `build.gradle` and `gradle/libs.versions.toml`.
  - Auth: none detected; management UI is exposed separately by `src/main/docker/hazelcast-management-center.yml`.
- SMTP mail relay - outbound account lifecycle emails are sent by `src/main/java/com/vn/core/service/MailService.java` using templates in `src/main/resources/templates/mail/*.html`.
  - SDK/Client: Spring Mail via `spring-boot-starter-mail` in `build.gradle`.
  - Auth: `spring.mail.*` properties are configured in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`; no dedicated env-var-only mail secret flow is defined.
- Prometheus and Grafana - local monitoring stack defined in `src/main/docker/monitoring.yml`, with scrape config in `src/main/docker/prometheus/prometheus.yml` and Grafana provisioning in `src/main/docker/grafana/provisioning/**`.
  - SDK/Client: `io.micrometer:micrometer-registry-prometheus-simpleclient` in `build.gradle`.
  - Auth: local compose setup uses default Grafana env vars in `src/main/docker/monitoring.yml`; no app-side auth integration is required for Prometheus scraping.
- JHipster Control Center - optional operations companion defined in `src/main/docker/jhipster-control-center.yml`.
  - SDK/Client: external Docker service only; the application integrates by exposing management endpoints and sharing the JWT secret.
  - Auth: compose env vars include `SPRING_SECURITY_USER_PASSWORD` and `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET` in `src/main/docker/jhipster-control-center.yml`.
- SonarQube - local code quality service for developer workflows, defined in `src/main/docker/sonar.yml` and configured by `sonar-project.properties`.
  - SDK/Client: `org.sonarsource.scanner.gradle:sonarqube-gradle-plugin` from `buildSrc/gradle/libs.versions.toml`.
  - Auth: `sonar.login` and `sonar.password` properties are referenced in `README.md` and `sonar-project.properties`, but no secret manager integration is present.

## Data Storage

**Databases:**
- PostgreSQL
  - Connection: `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` are set in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`. Containerized deployment overrides use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_LIQUIBASE_URL` in `src/main/docker/app.yml`.
  - Client: Spring Data JPA/Hibernate from `build.gradle`, with schema managed by Liquibase through `src/main/resources/config/liquibase/master.xml` and `gradle/liquibase.gradle`.

**File Storage:**
- Local filesystem/classpath only. Templates, i18n bundles, and logging config live under `src/main/resources/**`; no S3, Azure Blob, GCS, or other object storage integration is detected.

**Caching:**
- Hazelcast embedded cache in `src/main/java/com/vn/core/config/CacheConfiguration.java`, backed by JHipster cache properties in `src/main/resources/config/application.yml`, `src/main/resources/config/application-dev.yml`, and `src/main/resources/config/application-prod.yml`.

## Authentication & Identity

**Auth Provider:**
- Custom local authentication with JWT tokens
  - Implementation: credentials are authenticated through Spring Security in `src/main/java/com/vn/core/config/SecurityConfiguration.java`; JWT tokens are issued by `src/main/java/com/vn/core/web/rest/AuthenticateController.java`; signing and verification are implemented by Nimbus-based beans in `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.
- User and authority data are stored in the local database through `src/main/java/com/vn/core/domain/User.java`, `src/main/java/com/vn/core/domain/Authority.java`, `src/main/java/com/vn/core/repository/UserRepository.java`, and `src/main/java/com/vn/core/repository/AuthorityRepository.java`.
- External identity providers are not configured. `.yo-rc.json` sets `authenticationType` to `jwt`, and there is no `issuer-uri`, `jwk-set-uri`, Keycloak, Okta, Auth0, or OAuth client registration in `src/main/resources/config/*.yml`.

## Monitoring & Observability

**Error Tracking:**
- None detected. There are no Sentry, Bugsnag, Rollbar, New Relic, or Datadog application SDKs declared in `build.gradle` or referenced under `src/main/java/com/vn/core/**`.

**Logs:**
- Logback is the active logging backend via `src/main/resources/logback-spring.xml`.
- Optional JSON logging and Logstash TCP forwarding are wired in `src/main/java/com/vn/core/config/LoggingConfiguration.java` and configured through `jhipster.logging.use-json-format` and `jhipster.logging.logstash.*` in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.

**Metrics and Health:**
- Spring Boot Actuator management endpoints are exposed under `/management` by `src/main/resources/config/application.yml`.
- Prometheus metrics export is enabled in `src/main/resources/config/application.yml` and can be re-enabled for containers through `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED` in `src/main/docker/app.yml`.
- Prometheus scrapes `/management/prometheus` according to `src/main/docker/prometheus/prometheus.yml`.
- Grafana dashboards and datasource provisioning live in `src/main/docker/grafana/provisioning/**`.

**Admin/Ops Consoles:**
- JHipster Control Center is available through `src/main/docker/jhipster-control-center.yml`.
- Hazelcast Management Center is available through `src/main/docker/hazelcast-management-center.yml`.
- SonarQube is available through `src/main/docker/sonar.yml`.

## CI/CD & Deployment

**Hosting:**
- Not pinned to a cloud provider. The current repository supports direct JVM deployment from `README.md` and `package.json`, plus container image builds through Jib in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.

**CI Pipeline:**
- None detected. There is no `.github/workflows/**`, `.gitlab-ci.yml`, `Jenkinsfile`, or `.circleci/**` file in the repository root scan.

**Deployment Wiring:**
- `src/main/docker/services.yml` starts dependency services only.
- `src/main/docker/app.yml` runs the application image plus PostgreSQL for a local full stack.
- `package.json` exposes the main operational commands: `services:up`, `app:up`, `java:jar:*`, `java:war:*`, and `java:docker*`.

## Environment Configuration

**Required env vars:**
- Application container overrides in `src/main/docker/app.yml`: `_JAVA_OPTIONS`, `SPRING_PROFILES_ACTIVE`, `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_LIQUIBASE_URL`.
- JHipster Control Center overrides in `src/main/docker/jhipster-control-center.yml`: `_JAVA_OPTIONS`, `SPRING_PROFILES_ACTIVE`, `SPRING_SECURITY_USER_PASSWORD`, `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET`, `SPRING_CLOUD_DISCOVERY_CLIENT_SIMPLE_INSTANCES_JHIPSTER-SEC_0_URI`, and `LOGGING_FILE_NAME`.
- No `.env`-based variable loading is referenced from `package.json`, `src/main/resources/config/application.yml`, or the compose files.

**Secrets location:**
- Current secrets and sample credentials are stored directly in tracked configuration files such as `src/main/resources/config/application-secret-samples.yml`, `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `gradle/liquibase.gradle`, and `src/main/docker/jhipster-control-center.yml`.
- External secret storage such as Vault, AWS Secrets Manager, Azure Key Vault, Kubernetes Secrets, or Doppler is not detected.

## Webhooks & Callbacks

**Incoming:**
- None detected. Controllers under `src/main/java/com/vn/core/web/rest/**` expose authentication, account, user, authority, and management-related APIs, not webhook receivers.

**Outgoing:**
- SMTP email delivery from `src/main/java/com/vn/core/service/MailService.java`.
- No outbound webhook publisher or generic HTTP client integration is detected in `src/main/java/com/vn/core/**`.

---

*Integration audit: 2026-03-21*
