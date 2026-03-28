# Repository Guidelines

This file provides durable instructions for coding agents working in this repository.
It is intended for tools such as Codex, Claude Code, and similar agentic programming systems.

If a more specific `AGENTS.md` exists in a subdirectory, follow the closer file for work in that scope.
If explicit user instructions conflict with this file, explicit user instructions win.

---

## 1. Project structure

This is a full-stack application with:

- `backend/` → Java Spring Boot service
- `frontend/` → Vue + Vite application

General expectations:

- Backend owns domain logic, persistence, authentication, validation, cache coordination, and APIs.
- Frontend owns UI, routing, state management, and API integration.
- Avoid leaking frontend concerns into backend code.
- Avoid putting persistence or infrastructure logic into frontend code.
- Keep responsibilities separated by layer.

---

## 2. What to optimize for

- Make the smallest correct change that fully solves the task.
- Prefer consistency with existing code over introducing new patterns.
- Keep changes easy to review.
- Do not fix unrelated issues unless they block the requested task.
- Prefer explicit, testable behavior over implicit magic.
- Before finishing, verify the result with the most relevant checks.

---

## 3. Source of truth

When implementing or modifying behavior, follow this priority:

1. Explicit user request
2. Closer directory-level `AGENTS.md`
3. Existing code, tests, and configuration
4. `README.md`, `docs/`, API contracts, and generated types
5. General framework conventions

Do not invent architecture or conventions that are not already present in the repository.

---

## 4. Working style

- Inspect relevant files before editing.
- Reuse existing service, controller, mapper, DTO, entity, composable, store, and component patterns.
- Avoid introducing new dependencies unless clearly necessary.
- Avoid broad refactors unless the task explicitly requires them.
- Avoid formatting-only diffs mixed with behavior changes.
- Keep naming aligned with surrounding code.

For multi-file changes:

- update all directly affected interfaces;
- update related tests when present;
- update docs if user-facing or integration-facing behavior changes.

---

## 5. Search and file inspection

Preferred tools for repository work:

- Search text: `rg`
- Find files: `fd` or `rg --files`
- Inspect JSON: `jq`
- Read files in focused chunks, not giant dumps

Before editing a file, also inspect nearby:

- tests
- DTOs / request / response models
- controllers / services / mappers / repositories
- interceptor / config / utils
- Vue components / stores / route definitions / API wrappers
- docs or API contract files

---

## 6. Backend rules (Spring Boot)

Scope: anything under `backend/`

### 6.1 Architecture

Prefer the repository’s existing Spring Boot layering:

- controller → service → mapper / persistence
- DTO / request / response objects separated from persistence entities where appropriate
- validation at request boundaries
- business logic in services, not controllers
- SQL / query concerns in mapper layer
- shared infrastructure in `config/`, `interceptor/`, or `utils/`

Do not introduce new architectural styles unless the repository already uses them.

### 6.2 Controllers

- Keep controllers thin.
- Controllers should handle routing, request parsing, auth boundary entry, validation entry, and response shaping.
- Do not place complex business logic in controllers.
- Use existing response conventions consistently.
- Preserve backward compatibility for public APIs unless the task requires a contract change.

### 6.3 Services

- Put business rules in services.
- Prefer extending existing services over duplicating logic.
- Keep transaction boundaries explicit and narrow.
- If a method changes business behavior, update or add service-level tests when present.
- For high-concurrency logic, preserve current locking, idempotency, and cache consistency behavior unless explicitly changing it.

### 6.4 Persistence and MyBatis Plus

- Reuse existing MyBatis Plus patterns, mapper style, and query style.
- Do not introduce inefficient query behavior without reason.
- Be careful with pagination semantics, batch updates, and conditional queries.
- If SQL is defined in XML, keep mapper interface and XML aligned.
- Do not move simple query logic into handwritten SQL unless necessary.
- If schema or entity changes are required, update the repository’s existing migration or SQL script strategy consistently.

### 6.5 Redis, cache, and concurrency

- Preserve existing Redis key patterns and expiration strategy unless the task requires change.
- Be careful with cache penetration, cache breakdown, and cache consistency logic.
- For distributed lock or Redisson logic, do not weaken safety guarantees.
- For seckill / voucher / stock logic, preserve atomicity and duplicate-order protection.
- If changing Lua script behavior, also inspect all Java call sites and related order / stock code.

### 6.6 Validation and error handling

- Validate external inputs at the boundary.
- Reuse existing exception and error response patterns.
- Do not expose internal stack traces, SQL details, or infrastructure details in API responses.
- Keep error messages and response shape consistent with existing endpoints.

### 6.7 Security and authentication

- Do not weaken authentication, authorization, token parsing, or input validation.
- Preserve existing interceptor behavior and request header conventions.
- Do not hardcode credentials, tokens, or secrets.
- Do not modify security-sensitive configuration unless the task clearly requires it.

### 6.8 API changes

If changing an API:

- update controller, DTOs, service logic, and persistence code together when needed;
- update tests when present;
- update API docs or contract files if present;
- update frontend integration code in the same task when required.

---

## 7. Frontend rules (Vue + Vite)

Scope: anything under `frontend/`

### 7.1 General

