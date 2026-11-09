# Clean Learning Platform

A web platform where customers buy and book consultations and training courses, experts deliver them, and admins manage the offerings. It's being built as a PoC first, then refined into an MVP.

**Status:** the third increment, [03 Buy a consultation package](_docs/specs/03-buy-consultation-package.md), is complete, after [01 Shop tracer bullet](_docs/specs/01-shop-tracer-bullet.md) and [02 Manage consultation offerings](_docs/specs/02-manage-consultation-offerings.md). A visitor logs in as one of the seeded users; an admin creates and edits consultation offerings with their packages, eligible experts and image, and publishes, unpublishes, archives and deletes them; the shop follows at once. A customer buys a package from the shop, gets a case with its purchase terms, and sees it on My purchases; an offering with purchases can be archived, but not unpublished or deleted. It's served by the Vite dev server or by the single jar. No next increment is planned yet.

## Run locally

You need Docker, JDK 25 and Node 22 with npm (`frontend/.nvmrc`).

### Development mode

1. Start Postgres 18 on :5432, in the repository root:

   ```bash
   docker compose up -d
   ```

2. Start the API on :8080, in `backend/`. Flyway creates the schema and loads the seed data on start. The `poc` profile is required: it holds the database settings and the PoC login.

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=poc
   ```

3. Start the SPA on http://localhost:5173, in `frontend/`. Vite proxies `/api` to the API on :8080.

   ```bash
   npm ci
   npm run dev
   ```

### Single jar

With Postgres running, in `backend/`. The build runs `npm ci` and `npm run build` in `frontend/` and copies the SPA into the jar. It also runs the backend tests, which need Docker; add `-DskipTests` to skip them.

```bash
./mvnw -Pfrontend package
java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

The jar serves the SPA and the API on http://localhost:8080.

### Log in

There are no passwords: pick one of the seeded users.

| User | Roles |
|---|---|
| Customer 1, Customer 2 | customer |
| Expert 1 | expert, customer (to try booking yourself) |
| Expert 2 | expert, admin |
| Admin 1, Admin 2 | admin |

The shop starts with one published offering, Career Coaching, with packages of 3 sessions for 450 and 5 sessions for 700.

`docker compose down -v` stops Postgres and drops its data; the next start of the API migrates and seeds again.

## Docs

- [_docs/](_docs/README.md): use cases, architecture, glossary and decision log
- [AGENTS.md](AGENTS.md): instructions for coding agents

## Planned stack

React SPA · Spring Boot · PostgreSQL · OAuth 2 / OIDC · monorepo
