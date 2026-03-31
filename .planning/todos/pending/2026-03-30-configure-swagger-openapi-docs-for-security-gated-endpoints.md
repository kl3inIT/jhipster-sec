---
created: 2026-03-30T08:20:29.560Z
title: Configure Swagger OpenAPI docs for security-gated endpoints
area: api
files:
  - src/main/java/com/vn/core/web/rest/
  - src/main/java/com/vn/core/security/
---

## Problem

Standard JHipster Swagger/OpenAPI setup documents every endpoint uniformly with no awareness of the security pipeline. This project's endpoints behave differently depending on the caller's role and permissions:

- The response schema varies per caller — `SecureEntitySerializerImpl` strips attributes the caller has no `VIEW` permission on, so the same endpoint returns different field sets for different roles.
- Fetch-plan codes (`organization-list`, etc.) shape what relations are included; the standard `@Schema` annotations do not capture this.
- Security-specific endpoints (`/api/security/entity-capabilities`, `/api/security/menu-permissions`) have no OpenAPI descriptions at all.
- JWT bearer auth is already configured by JHipster, but there is no indication in the docs which endpoints enforce `@SecuredEntity` vs. plain `@PreAuthorize`.

Without accurate API docs, frontend developers and API consumers cannot know what to expect from a response without reading source code.

## Solution

Decide on a documentation strategy that is both accurate and maintainable:

1. **Audit current Swagger config** — check what JHipster's `OpenApiAutoConfiguration` already generates and what is missing or misleading.
2. **Annotate security endpoints** — add `@Operation`, `@ApiResponse`, and `@SecurityRequirement` to `/api/security/**` resources.
3. **Handle variable response schemas** — options to evaluate:
   - Document the full schema (all possible fields) and note that fields may be omitted based on caller permissions.
   - Use `@ApiResponse` variants per role (complex, may not scale).
   - Add a custom Swagger description block explaining the attribute-permission filtering model.
4. **Document fetch-plan codes** — add `@Parameter` descriptions for query params that accept fetch-plan codes, listing valid values from `fetch-plans.yml`.
5. **Mark `@SecuredEntity` endpoints** — consider a custom Swagger extension tag (e.g. `x-secured-entity: true`) to distinguish security-pipeline endpoints from plain ones.
6. **Decide scope** — full OpenAPI 3.1 compliance vs. developer-friendly reference only; align with team expectations before investing in annotations.
