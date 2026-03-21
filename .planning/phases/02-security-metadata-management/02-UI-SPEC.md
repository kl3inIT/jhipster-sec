---
phase: 2
slug: security-metadata-management
status: draft
shadcn_initialized: false
preset: none
created: 2026-03-21
---

# Phase 2 — UI Design Contract

> Visual and interaction contract for Phase 2: Security Metadata Management.
> This phase is backend-only. No browser frontend is delivered in this phase (frontend screens are deferred to Phase 5 per CONTEXT.md §Deferred Ideas). This contract defines the API interaction surface — the contract that Swagger UI, admin HTTP clients, integration tests, and the Phase 5 frontend consume.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none |
| Preset | not applicable |
| Component library | none — backend REST API phase |
| Icon library | not applicable |
| Font | not applicable |

**Rationale:** No browser client is delivered in Phase 2. The project is `skipClient: true` in `.yo-rc.json`. The standalone `frontend/` app is a Phase 5 deliverable. Design tokens are not applicable here; the interaction contract is the REST API surface.

---

## Spacing Scale

Not applicable to this phase. Phase 2 delivers backend REST endpoints only.

Exceptions: API contract uses standard JHipster pagination — page size defaults to 20 items per list response, consistent with existing `/api/admin/users` behavior.

---

## Typography

Not applicable to this phase. No rendered HTML surfaces exist in Phase 2.

---

## Color

Not applicable to this phase.

---

## API Interaction Contract

This section replaces the browser rendering contract for backend-only phases. It defines the interaction model that consumers (admin HTTP clients, integration tests, Phase 5 frontend) must rely on.

### Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/admin/sec/roles` | GET | ROLE_ADMIN | List all merged security roles |
| `/api/admin/sec/roles` | POST | ROLE_ADMIN | Create a new merged security role |
| `/api/admin/sec/roles/{name}` | GET | ROLE_ADMIN | Get a single role by name |
| `/api/admin/sec/roles/{name}` | PUT | ROLE_ADMIN | Update display name and/or type of a role |
| `/api/admin/sec/roles/{name}` | DELETE | ROLE_ADMIN | Delete a role (cascade-deletes its permissions) |
| `/api/admin/sec/permissions` | GET | ROLE_ADMIN | List all permission rules |
| `/api/admin/sec/permissions` | POST | ROLE_ADMIN | Create a permission rule |
| `/api/admin/sec/permissions/{id}` | GET | ROLE_ADMIN | Get a single permission rule by id |
| `/api/admin/sec/permissions/{id}` | PUT | ROLE_ADMIN | Update a permission rule |
| `/api/admin/sec/permissions/{id}` | DELETE | ROLE_ADMIN | Delete a permission rule |
| `/api/admin/sec/row-policies` | GET | ROLE_ADMIN | List all row policies |
| `/api/admin/sec/row-policies` | POST | ROLE_ADMIN | Create a row policy |
| `/api/admin/sec/row-policies/{id}` | GET | ROLE_ADMIN | Get a single row policy by id |
| `/api/admin/sec/row-policies/{id}` | PUT | ROLE_ADMIN | Update a row policy |
| `/api/admin/sec/row-policies/{id}` | DELETE | ROLE_ADMIN | Delete a row policy |

Source: CONTEXT.md D-05, D-09, D-14, D-18.

### Request / Response Shape Contract

#### SecRoleDTO

```
{
  "name": "ROLE_ACCOUNTANT",          // String, @NotBlank, @Size(max=50), PK — role code
  "displayName": "Accountant",        // String, nullable, @Size(max=255) — human label
  "type": "RESOURCE"                  // String enum: RESOURCE | ROW_LEVEL, @NotNull
}
```

Source: CONTEXT.md D-02, D-03. Enum as String code (not numeric) per research discretion finding.

#### SecPermissionDTO

