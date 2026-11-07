# 08 SPA in the jar

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [06](06-shop-list.md) |
| Status | Todo |

## Goal

`./mvnw -Pfrontend package` builds one jar that serves the SPA and the API from one origin, which is the production runtime described in [overview.md](../../architecture/overview.md#runtime-and-profiles).

## What to build

- **Maven profile `frontend`:** runs `npm ci` and `npm run build` in `../frontend` with the installed Node and npm, so `.nvmrc` stays the only Node version to maintain. It then copies `frontend/dist/` into the jar's `static/` folder. Without the profile, the backend build doesn't touch the frontend.
- **SPA fallback:** a `GET` outside `/api/**` and `/v3/**` whose last path segment has no file extension is answered with `index.html`, so client routes like `/offerings/<id>` survive a reload. Existing static files are served as they are. A missing asset gets 404. An unknown `/api/...` path keeps its API answer and never gets `index.html`.
- Spring Security already lets everything outside `/api/**` through ([04](04-security-baseline.md)).
- **CI:** a `package` job, after `backend` and `frontend`, that builds the jar with the profile and skips the tests that already ran.
- **Docs:** the command in AGENTS.md.

## Acceptance criteria

- [ ] With the docker-compose Postgres, `java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc` serves the shop at `http://localhost:8080`, and demo step 5 of the [spec](../../specs/01-shop-tracer-bullet.md#demo) works, including a reload on a detail page.
- [ ] `./mvnw verify` without the profile runs no npm.
- [ ] `GET /api/shop/nope` returns a `ProblemDetail` 404, not `index.html`, and `GET /assets/missing.js` returns 404.
- [ ] CI builds the jar with the SPA.
- [ ] AGENTS.md has the command.

## Tests

- `SpaFallbackTest` (`@IntegrationTest`, with a stub `index.html` in the test resources): a client route returns `index.html`, a missing asset returns 404, and an unknown API path returns the API's own answer.

## Out of scope

- A Dockerfile, hosting, and cache headers for the static files: MVP, together with hosting.

## Notes
