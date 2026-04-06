# Quick Task 260406-i2k Summary

## Goal
Update deployment assets for a production-only backend deployment that uses an existing PostgreSQL database, excludes Mailpit, keeps the server compose contract at `~/app/app.yml`, and includes repository-managed environment setup guidance.

## Changes Made

### Deployment compose template
- Updated `D:\jhipster\src\main\docker\app.yml` to an app-only production compose template.
- Removed bundled PostgreSQL and Mailpit services.
- Added `env_file: ./app.env` support.
- Kept the deployment target shape aligned with `~/app/app.yml`.
- Attached the service to the external Docker network `app-network` via `DOCKER_NETWORK`.
- Pointed datasource and Liquibase defaults at PostgreSQL database `postgres`.

### Environment setup template
- Added `D:\jhipster\src\main\docker\app.env.example`.
- Documented required production environment values for image selection, Docker network, datasource, Liquibase, mail relay, and JWT secret.

### Production config defaults
- Updated `D:\jhipster\src\main\resources\config\application-prod.yml` so production mail defaults no longer assume Mailpit.
- Default mail host now falls back to `127.0.0.1` and port `25`.

### Supporting deployment guidance
- Updated `D:\jhipster\.github\workflows\deploy.yml` operator prerequisite output to call out:
  - `~/app/app.yml`
  - matching `~/app/app.env`
  - external network `app-network`
  - existing PostgreSQL database `postgres`
  - app-only deployment responsibilities on the server
- Updated `D:\jhipster\src\main\docker\postgresql.yml` comments to clarify it is development-only and not part of production deployment.

## Verification Performed
- Parsed updated YAML files successfully with `python` + `yaml.safe_load(...)` for:
  - `D:/jhipster/.github/workflows/deploy.yml`
  - `D:/jhipster/src/main/docker/app.yml`
  - `D:/jhipster/src/main/docker/postgresql.yml`
  - `D:/jhipster/src/main/resources/config/application-prod.yml`
- Reviewed git diff for all changed deployment files.
- Searched for leftover Mailpit/PostgreSQL bundled-production assumptions in the updated deployment files and found none in the targeted files.

## Files Changed
- `D:\jhipster\.github\workflows\deploy.yml`
- `D:\jhipster\src\main\docker\app.yml`
- `D:\jhipster\src\main\docker\app.env.example`
- `D:\jhipster\src\main\docker\postgresql.yml`
- `D:\jhipster\src\main\resources\config\application-prod.yml`

## Notes
- No docs commit was created.
- The production server should copy `src/main/docker/app.yml` to `~/app/app.yml` and `src/main/docker/app.env.example` to `~/app/app.env`, then replace placeholders with real values before deployment.
