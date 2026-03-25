# Phase 7: Enterprise Navigation Shell - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `07-CONTEXT.md` - this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 07-enterprise-navigation-shell
**Areas discussed:** Navigation ownership, Denied-route behavior, Enterprise workspace pattern, Visibility threshold for menu entries

---

## Navigation Ownership

### Q1. What should the backend own for the shell menu?

| Option | Description | Selected |
|--------|-------------|----------|
| Visibility only | Client keeps the current section tree and translated labels/icons; backend only says which entries are allowed | |
| Full navigation tree | Backend returns the actual hierarchy and ordering; client mainly renders it | |
| Hybrid | Backend owns ids, targets, and visibility; client maps ids to labels/icons | |
| Clarified selection | Frontend keeps the canonical `Home / Entities / Security` tree and stable ids; backend returns allowed node ids only | x |

**User's choice:** Fixed sections plus backend visibility only.
**Notes:** The user's first free-text answer mixed a fixed frontend tree with a hierarchical backend tree and also requested a multi-app authorization table plus a menu-permission management UI. Follow-up clarification locked the contract as frontend-owned tree plus backend-allowed node ids only. Multi-app backend support with `app_name` remained desired. Menu-permission management UI was marked deferred as scope expansion beyond Phase 7.

### Q2. What happens if an entire section ends up empty?

| Option | Description | Selected |
|--------|-------------|----------|
| Hide the section completely | Remove sections that have no visible children | x |
| Show the section header only | Keep empty sections visible | |
| Show a disabled placeholder | Keep the section with explanatory empty UI | |

**User's choice:** Hide the section completely.
**Notes:** This keeps the fixed shell clean for mixed-access users.

### Q3. Where do stable menu node ids live?

| Option | Description | Selected |
|--------|-------------|----------|
| Frontend is canonical | Client owns the stable id catalog and backend references those ids | x |
| Backend is canonical | Database defines ids and frontend syncs to it | |
| Shared contract file | One shared contract defines ids for both sides | |

**User's choice:** Frontend is canonical.
**Notes:** This aligns with the fixed-shell decision and preserves the Phase 6 stable-id rule.

### Q4. How broad should the visibility model be in Phase 7?

| Option | Description | Selected |
|--------|-------------|----------|
| This app only, schema-ready for multi-app | Use `app_name` now but only serve this app in Phase 7 | |
| Multi-app from day one | API and authorization semantics are immediately app-scoped | x |
| No app split yet | Single app now, add app split later | |

**User's choice:** Multi-app from day one.
**Notes:** The user explicitly wants the backend authorization model to support multiple frontend apps with an app identifier such as `app_name`.

---

## Denied-Route Behavior

### Q1. What should happen on an unauthorized deep link?

| Option | Description | Selected |
|--------|-------------|----------|
| Go to `/accessdenied` | Explicit denial page | x |
| Redirect to first allowed destination | Smooth fallback to another area | |
| Go to a default workspace page | Always land on home/dashboard | |

**User's choice:** Go to `/accessdenied`.
**Notes:** The user rejected silent rerouting and preferred explicit denial.

### Q2. Should the denial message mention what was blocked?

| Option | Description | Selected |
|--------|-------------|----------|
| Generic message only | No destination details | |
| Mention the blocked destination | Explain which area the user tried to open | x |
| Mention destination plus required permission context | Include permission wording too | |

**User's choice:** Mention the blocked destination.
**Notes:** This keeps the denial helpful without surfacing low-level permission vocabulary.

### Q3. If a leaf is denied, should the parent section remain?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep the parent section if any sibling remains accessible | Hide only the denied leaf | x |
| Hide the whole section on any denied child | Collapse the section aggressively | |
| Keep the section and show denied leaves disabled | Expose unavailable leaves visually | |

**User's choice:** Keep the parent section if any sibling remains accessible.
**Notes:** This supports a stable sectioned shell for mixed-access users.

### Q4. What recovery action should `/accessdenied` offer?

| Option | Description | Selected |
|--------|-------------|----------|
| Show a clear "Back to allowed area" action | Primary recovery path back to a safe page | x |
| Back button only | Minimal UI | |
| Show related allowed links | Suggest sibling destinations directly | |

