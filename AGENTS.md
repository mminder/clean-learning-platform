# AGENTS.md

Instructions for coding agents working in this repository.

## Project

A web platform where customers buy and book consultation packages and training runs, experts deliver them, and admins manage offerings and cases. We build a PoC first and refine it into an MVP.

**Status:** increment [01 Shop tracer bullet](_docs/specs/01-shop-tracer-bullet.md) is complete. The shop list and the consultation offering detail work end to end: the seeded Career Coaching offering travels from Postgres through every backend layer and the generated API client to the React shop, served by the Vite dev server or by the single jar. That path ([task 06](_docs/tasks/01-shop-tracer-bullet/06-shop-list.md)) is the pattern to copy for a new use case. Error handling follows [task 07](_docs/tasks/01-shop-tracer-bullet/07-consultation-offering-detail.md): `ApiExceptionHandler` on the backend and `ApiError` on the frontend.

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
├─ backend/            Spring Boot, Maven
├─ frontend/           React SPA, Vite
├─ docker-compose.yml  Postgres for local development
└─ .github/workflows/  CI on GitHub Actions
```

## Backend conventions

Details: [_docs/architecture/backend.md](_docs/architecture/backend.md).

- Root packages `api`, `application`, `domain`, `adapters.database` (plus `config`). Each has the feature sub-packages `user`, `offering`, `consultation`, `training`, `availability`, `image`; `api` also has `shop`, the public catalog endpoints. Shared value objects live in `domain.shared`.
- Each use case is one `@Service` class named `<Verb><Noun>UseCase` in `application.<feature>`. It has one public method named `invoke`, which is `@Transactional` and contains flow only.
- Business rules live in `domain` POJOs and records. `domain` has no Spring or JPA imports.
- Outgoing ports are interfaces named `<Purpose>Port` in `application.<feature>`. They are narrow and use-case specific, and implemented in `adapters.database.<feature>`. There are no incoming ports: controllers call use cases directly.
- Controllers pass `CurrentUser` explicitly into use cases. Ownership is enforced by scoped port queries; a resource that isn't yours returns 404.
- JPA entities never leave `adapters.database`. In `adapters.database.<feature>`, the adapters and Spring Data repositories sit at the top, the entities in `entity`, and the `*Row` records that queries select into in `row`. Mappers are hand-written. No Lombok, no MapStruct. Value objects, commands and DTOs are records.
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
- Use-case tests only where they add real value.
- The ArchUnit layer rules and the OpenAPI drift test must pass.

## Cloud sessions

In Claude Code cloud sessions, the environment's setup script installs JDK 25, and the SessionStart hook ([.claude/hooks/session-start.sh](.claude/hooks/session-start.sh)) starts Docker and sets `JAVA_HOME` to Java 25. Locally the hook does nothing.

## Commands

CI (GitHub Actions, `.github/workflows/ci.yml`) runs the same commands on every pull request and push to `main`.

### Backend

| Where | Command | Does |
|---|---|---|
| root | `docker compose up -d` | Postgres 18 on :5432 (`docker compose down -v` stops it and drops the data) |
| `backend/` | `./mvnw verify` | all backend tests, including `LayerRulesTest` (needs Docker for Testcontainers) |
| `backend/` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=poc` | API on :8080 against the docker-compose Postgres, Flyway migrates and seeds on start |
| `backend/` | `./mvnw -Pfrontend package` | one jar with the SPA: runs `npm ci` and `npm run build` in `frontend/` (installed Node and npm), copies `dist/` into `static/`. Without `-Pfrontend` the backend build doesn't touch the frontend |
| `backend/` | `java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc` | the jar serves the SPA and the API on :8080 against the docker-compose Postgres; client routes like `/offerings/<id>` get `index.html` |
| `backend/` | `./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true` | regenerate the committed `openapi.json` after an API change (`./mvnw verify` fails while it's stale); then run `npm run generate:api` in `frontend/` |

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
| `npm run generate:api` | generate `src/api/schema.d.ts` from `../backend/openapi.json`; commit both together |
| `npm run check:api` | fail if `src/api/schema.d.ts` doesn't match `../backend/openapi.json` |
| `npx shadcn@latest add <component>` | add a shadcn/ui component to `src/components/ui/`; then `npm run format` ([frontend.md](_docs/architecture/frontend.md#structure)) |
