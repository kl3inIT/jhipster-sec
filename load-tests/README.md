# Performance Benchmarks

k6 load tests comparing the @SecuredEntity pipeline overhead against an unsecured baseline.

## Prerequisites

- **k6** — install via `winget install k6 --source winget` or download from
  https://github.com/grafana/k6/releases
- **Backend running** with dev profile (includes `api-docs` profile group which activates the
  benchmark baseline endpoint at `/api/benchmark/organizations-standard`)
- **Seeded test data** — the `dev` + `faker` Liquibase contexts seed Organization data automatically

## Starting the Backend

```bash
./gradlew bootRun
```

The dev profile activates by default and includes the `api-docs` profile group. Verify the
benchmark endpoint is available:

```bash
curl -s http://localhost:8080/api/benchmark/organizations-standard -H "Authorization: Bearer <token>" | head -c 200
```

## Running Benchmarks

### Organization List (GET /api/organizations)

```bash
k6 run load-tests/scripts/org-list-benchmark.js
```

### Organization Detail (GET /api/organizations/{id})

```bash
k6 run load-tests/scripts/org-detail-benchmark.js
```

### Custom Configuration

Override the base URL or organization ID:

```bash
k6 run --env BASE_URL=http://localhost:9090 load-tests/scripts/org-list-benchmark.js
k6 run --env ORG_ID=42 load-tests/scripts/org-detail-benchmark.js
```

## Results

After each run, k6 writes results to `load-tests/results/`:

- `org-list-summary.md` — Markdown table with p50/p95/p99 latency comparison
- `org-list-raw.json` — Full k6 metric dump
- `org-detail-summary.md` — Same format for single-entity endpoint
- `org-detail-raw.json` — Full k6 metric dump

### Overhead Target

The acceptable overhead target is **< 10% p95 latency delta** between the secured
`@SecuredEntity` pipeline and the unsecured baseline. The summary report flags if this is
exceeded but does not block builds.

## What Is Being Measured

| Endpoint                          | Pipeline               | Security Checks                                  |
| --------------------------------- | ---------------------- | ------------------------------------------------ |
| `GET /api/organizations`                       | SecureDataManagerImpl  | CRUD check + permission cache + secure serialize |
| `GET /api/benchmark/organizations-standard`    | Standard JHipster flow | None (same serializer, no permission checks)     |

Both endpoints use the same `SecuredEntityJsonAdapter` serializer with the `organization-list`
fetch plan. The only difference is the CRUD permission check and permission cache lookup in
the secured path.

## Concurrency Levels

Each benchmark runs at 1, 10, and 50 virtual users (per the test specification). Each level
runs for 30 seconds with secured and baseline scenarios executing in parallel.
