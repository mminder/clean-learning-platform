# 08 SPA in the jar

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [06](06-shop-list.md) |
| Status | Done |

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
- [x] `./mvnw verify` without the profile runs no npm.
- [ ] `GET /api/shop/nope` returns a `ProblemDetail` 404, not `index.html`, and `GET /assets/missing.js` returns 404.
- [ ] CI builds the jar with the SPA.
- [x] AGENTS.md has the command.

## Tests

- `SpaFallbackTest` (`@IntegrationTest`, with a stub `index.html` in the test resources): a client route returns `index.html`, a missing asset returns 404, and an unknown API path returns the API's own answer.

## Out of scope

- A Dockerfile, hosting, and cache headers for the static files: MVP, together with hosting.

## Notes

- **Profile:** `frontend` in `backend/pom.xml`. `exec-maven-plugin` (3.6.4, not managed by the Spring Boot parent) runs `npm ci` and `npm run build` in `generate-resources`; `maven-resources-plugin` `copy-resources` puts `frontend/dist/` into `target/classes/static` in `process-resources`. It calls `npm` directly, so it needs a Unix-like shell or an `npm` on the path (no Windows `npm.cmd` handling).
- **Fallback:** `config.SpaConfig` registers a `/**` resource handler on `classpath:/static/` with a `PathResourceResolver` that returns `index.html` for a path whose last segment has no `.` and that isn't under `api/` or `v3/`. `/` is Boot's welcome page (a forward to `index.html`).
- **Missing asset and unknown API path:** both end as Boot's default 404 (`application/json` error body). `GET /api/shop/nope` becomes a `ProblemDetail` only once [07](07-consultation-offering-detail.md) adds `ApiExceptionHandler`; `SpaFallbackTest` only asserts a 404 that isn't `index.html`. Check the `ProblemDetail` body after both tasks are merged.
- **Test stub:** `src/test/resources/static/index.html` (and `stub.js`) shadows nothing in the jar, because test resources aren't packaged. It doesn't change `RoleMatrixTest`.
- **Left unticked until the merge with 07 and the first CI run:** the detail-page reload (needs 07's route), the `ProblemDetail` body of the API 404 (needs 07's `ApiExceptionHandler`), and the CI `package` job.
- **Not checked locally:** the CI `package` job only ran through `actionlint`; it runs for real on the pull request. Demo step 5's reload on a detail page serves `index.html` here, but the detail route is [07](07-consultation-offering-detail.md), so the SPA shows its not-found page until it's merged.
