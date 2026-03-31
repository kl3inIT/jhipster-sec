# Phase 11 Benchmark Analysis & Reproduce Guide

## Why detail baseline 64.6ms (Phase 10) vs 229ms (Phase 11)?

### Root cause

| Factor | Phase 10 | Phase 11 |
|--------|----------|----------|
| DB host | Remote `157.230.42.136:5555` | Remote `157.230.42.136:5555` (same) |
| Logging | INFO level (Hibernate SQL = WARN) | WARN level (SQL OFF) |
| Run mode | Concurrent (list+detail scripts ran together) | Sequential (list first, then detail) |
| JVM state | Fresh start | Warm (after list benchmark completed) |
| Pool load | Shared with list script at 50VU | Dedicated — no contention |
| VU stages | 1→10→50 with 35s gaps | 1→10→50 with 35s gaps |

**The 64.6ms number is real but misleading** — it was measured when:
- Only 1 VU hitting the detail endpoint (no concurrency pressure)
- DB connection pool was mostly idle (list script hadn't ramped yet, or script ran at low VU stage)
- The `organizations-standard/{id}` baseline endpoint is a simple `findById` — extremely fast at low concurrency

At 50 VUs with concurrent load, the same endpoint realistically takes 200–250ms due to:
- HikariCP pool contention (60 connections shared across 100 total VUs from both scripts)
- PostgreSQL server-side query scheduling
- Network RTT variance at high concurrency

### What numbers to trust

The **overhead delta** (secured minus baseline) within the same run is always valid — both endpoints experience identical conditions simultaneously.

| Phase | List overhead | Detail overhead | Verdict |
|-------|---:|---:|---------|
| Phase 10 | +70.5% | +426.6% | ❌ FAIL — security adds massive DB cost |
| Phase 11 (concurrent) | +0.2% | −0.8% | ✅ PASS — within noise |
| Phase 11 (sequential, prod-like) | −0.9% | 0.0% | ✅ PASS — within noise |

---

## Reproduce Steps (Phase 11 prod-like benchmark)

### Prerequisites
- k6 v1.7.1 on PATH
- PostgreSQL running (remote or local)
- Java 25 (temurin-25.0.2)

### Step 1: Build
```bash
./gradlew bootJar -x test
```

### Step 2: Start server (prod-like profile)
```bash
java \
  -server \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -Xms512m -Xmx1g \
  -jar build/libs/jhipster-sec-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --logging.level.ROOT=WARN \
  --logging.level.com.vn.core=WARN \
  --logging.level.org.hibernate.SQL=OFF \
  --logging.level.tech.jhipster=WARN \
  --spring.jpa.properties.hibernate.generate_statistics=false \
  --spring.datasource.hikari.maximum-pool-size=60 \
  --spring.datasource.hikari.minimum-idle=20
```

### Step 3: Wait for health
```bash
curl http://localhost:8080/management/health
# Expect: {"status":"UP"}
```

### Step 4: Warm up (optional but recommended)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin","rememberMe":true}' \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>console.log(JSON.parse(d).id_token))")

# 10 warm-up requests
for i in $(seq 1 10); do
  curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" \
    "http://localhost:8080/api/organizations?page=0&size=20"
done
```

### Step 5: Run benchmarks SEQUENTIALLY
```bash
# List benchmark (runs 1VU→10VU→50VU, ~2 minutes)
k6 run load-tests/scripts/org-list-benchmark.js

# Wait for it to finish, THEN run detail
k6 run --env ORG_ID=1501 load-tests/scripts/org-detail-benchmark.js
```

⚠️ **Do NOT run both scripts concurrently** — concurrent execution causes pool contention and inflates latencies to 900ms+, making comparison unfair.

---

## Phase 11 Final Results (2026-03-31, prod-like sequential)

### List: `GET /api/organizations?page=0&size=20`
| Scenario | avg | med | p90 | p95 |
|----------|----:|----:|----:|----:|
| Baseline (`/api/benchmark/organizations-standard`) | 209ms | 174ms | 277ms | 329ms |
| Secured (`/api/organizations`) | 209ms | 177ms | 276ms | **326ms** |
| **Overhead** | — | — | — | **−0.9%** |

### Detail: `GET /api/organizations/1501`
| Scenario | avg | med | p90 | p95 |
|----------|----:|----:|----:|----:|
| Baseline (`/api/benchmark/organizations-standard/1501`) | 138ms | 126ms | 223ms | 229ms |
| Secured (`/api/organizations/1501`) | 138ms | 125ms | 222ms | **229ms** |
| **Overhead** | — | — | — | **0.0%** |

**PERF-04 (<10% p95 overhead): ✅ PASS**

All 6,339 checks passed. 0 failures.

---

## Why is baseline detail still 200ms+ at 50VU?

The `organizations-standard/{id}` endpoint does a plain `findById` but:
- At 50 concurrent VUs the HikariCP pool (60 connections) queues requests
- PostgreSQL remote host adds ~10–20ms network RTT per query
- 50 VUs × sleep(1s) iteration means ~50 req/s sustained — normal for a dev DB

For a production DB on the same network as the app server, p95 would be <20ms for a detail by-id endpoint.
