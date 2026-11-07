# AGENTS.md

Instructions for coding agents working in this repository.

## Project

A web platform where customers buy and book consultation packages and training runs, experts deliver them, and admins manage offerings and cases. We build a PoC first and refine it into an MVP.

**Status:** documentation only. No code exists yet. The first increment is planned: [01 Shop tracer bullet](_docs/specs/01-shop-tracer-bullet.md).

## Read first

| What | Where |
|---|---|
| Domain terms (use them in code) | [_docs/glossary.md](_docs/glossary.md) |
| Use cases (the functional spec) | [_docs/use-cases/README.md](_docs/use-cases/README.md) |
| The current increment: what and why | [_docs/specs/](_docs/specs/README.md) |
| The current increment: technical plan and tasks | [_docs/tasks/](_docs/tasks/README.md) |
| Why things are the way they are | [_docs/decisions.md](_docs/decisions.md) |
| Architecture | [_docs/architecture/overview.md](_docs/architecture/overview.md) |

Work arrives as tasks. Follow [working on a task](_docs/tasks/README.md#working-on-a-task) and the [definition of done](_docs/tasks/README.md#definition-of-done). Before implementing, read the task, its increment's technical plan, the use case's file in `_docs/use-cases/`, and the architecture doc for the side you touch ([backend](_docs/architecture/backend.md), [frontend](_docs/architecture/frontend.md), [security](_docs/architecture/security.md)).

## Rules

- `_docs/` is the source of truth. A behavior change updates the matching use-case doc in the same change. A changed decision updates [_docs/decisions.md](_docs/decisions.md).
- YAGNI: build the smallest thing that fully meets the use case. No speculative abstractions, configuration or features.
- Be boring, not clever.
- Name classes, tables, endpoints and components with [glossary](_docs/glossary.md) terms.
- Respect scope: check the use case's "Out of scope" section, the PoC/MVP column and the future list in [_docs/use-cases/README.md](_docs/use-cases/README.md).

## Repository layout

```
.
├─ AGENTS.md
├─ .claude/            SessionStart hook for cloud sessions
├─ _docs/              specification and architecture (source of truth)
│  ├─ specs/           increment specs: what we build now, and why
│  └─ tasks/           technical plans and tasks per increment
├─ backend/            Spring Boot, Maven           (not created yet)
├─ frontend/           React SPA, Vite
├─ docker-compose.yml  Postgres, Keycloak for local (not created yet)
└─ .github/workflows/  CI on GitHub Actions          (not created yet)
```

## Backend conventions

Details: [_docs/architecture/backend.md](_docs/architecture/backend.md).

- Root packages `api`, `application`, `domain`, `adapters.database` (plus `config`). Each has the feature sub-packages `user`, `offering`, `consultation`, `training`, `availability`, `image`. Shared value objects live in `domain.shared`.
- Each use case is one `@Service` class named `<Verb><Noun>Service` in `application.<feature>`. It has one public method, is `@Transactional`, and contains flow only.
- Business rules live in `domain` POJOs and records. `domain` has no Spring or JPA imports.
- Outgoing ports are interfaces named `<Purpose>Port` in `application.<feature>`. They are narrow and use-case specific, and implemented in `adapters.database.<feature>`. There are no incoming ports: controllers call use-case services directly.
- Controllers pass `CurrentUser` explicitly into use cases. Ownership is enforced by scoped port queries; a resource that isn't yours returns 404.
- JPA entities never leave `adapters.database`. Mappers are hand-written. No Lombok, no MapStruct. Value objects, commands and DTOs are records.
- Always get the current time from the injected `java.time.Clock`.
- Derived states (consumed appointment, completed case or enrollment, open credits) are computed, never stored.
- Contended writes take row locks first, in the order case → expert → run.
- Domain exceptions are mapped centrally to `ProblemDetail`.

## Frontend conventions

Details: [_docs/architecture/frontend.md](_docs/architecture/frontend.md).

- Stack: Vite, React, TypeScript (strict), Tailwind, shadcn/ui, React Router, TanStack Query, React Hook Form, date-fns with `@date-fns/tz`.
- Feature folders under `frontend/src/features/` mirror the backend features.
- API types are generated from the committed `backend/openapi.json`. Never hand-write API DTOs.
- Show all dates and times in `Europe/Zurich`.

## Testing

Details: [_docs/architecture/backend.md#testing](_docs/architecture/backend.md#testing).

- Domain: extensive unit tests.
- REST: one happy-path integration test per route, against Testcontainers Postgres.
- Authorization (mandatory): a role matrix over all routes, plus ownership tests that use the two seeded users per role.
- Concurrency: one test per locked race (expert slot, case credits, run seats).
- Use-case service tests only where they add real value.
- The ArchUnit layer rules and the OpenAPI drift test must pass.

## Cloud sessions

In Claude Code cloud sessions, the environment's setup script installs JDK 25, and the SessionStart hook ([.claude/hooks/session-start.sh](.claude/hooks/session-start.sh)) starts Docker and sets `JAVA_HOME` to Java 25. Locally the hook does nothing.

## Commands

### Frontend

Run in `frontend/`. Node 22 (`.nvmrc`) and npm.

| Command | Does |
|---|---|
| `npm ci` | install the locked dependencies |
| `npm run dev` | Vite on http://localhost:5173, proxies `/api` to http://localhost:8080 |
| `npm run lint` | ESLint |
| `npm run format` | Prettier, writes changes |
| `npm run format:check` | Prettier, fails on unformatted files |
| `npm test` | Vitest, one run |
| `npm run build` | type-check with `tsc -b`, then build into `dist/` |
| `npm run preview` | serve `dist/` locally |
