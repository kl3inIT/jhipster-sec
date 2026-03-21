# Phase 4: Protected Entity Proof - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-21
**Phase:** 04-protected-entity-proof
**Areas discussed:** Secured catalog discovery

---

## Secured Catalog Discovery

| Option | Description | Selected |
|--------|-------------|----------|
| Manual registrations | Build the Phase 4 catalog by explicitly listing each proof entity class in code | |
| JPA metamodel scan + secured filter | Discover managed entities from `EntityManager.getMetamodel().getEntities()`, then filter through allowlist or `@SecuredEntity` before enriching `SecuredEntityEntry` metadata | ✓ |
| Raw classpath `@Entity` scan | Use classpath scanning or reflection-first discovery independent of JPA runtime state | |

**User's choice:** JPA metamodel scan with an explicit secured-entity filter. The catalog must scan runtime-managed entities, then allowlist or mark the approved proof entities. It must not hand-build the catalog as a fixed class list, and it must not use raw classpath scanning.

**Notes:** Keep the Phase 3 fail-closed posture. Metamodel discovery is only the source for candidate entities and attributes; security participation still belongs to `SecuredEntityCatalog`, which enriches approved entities with logical code, allowed operations, fetch-plan codes, and `jpqlAllowed` metadata.

---

## Deferred Ideas

- Generic admin catalog endpoint such as `/api/admin/sec/catalog/entities` was mentioned as a useful exposure seam, but it is outside the locked Phase 4 proof boundary and should be handled in a later admin/frontend phase if needed.
