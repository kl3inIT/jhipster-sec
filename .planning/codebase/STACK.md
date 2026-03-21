# Technology Stack

**Analysis Date:** 2026-03-21

## Languages

**Primary:**
- Java 21 - application code lives under `src/main/java/com/vn/core/**` and tests under `src/test/java/com/vn/core/**`; the version is set in `build.gradle`.

**Secondary:**
- Groovy (Gradle DSL) - build logic is defined in `build.gradle`, `settings.gradle`, `gradle/*.gradle`, and `buildSrc/src/main/groovy/*.gradle`.
- YAML - runtime and infrastructure configuration lives in `src/main/resources/config/*.yml` and `src/main/docker/*.yml`.
- JSON - project metadata and Node tooling configuration live in `package.json`, `package-lock.json`, `.yo-rc.json`, and `sonar-project.properties`.
- HTML/Thymeleaf - server-rendered mail templates live in `src/main/resources/templates/mail/*.html`.
- Properties/XML - logging, Gradle, and migration metadata live in `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `src/main/resources/logback-spring.xml`, and `src/main/resources/config/liquibase/*.xml`.

## Runtime

**Environment:**
- JVM: Java 21, configured by `sourceCompatibility` and `targetCompatibility` in `build.gradle`.
- Gradle Wrapper: 9.4.0, pinned in `gradle/wrapper/gradle-wrapper.properties`.
- Node.js: `>=24.14.0`, declared in `package.json`; it is used for scripts, formatting, hooks, and Docker orchestration rather than a browser app.
- Container runtime: Jib builds an OCI image from `eclipse-temurin:21-jre-noble` in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.

**Package Manager:**
- Gradle Wrapper - backend build and test entrypoint via `gradlew` and `gradlew.bat`.
- npm - developer scripts and JS tooling via `package.json`.
- Lockfile: `package-lock.json` present; Gradle wrapper present in `gradle/wrapper/gradle-wrapper.properties`.

## Frameworks

**Core:**
- Spring Boot 4.0.3 - application framework, declared in `gradle/libs.versions.toml` and bootstrapped from `src/main/java/com/vn/core/JhipsterSecApp.java`.
- JHipster Framework 9.0.0 - application conventions and config helpers, declared in `gradle/libs.versions.toml`; project generation metadata lives in `.yo-rc.json`.
- Spring MVC + Tomcat + Validation + Jackson + AspectJ + Thymeleaf - enabled by starters in `build.gradle`.
- Spring Data JPA + Hibernate ORM - persistence layer configured by starters in `build.gradle` and Hibernate settings in `src/main/resources/config/application.yml`.
- Spring Security + OAuth2 Resource Server - stateless JWT security configured in `src/main/java/com/vn/core/config/SecurityConfiguration.java` and `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.
- Spring Mail + Thymeleaf templates - outbound account emails implemented in `src/main/java/com/vn/core/service/MailService.java` and `src/main/resources/templates/mail/*.html`.
- Springdoc OpenAPI 3.0.2 - API documentation support from `gradle/libs.versions.toml` and `src/main/resources/config/application.yml`.
- MapStruct 1.6.3 - DTO/entity mapping declared in `gradle/libs.versions.toml` and used in `src/main/java/com/vn/core/service/mapper/UserMapper.java`.

**Testing:**
- JUnit Platform - enabled by `test` and `integrationTest` in `gradle/spring-boot.gradle`.
- Spring Boot Test + Spring Security Test - declared in `build.gradle`.
- Testcontainers + PostgreSQL - integration database support declared in `build.gradle` and implemented in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
- ArchUnit 1.4.1 - architecture tests declared in `gradle/libs.versions.toml` and used from `src/test/java/com/vn/core/TechnicalStructureTest.java`.

**Build/Dev:**
- Liquibase 5.0.1 with Gradle plugin 3.1.0 - schema migration tooling configured in `gradle.properties` and `gradle/liquibase.gradle`.
- Jib 3.5.3 - container image build plugin declared in `buildSrc/gradle/libs.versions.toml` and applied by `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- JaCoCo 0.8.14 - coverage tooling configured in `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- SonarQube Gradle plugin 7.2.3.7755 - static analysis integration declared in `buildSrc/gradle/libs.versions.toml` and configured by `sonar-project.properties`.
- Spotless 8.3.0 - Java formatting plugin declared in `buildSrc/gradle/libs.versions.toml`.
- Modernizer 1.12.0 and NoHTTP 0.0.11 - build hygiene plugins declared in `buildSrc/gradle/libs.versions.toml`.
- Checkstyle 13.3.0 - code style enforcement configured in `checkstyle.xml` and `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- Prettier 3.8.1 with `prettier-plugin-java` 2.8.1 and `prettier-plugin-packagejson` 3.0.2 - formatting for non-Java and Java files via `package.json` and `.prettierrc`.
- Husky 9.1.7 and lint-staged 16.3.3 - local git hook tooling configured in `package.json` and `.lintstagedrc.cjs`.

## Key Dependencies

**Critical:**
- `tech.jhipster:jhipster-framework` 9.0.0 - JHipster runtime support used across `src/main/java/com/vn/core/config/**`.
- `org.springframework.boot:*` starters - the HTTP API, security, JPA, mail, actuator, cache, and validation stack defined in `build.gradle`.
- `org.postgresql:postgresql` - JDBC driver for the primary SQL database, declared in `build.gradle`.
- `org.liquibase:liquibase-core` - schema migration engine configured in `gradle/liquibase.gradle` and `src/main/resources/config/liquibase/master.xml`.
- `com.hazelcast:hazelcast-spring` 5.5.0 and `com.hazelcast:hazelcast-hibernate53` 5.2.0 - application cache and Hibernate second-level cache configured in `src/main/java/com/vn/core/config/CacheConfiguration.java`.
- `io.micrometer:micrometer-registry-prometheus-simpleclient` - Prometheus metrics export enabled from `build.gradle` and `src/main/resources/config/application.yml`.
- `org.springdoc:springdoc-openapi-starter-webmvc-api` 3.0.2 - API docs support for `/v3/api-docs`, declared in `gradle/libs.versions.toml`.
- `org.mapstruct:mapstruct` 1.6.3 - compile-time mapping support used by `src/main/java/com/vn/core/service/mapper/UserMapper.java`.

**Infrastructure:**
- `com.zaxxer:HikariCP` - datasource pooling configured through `spring.datasource.hikari` in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.
- `org.testcontainers:*` - containerized test dependencies declared in `build.gradle`.
- `com.google.cloud.tools:jib-gradle-plugin` 3.5.3 - container image packaging from Gradle without a Dockerfile, configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.

## Configuration

**Environment:**
- Base runtime configuration lives in `src/main/resources/config/application.yml`.
- Profile-specific overrides live in `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, and `src/main/resources/config/application-tls.yml`.
- The default Gradle profile is `dev`, set in `gradle.properties` and expanded into `application.yml` by `gradle/spring-boot.gradle`.
- `spring.docker.compose.enabled` is currently `false` in `src/main/resources/config/application.yml`; Docker services are started explicitly through npm scripts in `package.json` and compose files in `src/main/docker/*.yml`.
- The project is server-only: `.yo-rc.json` sets `skipClient` to `true`, and there is no generated web client under `src/main/webapp/`.
- Secrets are not externalized through `.env` files or a secret manager in the current repository state; runtime credentials are stored directly in tracked config files such as `src/main/resources/config/application-*.yml`, `gradle/liquibase.gradle`, and `src/main/docker/*.yml`.

**Build:**
- Primary build files are `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/*.gradle`, and `buildSrc/**`.
- Quality configuration lives in `checkstyle.xml`, `sonar-project.properties`, `.prettierrc`, `.editorconfig`, and `.lintstagedrc.cjs`.
- Generator metadata lives in `.yo-rc.json`, which defines monolith/JWT/PostgreSQL/Hazelcast generation choices.

## Platform Requirements

**Development:**
- JDK 21 is required by `build.gradle`.
- Gradle 9.4.0 is supplied by `gradle/wrapper/gradle-wrapper.properties`.
- Node.js 24.14.0 or newer is required by `package.json`.
- Docker Compose is required for the local dependency stacks defined in `src/main/docker/services.yml`, `src/main/docker/postgresql.yml`, `src/main/docker/monitoring.yml`, `src/main/docker/sonar.yml`, and `src/main/docker/jhipster-control-center.yml`.
- PostgreSQL is required either through the container in `src/main/docker/postgresql.yml` or an externally reachable database matching the JDBC settings in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.

**Production:**
- Deploy the service as a JVM artifact with `bootJar` or `bootWar` tasks described in `package.json` and `README.md`, or as a container image via Jib in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- Production requires a PostgreSQL database, a JWT secret for `jhipster.security.authentication.jwt.base64-secret`, and optional SMTP, Prometheus, and Logstash endpoints configured through `src/main/resources/config/application-prod.yml` or environment overrides.
- The generated Jib image exposes port `8080` for HTTP and `5701/udp` for Hazelcast, as configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.

---

*Stack analysis: 2026-03-21*