```
{
  "id": 1,                            // Long, read-only on create
  "authorityName": "ROLE_ACCOUNTANT", // String, @NotBlank, @Size(max=50), FK → jhi_authority.name
  "targetType": "ENTITY",             // String enum: ENTITY | ATTRIBUTE | ROW_POLICY, @NotNull
  "target": "Organization",           // String, @NotBlank, @Size(max=255) — entity/attribute/policy code
  "action": "READ",                   // String, @NotBlank, @Size(max=50) — CRUD or view/edit verb
  "effect": "ALLOW"                   // String enum: ALLOW | DENY, @NotNull
}
```

Source: CONTEXT.md D-06, D-07, D-08. FETCH_PLAN excluded per D-07.

#### SecRowPolicyDTO

```
{
  "id": 1,                            // Long, read-only on create
  "code": "org-owner-only",           // String, @NotBlank, @Size(max=100), unique
  "entityName": "Organization",       // String, @NotBlank, @Size(max=255)
  "operation": "READ",                // String, @NotBlank, @Size(max=20) — READ | UPDATE | DELETE
  "policyType": "SPECIFICATION",      // String enum: SPECIFICATION | JPQL | JAVA, @NotNull
  "expression": "owner = :currentUser" // String, @NotBlank, @Size(max=1000)
}
```

Source: CONTEXT.md D-11, D-12.

### HTTP Status Contract

| Scenario | Status | Notes |
|----------|--------|-------|
| Successful create | 201 Created | Location header + entity body; `X-jhipsterApp-alert` header via `HeaderUtil.createEntityCreationAlert` |
| Successful list | 200 OK | Array body; optional `X-Total-Count` / Link pagination headers via `PaginationUtil` |
| Successful GET single | 200 OK | Entity body |
| Entity not found | 404 Not Found | RFC 7807 problem response via `ResponseUtil.wrapOrNotFound` |
| Successful update | 200 OK | Updated entity body |
| Successful delete | 204 No Content | `X-jhipsterApp-alert` header via `HeaderUtil.createEntityDeletionAlert` |
| Validation failure | 400 Bad Request | RFC 7807 `ConstraintViolationProblem` with field errors via `ExceptionTranslator` |
| Conflict (duplicate name/code) | 400 Bad Request | RFC 7807 `BadRequestAlertException` with typed error key |
| Unauthenticated | 401 Unauthorized | `BearerTokenAuthenticationEntryPoint` handles this at filter level |
| Unauthorized (not ROLE_ADMIN) | 403 Forbidden | `BearerTokenAccessDeniedHandler` handles this at filter level |

Source: RESEARCH.md §Standard Stack; existing pattern from `UserResource`, `AuthorityResource`, `ExceptionTranslator`.

### Validation Constraints

| Field | Constraint | Reason |
|-------|-----------|--------|
| `SecRoleDTO.name` | @NotBlank, @Size(max=50), matches `[A-Z_]+` pattern | PK value; must be a valid authority code matching `jhi_authority.name` constraint |
| `SecRoleDTO.type` | @NotNull, valid enum code | Required — defaults to RESOURCE in DB but must be explicit in API to avoid silent coercion |
| `SecPermissionDTO.authorityName` | @NotBlank, @Size(max=50) | FK reference; 400 on missing role |
| `SecPermissionDTO.targetType` | @NotNull, valid enum code | Drives enforcement routing in Phase 3 |
| `SecPermissionDTO.target` | @NotBlank, @Size(max=255) | Entity/attribute/policy name |
| `SecPermissionDTO.action` | @NotBlank, @Size(max=50) | CRUD/view/edit verb |
| `SecPermissionDTO.effect` | @NotNull, valid enum code | ALLOW or DENY — determines deny-wins evaluation |
| `SecRowPolicyDTO.code` | @NotBlank, @Size(max=100), unique | Used as a stable reference key by Phase 3 |
| `SecRowPolicyDTO.entityName` | @NotBlank, @Size(max=255) | Target entity class simple name |
| `SecRowPolicyDTO.operation` | @NotBlank, @Size(max=20) | READ, UPDATE, or DELETE |
| `SecRowPolicyDTO.policyType` | @NotNull, valid enum code | SPECIFICATION / JPQL / JAVA — Phase 3 decides enforcement order |
| `SecRowPolicyDTO.expression` | @NotBlank, @Size(max=1000) | Policy expression body |

