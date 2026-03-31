---
created: 2026-03-27T17:16:05.273Z
title: Cache current-user permission checks
area: general
files:
  - src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java:47
  - src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java:35
  - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java:44
  - src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java:39
  - src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java:46
  - frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts:34
---

## Problem

Reloading a secured entity page currently causes repeated authority and permission lookups on the backend. `MergedSecurityContextBridge` revalidates JWT authority names against `jhi_authority` on each call, and entity or attribute checks query `sec_permission` again during CRUD gating and response serialization. The dedicated `/api/security/entity-capabilities` endpoint already builds a bulk permission matrix once, but that matrix is not reused by the rest of the secured read pipeline. Frontend capability caching helps only on the client; the backend still must enforce permissions server-side and cannot trust client state.

## Solution

Introduce a backend permission snapshot that is reused within one request. First step: add a request-scoped cache or request-bound context that loads validated authority names and the relevant `sec_permission` rows once, then serves entity and attribute checks from memory for the rest of that request. Keep live permission refresh semantics by rebuilding the snapshot on the next request rather than persisting it across the session. If more reduction is needed after that, consider a second-stage cross-request cache keyed by user or authority set, but only with explicit invalidation when role membership, security permissions, menu permissions, or authority rows change.
