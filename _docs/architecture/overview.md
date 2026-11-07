# Architecture overview

## System context

```
                 ┌──────────────────────────── one container ───────────────────────────┐
  Browser        │  Spring Boot                                                         │
 ┌─────────┐     │  ├─ serves the built SPA (static files, fallback to index.html)      │
 │  React  │◄───►│  ├─ /api/**  REST + JSON                                             │
 │  SPA    │cookie  └─ OIDC client (BFF), session                                       │
 └─────────┘     └───────────┬───────────────────────────────┬──────────────────────────┘
      │                      │ JDBC                          │ OIDC (MVP)
      │ opens link           ▼                               ▼
      ▼                ┌────────────┐                 ┌──────────────┐
 ┌─────────┐           │ PostgreSQL │                 │ IdP (OIDC)   │
 │ MS Teams│           └────────────┘                 │ Keycloak lo- │
 └─────────┘                                          │ cally; prod  │
 (external, link only)                                │ open         │
                                                      └──────────────┘
```

- **Same origin.** The SPA and the API are served from one origin, so cookies and CSRF work without CORS.
- **Teams** is only linked to. There's no integration.
- **No other external systems** in the PoC. Payment arrives in the MVP ([decisions](../decisions.md#open-decisions-before-mvp)).

## Building blocks

| Block | Tech | Doc |
|---|---|---|
| Frontend | React SPA (Vite, TypeScript, Tailwind) | [frontend.md](frontend.md) |
| Backend | Spring Boot modular monolith, hexagonal, layer-first packages | [backend.md](backend.md) |
| Security | BFF session auth, roles in DB, ownership in use cases | [security.md](security.md) |
| Database | PostgreSQL, one schema, Flyway | [backend.md](backend.md#persistence) |

## Repository layout

```
.
├─ AGENTS.md                 agent instructions
├─ .github/workflows/        CI (GitHub Actions)
├─ _docs/                    this documentation (source of truth)
│  ├─ specs/                 increment specs: what we build now, and why
│  └─ tasks/                 technical plans and tasks per increment
├─ backend/                  Maven project, Spring Boot
│  ├─ src/main/java/...      packages: see backend.md
│  ├─ src/main/resources/db/migration   Flyway schema
│  ├─ src/main/resources/db/seed        Flyway seed data (poc, dev only)
│  └─ openapi.json           generated and committed API contract
├─ frontend/                 Vite project, React
└─ docker-compose.yml        local Postgres and Keycloak
```

## Runtime and profiles

| Profile | Login | Seed data | Used for |
|---|---|---|---|
| `poc` | user picker, `POST /api/poc/login` | yes | PoC, local development |
| `dev` | OIDC against local Keycloak (realm import with the seed users) | yes | MVP local development |
| *(none)* | OIDC against the production IdP | no | production |

**Local development**

```
docker compose up                 → Postgres :5432, Keycloak :8180
backend (Spring Boot, poc|dev)    → :8080
frontend (Vite dev server)        → :5173, proxies /api to :8080
```

**Production:** the Maven build bundles the frontend's `dist/` into the Spring Boot jar, giving one container image. It needs Postgres and an OIDC IdP. Hosting is an open decision. CI runs on GitHub Actions (D-30).

## Time

- The business time zone is `Europe/Zurich` (D-20).
- Points in time (appointments, run slots, purchases) are stored as `timestamptz` and handled as `Instant`.
- Local times (template time slots, availability ranges) are stored as `time` and turned into instants in Zurich time. This keeps them correct across daylight-saving changes.
- The current time always comes from an injected `java.time.Clock`.

## Quality goals

1. **Correct business rules** (credits, deadlines, seats, slots): unit tests in the domain and concurrency tests on the locks.
2. **Nobody sees what isn't theirs**: authorization tests on every route.
3. **Simplicity**: YAGNI, boring technology, and docs that match the code.
