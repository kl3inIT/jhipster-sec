  # Benchmark Run Summary (2026-03-31)

## 1) Objective
- Measure and compare latency between:
- `GET /api/organizations` (secured pipeline)
- `GET /api/benchmark/organizations-standard` (baseline)
- `GET /api/organizations/{id}` (secured pipeline)
- `GET /api/benchmark/organizations-standard/{id}` (baseline)
- Target KPI from README: `p95 overhead < 10%`.

## 2) Benchmark Folder and Scripts
- Benchmark folder: `D:\jhipster\load-tests`
- Scripts:
- `load-tests/scripts/org-list-benchmark.js`
- `load-tests/scripts/org-detail-benchmark.js`
- `load-tests/scripts/auth.js`
- Output files:
- `load-tests/results/org-list-summary.md`
- `load-tests/results/org-list-raw.json`
- `load-tests/results/org-detail-summary.md`
- `load-tests/results/org-detail-raw.json`

## 3) Benchmark Parameters
- Run date: `2026-03-31` (Asia/Bangkok).
- Load test tool: `k6 v1.7.1`.
- Each script has 6 scenarios (secured + baseline in parallel):
- `1 VU` for `30s`
- `10 VU` for `30s` (start at `35s`)
- `50 VU` for `30s` (start at `70s`)
- `sleep(1)` is applied after each request iteration.
- Auth token is generated in `setup()` and reused during the run.

## 4) Benchmark Execution History

### Phase A: Initial Runs (Before Standardization)
- `k6` was not available initially:
- `winget` was not installed.
- `choco` did not have permission to write to `C:\ProgramData`.
- Installed `k6` from the GitHub release zip to:
- `C:\Users\admin\tools\k6\v1.7.1\k6-v1.7.1-windows-amd64\k6.exe`
- Added it to user `PATH`.
- Data issue found for detail benchmark:
- Script default is `ORG_ID=1`, but that ID returned `404` in current seed data.
- Valid ID confirmed as `1501` (`200` for both secured and baseline endpoints).

### Phase B: Standardized Benchmark Environment
- Restarted the app with a cleaner benchmark profile:
- `dev,api-docs`
- Runtime args used:
- `--logging.level.ROOT=INFO`
- `--logging.level.org.hibernate.SQL=WARN`
- `--logging.level.tech.jhipster=INFO`
- `--logging.level.com.vn.core=INFO`
- `--spring.datasource.hikari.maximum-pool-size=60`
- `--spring.datasource.hikari.minimum-idle=10`
- `--spring.jpa.properties.hibernate.generate_statistics=false`
- Health check passed: `GET /management/health = 200`.
- Re-ran benchmarks with:
- list benchmark: default script settings
- detail benchmark: `ORG_ID=1501`

## 5) Final Results (Standardized Run)

| Test | Baseline p95 (ms) | Secured p95 (ms) | p95 Overhead | KPI 10% Evaluation |
|---|---:|---:|---:|---|
| Organization List | 291.5 | 497.1 | 70.5% | FAIL |
| Organization Detail (ORG_ID=1501) | 64.6 | 340.1 | 426.6% | FAIL |

Additional notes:
- Script threshold (`p95 < 500` per endpoint) passed in the final run.
- Main KPI (`p95 overhead < 10%`) failed for both list and detail.

## 6) Important Factors Affecting Results
- Dev database is remote (`jdbc:postgresql://157.230.42.136:5555/jhipster-sec`), so network variance exists.
- Secured and baseline scenarios run in parallel at each VU stage, causing resource contention.
- `org-detail` baseline is very fast, so percentage overhead becomes very high once secured logic is added.
- Summary currently shows `p50/p99 = n/a` because current `k6 summaryTrendStats` does not include `p(50)`/`p(99)`.

## 7) Commands Used (Final Run)
- Start app:
```bash
./gradlew bootRun --args="--spring.profiles.active=dev,api-docs --logging.level.ROOT=INFO --logging.level.org.hibernate.SQL=WARN --logging.level.tech.jhipster=INFO --logging.level.com.vn.core=INFO --spring.datasource.hikari.maximum-pool-size=60 --spring.datasource.hikari.minimum-idle=10 --spring.jpa.properties.hibernate.generate_statistics=false"
```
- Run list benchmark:
```bash
k6 run load-tests/scripts/org-list-benchmark.js
```
- Run detail benchmark (valid ID):
```bash
k6 run --env ORG_ID=1501 load-tests/scripts/org-detail-benchmark.js
```

## 8) Artifacts
- [org-list-summary.md](D:\jhipster\load-tests\results\org-list-summary.md)
- [org-list-raw.json](D:\jhipster\load-tests\results\org-list-raw.json)
- [org-detail-summary.md](D:\jhipster\load-tests\results\org-detail-summary.md)
- [org-detail-raw.json](D:\jhipster\load-tests\results\org-detail-raw.json)
- App runtime logs:
- `D:\jhipster\output\benchmark-app.out.log`
- `D:\jhipster\output\benchmark-app.err.log`