- Follow the existing component style and project conventions already used nearby.
- Do not introduce a new state management or request pattern if one already exists.
- Reuse existing UI components, utilities, and request wrappers before creating new ones.
- Keep business rules out of presentation-only components when possible.

### 7.2 Components

- Keep components focused and reasonably small.
- Avoid deeply nested inline logic in templates.
- Prefer computed state over duplicating derived values.
- Keep props, emits, and local state explicit.
- Reuse existing page and component structure where possible.

### 7.3 Routing and pages

- Follow existing route conventions and navigation behavior.
- If adding a page, update routes and entry points consistently.
- Do not break deep links or existing route names unless the task requires it.

### 7.4 API integration

- Keep API calling code centralized according to repo conventions.
- Reuse existing auth header handling, token usage, interceptors, and error handling.
- If backend response contracts change, update frontend consumers in the same task.
- Do not silently swallow request failures.

### 7.5 UX consistency

- Reuse existing loading, empty, error, and success states.
- Keep form validation and feedback consistent with nearby pages.
- If changing user-visible behavior, update related text, docs, or examples where appropriate.

---

## 8. Full-stack coordination rules

For changes that touch both `backend/` and `frontend/`:

- keep API contract changes synchronized;
- update DTOs / response models and frontend consumers together;
- verify auth, validation, and error handling across both sides;
- avoid shipping backend contract changes without updating the frontend usage.

When adding new functionality end-to-end, typical order is:

1. backend model / DTO / API
2. backend tests if present
3. frontend API integration
4. frontend UI wiring
5. end-to-end verification

---

## 9. Project-specific conventions

These rules reflect current repository behavior and should be preserved unless the task explicitly changes them:

- Backend is a Spring Boot application under `backend/`.
- Persistence uses MyBatis Plus and mapper-based access patterns.
- Redis is used for caching and concurrency-sensitive flows.
- Common API response structure follows:
  - `success`
  - `errorMsg`
  - `data`
  - `total`
- Login token is passed by request header:
  - `authorization: <token>`
- There is no unified `/api` prefix by default unless existing code in a specific area defines one.
- Time fields commonly use `LocalDateTime`.
- Some endpoints may return `null` for empty results instead of empty arrays; preserve existing behavior unless explicitly changing the API contract.
- Image fields such as shop or blog images may use comma-separated strings instead of arrays; preserve existing contract unless explicitly changing it.
- Do not silently convert existing response contracts without updating both backend and frontend.

---

## 10. Testing and validation

Run the smallest useful validation first, then widen only if needed.

### 10.1 Backend minimum validation

For backend changes, prefer this order:

1. targeted unit or integration test for changed service/controller if present
2. related module tests if shared code changed
3. broader backend test suite only when needed

Typical backend commands, depending on repo setup:

- `cd backend && mvn test`
- `cd backend && mvn -Dtest=ClassName test`
- `cd backend && ./mvnw test`

### 10.2 Frontend minimum validation

For frontend changes, prefer this order:

1. targeted test if the repo has frontend tests
2. build or type-related validation
3. broader frontend checks only when needed

Typical frontend commands, depending on repo setup:

- `cd frontend && npm run build`
- `cd frontend && npm run test`
- `cd frontend && pnpm build`

### 10.3 Full-stack checks

For API contract or cross-stack behavior changes, verify as many as are available:

- backend tests pass;
- frontend build / lint / tests pass if configured;
- contract usage is aligned on both sides.

Do not claim success without running relevant checks when tooling is available.

---

## 11. Editing rules

- Preserve public APIs unless changing them is required.
- Do not edit generated files by hand if the repo marks them as generated.
- If generated artifacts must change, regenerate them using the repo’s standard process.
- Do not add TODOs, placeholder implementations, or dead code unless explicitly requested.
- Do not add comments that merely restate the code.
- Add comments only for non-obvious intent, constraints, or edge-case reasoning.

---

## 12. Database and scripts

If the task involves schema or SQL changes:

- follow the repository’s existing SQL and script conventions;
- update affected entity, DTO, mapper, service, and tests together when needed;
- be careful with destructive or data-loss-prone changes.

Do not perform destructive data operations unless explicitly required.

---

## 13. Config and environments

- Do not commit secrets, private keys, tokens, or credentials.
- Do not modify deployment, Docker, CI, or production configuration unless the task explicitly requires it.
- Keep local development config patterns consistent with the existing repository.
- Prefer environment variables and existing config abstractions over hardcoded values.

---

## 14. Performance and reliability

- Avoid unnecessary allocations, duplicate requests, and repeated database queries.
- Be careful with pagination, sorting, filtering, and lazy-loading behavior.
- Preserve existing performance patterns for hot paths such as cache lookup, voucher ordering, and shop query flows.
- Do not introduce speculative optimization unless a real bottleneck is involved.

---

## 15. Security and safety boundaries

- Do not remove or weaken auth, permission checks, interceptors, validation, or sanitization without explicit instruction.
- Do not expose internal error details.
- Do not change secrets, credentials, production URLs, certificates, or infrastructure settings unless clearly required.
- Ask before making irreversible or high-risk changes outside normal code edits.

---

## 16. Git and commit message rules

Use Conventional Commits for all commit messages.

Format:

```text
<type>(<scope>): <subject>
```
