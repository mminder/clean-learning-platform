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

- [x] With the docker-compose Postgres, `java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc` serves the shop at `http://localhost:8080`, and demo step 5 of the [spec](../../specs/01-shop-tracer-bullet.md#demo) works, including a reload on a detail page.
- [x] `./mvnw verify` without the profile runs no npm.
- [x] `GET /api/shop/nope` returns a `ProblemDetail` 404, not `index.html`, and `GET /assets/missing.js` returns 404.
- [x] CI builds the jar with the SPA.
- [x] AGENTS.md has the command.

## Tests

- `SpaFallbackTest` (`@IntegrationTest`, with a stub `index.html` in the test resources): a client route returns `index.html`, a missing asset returns 404, and an unknown API path returns the API's own answer.

## Out of scope

- A Dockerfile, hosting, and cache headers for the static files: MVP, together with hosting.

## Notes

- **Profile:** `frontend` in `backend/pom.xml`. `exec-maven-plugin` (3.6.4, not managed by the Spring Boot parent) runs `npm ci` and `npm run build` in `generate-resources`; `maven-resources-plugin` `copy-resources` puts `frontend/dist/` into `target/classes/static` in `process-resources`. It calls `npm` directly, so it needs a Unix-like shell or an `npm` on the path (no Windows `npm.cmd` handling).
- **Fallback:** `config.SpaConfig` registers a `/**` resource handler on `classpath:/static/` with a `PathResourceResolver` that returns `index.html` for a path whose last segment has no `.` and whose first segment isn't `api` or `v3` (so `/api` and `/v3` themselves never get `index.html` either). The resource chain runs without its cache, which would otherwise keep one entry per distinct client route a caller sends. `/` is Boot's welcome page (a forward to `index.html`).
- **Missing asset and unknown API path:** both end as Boot's default 404 (`application/json` error body). `GET /api/shop/nope` becomes a `ProblemDetail` only once [07](07-consultation-offering-detail.md) adds `ApiExceptionHandler`; `SpaFallbackTest` only asserts a 404 that isn't `index.html`. Check the `ProblemDetail` body after both tasks are merged.
- **Test stub:** `src/test/resources/static/index.html` (and `stub.js`) shadows nothing in the jar, because test resources aren't packaged. It doesn't change `RoleMatrixTest`.
- **Left unticked, and the task stays In progress, until the merge with 07 and the first CI run:** the detail-page reload (needs 07's route), the `ProblemDetail` body of the API 404 (needs 07's `ApiExceptionHandler`), and the CI `package` job.
- **Not checked locally:** the CI `package` job only ran through `actionlint`; it runs for real on the pull request. Demo step 5's reload on a detail page serves `index.html` here, but the detail route is [07](07-consultation-offering-detail.md), so the SPA shows its not-found page until it's merged.
- 2026-11-07, after merging 07: checked on the merged branch. `./mvnw -Pfrontend package` built the jar (110 backend tests passed during the build). With the docker-compose Postgres, `java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc` served the shop at `http://localhost:8080`: the card shows "from CHF 450.00", a click opens `/offerings/00000000-0000-0000-0000-000000001001` with both packages and both experts, and a reload on that page answers 200 `text/html` and shows the detail again. An unknown id in the URL shows "Offering not found". With curl against the jar: `GET /api/shop/nope` answers 404 `application/problem+json` (the `ProblemDetail` from 07's `ApiExceptionHandler`, not `index.html`), `GET /assets/missing.js` answers 404 `application/problem+json`, `GET /v3/api-docs` answers 200 (identical to `backend/openapi.json`, apart from the file's trailing newline), and an anonymous `GET /api/admin/x` answers 401. "CI builds the jar with the SPA" is verified by the `package` job of [CI run 7](https://github.com/mminder/clean-learning-platform/actions/runs/42405417895) on pull request #2, which was green.