Source: CONTEXT.md D-06 to D-14; RESEARCH.md discretion items — @NotNull/@Size resolved here.

### Cascade / Guard Behavior

- Deleting a role via `DELETE /api/admin/sec/roles/{name}` must cascade-delete all `sec_permission` rows where `authority_name = {name}`. Application-level cascade guard in `SecRoleAdminResource` (not DB-level CASCADE) is the recommended approach to keep audit logging consistent with existing JHipster patterns. If DB-level ON DELETE CASCADE is used in Liquibase, the response header still fires via `HeaderUtil.createEntityDeletionAlert`.
- Deleting a permission or row policy has no downstream cascade.

Source: CONTEXT.md §Specific Ideas cascade note.

### SecurityContextBridge Override Contract

`MergedSecurityContextBridge` (registered as `@Primary`) overrides `JHipsterSecurityContextBridge` (Phase 1). The interaction contract:

- `getCurrentUserAuthorities()` returns only authority names that exist in `jhi_authority` at query time.
- Stale authorities (present in JWT but deleted from `jhi_authority`) are silently dropped — no error, no 403 at bridge level.
- Phase 3 relies on `MergedSecurityService` (ported from angapp's `SecurityService`) as the stable query interface for enforcement decisions.

Source: CONTEXT.md D-15, D-16, D-17.

---

## Copywriting Contract

These are the machine-readable alert keys and human strings for admin tooling and eventual Phase 5 UI consumption.

| Element | Copy |
|---------|------|
| Primary CTA (roles) | "Create Role" |
| Primary CTA (permissions) | "Create Permission Rule" |
| Primary CTA (row policies) | "Create Row Policy" |
| Empty state heading (roles) | "No security roles defined" |
| Empty state body (roles) | "Create the first role to start assigning permissions." |
| Empty state heading (permissions) | "No permission rules defined" |
| Empty state body (permissions) | "Add a permission rule to control what a role can do." |
| Empty state heading (row policies) | "No row policies defined" |
| Empty state body (row policies) | "Add a row policy to restrict which records a role can access." |
| Error state (validation) | "The request could not be saved. Check the highlighted fields and try again." |
| Error state (conflict — role name) | "A role with that name already exists. Use a unique role code." |
| Error state (conflict — policy code) | "A row policy with that code already exists. Use a unique code." |
| Error state (foreign key — role not found) | "The specified role does not exist. Select a valid role and try again." |
| Destructive confirmation (delete role) | "Delete role: This will permanently remove the role and all its permission rules. This cannot be undone." |
| Destructive confirmation (delete permission) | "Delete permission rule: This rule will be permanently removed." |
| Destructive confirmation (delete row policy) | "Delete row policy: This policy will be permanently removed." |

Source: Inferred from SEC-01/SEC-02/SEC-03 success criteria and standard JHipster alert key conventions. No upstream artifact specified copy — these are researcher defaults.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none | not applicable — no frontend in this phase |

No third-party registries are used. Phase 2 is a backend-only phase.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending

---

## Pre-Population Sources

| Source | Decisions Used |
|--------|---------------|
| CONTEXT.md | D-01 through D-20 — all endpoint paths, entity shapes, enum values, auth requirements |
| RESEARCH.md | Standard stack (JHipster HeaderUtil, ResponseUtil, PaginationUtil patterns); HTTP status contract; validation constraint recommendations |
| REQUIREMENTS.md | SEC-01, SEC-02, SEC-03 success criteria drove copywriting empty/error states |
| User input | 0 — all fields pre-populated from upstream artifacts |

---

*Phase: 02-security-metadata-management*
*UI-SPEC created: 2026-03-21*
