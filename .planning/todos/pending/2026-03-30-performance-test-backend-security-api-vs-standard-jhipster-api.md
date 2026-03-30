---
created: 2026-03-30T08:20:29.560Z
title: Performance test backend security API vs standard JHipster API
area: testing
files: []
---

## Problem

Chưa có baseline hiệu năng để đánh giá overhead mà hệ thống core security gây ra so với API JHipster gốc. Khi mọi request entity đi qua pipeline `SecureDataManagerImpl` (catalog lookup → CRUD check → fetch-plan resolution → secure merge/serialize), cần biết latency và throughput thực tế bị ảnh hưởng bao nhiêu so với endpoint không qua security pipeline.

Cụ thể cần đo:
- API JHipster gốc (không qua `@SecuredEntity` / `SecureDataManagerImpl`)
- API đi qua full security pipeline (permission check + fetch-plan + secure serialize)
- Diff về p50/p95/p99 latency và throughput (req/s)

## Solution

Dùng JMeter hoặc công cụ tương đương (k6, Gatling, hey) để tạo test plan:

1. Chọn 1-2 entity endpoint đại diện (e.g. GET list, GET single, POST create)
2. Tạo test plan với 2 thread group: baseline (JHipster standard) vs secured
3. Chạy với các mức concurrency: 1, 10, 50, 100 users
4. Thu thập metrics: latency (p50/p95/p99), throughput, error rate
5. So sánh và ghi nhận overhead % của security pipeline
6. Xác định bottleneck nếu overhead > ngưỡng chấp nhận được (TBD, đề xuất < 20%)