**User's choice:** Show a clear "Back to allowed area" action.
**Notes:** Exact fallback-target logic remains discretionary.

---

## Enterprise Workspace Pattern

### Q1. How strong should the master-detail shell shift be?

| Option | Description | Selected |
|--------|-------------|----------|
| List-first workspace | Section routes land on list/index pages with drill-in detail/edit | x |
| Balanced page-to-page | Improve consistency without much structural change | |
| Persistent split view | Keep list and detail visible side by side | |

**User's choice:** List-first workspace.
**Notes:** The user wants a stronger enterprise/Jmix feel without a full split-pane shell rewrite.

### Q2. What should be preserved when returning from detail/edit to a list?

| Option | Description | Selected |
|--------|-------------|----------|
| Preserve list context | Keep filters, pagination, sort, and section context | x |
| Preserve only the section | Reset the list itself | |
| Always reset to defaults | Start clean on every return | |

**User's choice:** Preserve list context.
**Notes:** This is the strongest enterprise-workflow continuity rule captured in the discussion.

### Q3. How should the shell show where the user is?

| Option | Description | Selected |
|--------|-------------|----------|
| Breadcrumb plus active section highlighting | Strong orientation with limited shell change | x |
| Active menu highlighting only | Minimal orientation | |
| Section header with local tabs/subnav | Larger shell expansion | |

**User's choice:** Breadcrumb plus active section highlighting.
**Notes:** This supports deep routes without requiring a larger subnavigation system in Phase 7.

### Q4. Should detail/edit flows remain route-based?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep route-based pages | Continue using navigable detail/edit routes | x |
| Use dialogs/drawers when practical | Shift toward quick-edit surfaces | |
| Mix freely per screen | Decide ad hoc | |

**User's choice:** Keep route-based pages.
**Notes:** This aligns with the current Angular route structure already present in `frontend/`.

---

## Visibility Threshold For Menu Entries

### Q1. When should an entity leaf appear in the shell?

| Option | Description | Selected |
|--------|-------------|----------|
| Only when the route is reachable at all | Hide leaves that cannot be opened | x |
| Show when any action exists | Visible even without list reachability | |
| Always show entity entries | Current behavior | |

**User's choice:** Only when the route is reachable at all.
**Notes:** Follow-up discussion clarified that reachability for entities comes from backend navigation grant, not necessarily entity `READ`.

### Q2. What counts as "route reachable" for secured entities?

| Option | Description | Selected |
|--------|-------------|----------|
| `READ` access to the entity list | Show only if the list can load | |
| Any CRUD capability on the entity | Show if any operation is allowed | |
| Backend menu grant only, with page-level denial if data read is not allowed | Visible route may still show denied state | x |

**User's choice:** Allow navigation from menu grant even when entity `READ` is absent; the page then shows denial instead of loading data.
**Notes:** This deliberately decouples shell visibility from entity list-read capability.

### Q3. What should the entity page show when menu-visible but not readable?

| Option | Description | Selected |
|--------|-------------|----------|
| Full-page access-denied state inside the shell | Do not load data; explain the area is denied | x |
| Empty table with inline error banner | Keep list structure visible | |
| Immediate redirect to `/accessdenied` | Treat as hard route denial | |

**User's choice:** Full-page access-denied state inside the shell.
**Notes:** The user explicitly wants the page to stay inside the shell while refusing data load.

### Q4. Should admin entries use the same rule?

| Option | Description | Selected |
|--------|-------------|----------|
| Stricter for admin | Show admin leaves only when truly reachable | x |
| Same as entities | Allow visible-but-denied admin leaves | |
| Mixed by leaf | Decide leaf by leaf | |

**User's choice:** Stricter for admin.
**Notes:** Admin areas should not appear as visible-but-denied destinations.

---

## the agent's Discretion

- Exact stable node-id naming convention
- Exact safe fallback-target algorithm for the access-denied recovery action
- Exact breadcrumb styling and shell chrome details within the existing layout
- Exact backend schema and service shape beyond the locked multi-app `app_name` requirement

## Deferred Ideas

- Menu permission management UI - out of Phase 7 scope and better treated as a later admin capability
