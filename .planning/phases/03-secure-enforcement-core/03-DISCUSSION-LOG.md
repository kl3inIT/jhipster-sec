# Phase 3: Secure Enforcement Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `03-CONTEXT.md`; this log preserves the alternatives considered.

**Date:** 2026-03-21
**Phase:** 03-secure-enforcement-core
**Areas discussed:** Data access abstraction, enforcement points, permission evaluation flow, row policy execution strategy, attribute filtering strategy, secured entity catalog

---

## Data access abstraction

| Option | Description | Selected |
|--------|-------------|----------|
| Central secured data managers | Introduce `SecureDataManager` for protected access and `UnconstrainedDataManager` as the explicit bypass path | x |
| Secure manager only | One manager handles both protected and internal access through flags/conventions | |
| Service/repository local enforcement | Keep repository calls in each service and enforce security ad hoc | |

**User's choice:** Use a central `SecureDataManager`, add `UnconstrainedDataManager`, and make `loadByQuery` with parameters the standard secured query entry point.
**Notes:** The user explicitly wanted angapp / Jmix philosophy preserved and rejected a simplified JHipster-only approach.

---

## Enforcement points

| Option | Description | Selected |
|--------|-------------|----------|
| Central manager enforcement | Reads and writes go through the central data-manager pipeline | x |
| Mixed enforcement | Reads use the central manager but writes still enforce in services/controllers | |
| Endpoint-first enforcement | Controllers own most of the checks and repository access remains direct | |

**User's choice:** Keep all protected read/write enforcement in the central data-manager path and reserve bypasses for `UnconstrainedDataManager` only.
**Notes:** This locks the enforcement order and avoids drift between API endpoints and internal callers.

---

## Permission evaluation flow

| Option | Description | Selected |
|--------|-------------|----------|
| Access-context pipeline | Keep `DENY`-wins and evaluate CRUD, attribute, row-level, and secured fetch-plan usage through a central context-based model | x |
| Direct evaluator calls only | Services call entity/attribute evaluators directly without a central context pipeline | |
| Reintroduce DB fetch-plan permissions | Add back DB-stored fetch-plan APPLY permission targets in Phase 3 | |

**User's choice:** Keep the context-based Jmix-aligned flow, preserve `DENY`-wins, and do not reintroduce DB fetch-plan permissions.
**Notes:** Fetch-plan control stays catalog/config driven, with attribute filtering still applied to serialized output.

---

## Row policy execution strategy

| Option | Description | Selected |
|--------|-------------|----------|
| `SPECIFICATION` only | Support specification-style row policies and fail unsupported types | |
| `SPECIFICATION` + controlled `JPQL` | Support DB predicates plus managed JPQL `WHERE` fragments with built-in security tokens only | x |
| Free-form `JPQL` / `JAVA` | Execute full stored queries or arbitrary Java row-policy handlers | |

**User's choice:** Support `SPECIFICATION` and controlled JPQL row policies.
**Notes:** JPQL policies are `WHERE`-style managed fragments only, runtime tokens are limited to built-in security-context values, and unsafe runtime application must fail closed with security-style access denial.

---

## Attribute filtering strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Omit on read, reject on write | Silently omit unreadable attributes from responses, keep `id`, and reject forbidden edits during merge | x |
| Omit on read, strip on write | Hide unreadable attributes and silently drop forbidden write fields | |
| Hard fail for both | Fail both reads and writes whenever any forbidden attribute is involved | |

**User's choice:** Omit unreadable attributes on read, keep `id` when requested, recurse through reference filtering, and reject unauthorized writes.
**Notes:** This keeps the donor serializer/merge direction while staying safer on writes.

---

## Secured entity catalog

| Option | Description | Selected |
|--------|-------------|----------|
| Scanner/metamodel -> controlled catalog | Derive metadata from JPA scanning/metamodel, then filter through an explicit allowlisted secured catalog | x |
| Code-only manual catalog | Define every entity and attribute manually in code without metamodel assistance | |
| Open scanner | Expose every scanned JPA entity automatically as a permission target | |

**User's choice:** Use the entity-scanner / JPA metamodel approach behind a controlled secured catalog.
**Notes:** Optional YAML may add labels, grouping, ordering, and display hints, but must not define new security targets outside the code-defined catalog.

---

## the agent's Discretion

- Exact type and package names for the secured query object and manager interfaces
- The concrete set of built-in security-context tokens allowed in controlled JPQL row policies
- The concrete configuration-loading seam for optional catalog YAML metadata

## Deferred Ideas

- Database-stored fetch-plan definitions or DB-managed fetch-plan APPLY permissions
- Automatic exposure of every scanned entity as a permission target
- Request-parameter-driven row-policy language
- `JAVA` row-policy execution
- Protected sample-entity proof work, which belongs to Phase 4
