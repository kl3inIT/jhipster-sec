---
created: 2026-03-30T08:20:29.560Z
title: Performance test backend security API vs standard JHipster API
area: testing
files: []
---

## Problem

No performance baseline exists to measure the overhead introduced by the core security pipeline versus the stock JHipster API. Every entity request now passes through `SecureDataManagerImpl` (catalog lookup → CRUD check → fetch-plan resolution → secure merge/serialize). Without benchmark data, it is impossible to know how much latency and throughput are affected compared to endpoints that bypass the security pipeline.

Need to measure:
- Stock JHipster endpoint (no `@SecuredEntity` / `SecureDataManagerImpl`)
- Full security-pipeline endpoint (permission check + fetch-plan + secure serialize)
- Delta on p50/p95/p99 latency and throughput (req/s)

## Solution

Use JMeter or an equivalent tool (k6, Gatling, hey) to build a test plan:

1. Select 1–2 representative entity endpoints (e.g. GET list, GET single, POST create).
2. Create two thread groups: baseline (standard JHipster) vs. secured pipeline.
3. Run at multiple concurrency levels: 1, 10, 50, 100 virtual users.
4. Collect metrics: latency (p50/p95/p99), throughput, error rate.
5. Compare and document the overhead percentage introduced by the security pipeline.
6. Identify bottlenecks if overhead exceeds an acceptable threshold (TBD — suggested target: < 20 %).
