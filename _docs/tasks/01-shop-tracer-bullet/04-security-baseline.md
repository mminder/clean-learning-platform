# 04 Security baseline

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [02](02-backend-skeleton.md) |
| Status | Done |

## Goal

Spring Security guards every API route by its prefix before the first route exists. The shop is public, and everything else needs a logged-in user with the right role. A new route is denied by default, and the role matrix shows how to test it.

## What to build

Read first: [security.md](../../architecture/security.md), [D-26](../../decisions.md#architecture).

- **Spring Security**, with one `SecurityFilterChain` in `config.SecurityConfig`, following [roles by route prefix](../../architecture/security.md#roles-by-route-prefix):
  - `/api/shop/**` is public;
  - `/api/customer/**`, `/api/expert/**` and `/api/admin/**` need `CUSTOMER`, `EXPERT` and `ADMIN`;
  - every other `/api/**` route (for example `/api/me`) needs a logged-in user;
  - everything outside `/api/**` is public, because it's the SPA's files ([08](08-spa-in-the-jar.md)).
- **401 and 403 without side effects:** an anonymous request gets 401, with no redirect, no login page and no Basic challenge. A missing role gets 403. Turn off form login and HTTP Basic, and make sure no default user or generated password is created.
- **CSRF** stays at Spring's default. The cookie setup for the SPA comes with the first mutating request (UC-01).
- **Not yet:** `/api/poc/**` (UC-01), `GET /api/images/**` (images) and `/v3/api-docs` ([06](06-shop-list.md)). Each task that adds one of these routes also adds its rule.
- **`RoleMatrixTest`**, as described in the plan's [tests](README.md#tests) section. No route exists yet, so its first rows are one probe path per prefix, such as `/api/customer/probe` and `/api/me`. Security decides before routing, so a probe either gets 401 or 403, or passes through to a 404. Until UC-01 adds a real login, the test builds users with Spring Security's test support. A comment above the route table says that every new route gets a row.
- **Docs:** add a row "everything outside `/api` (the SPA) → public" to the prefix table in [security.md](../../architecture/security.md#roles-by-route-prefix).

## Acceptance criteria

- [x] An anonymous request to `/api/shop/...` passes security (404 for a probe, not 401).
- [x] Anonymous requests to `/api/customer/...`, `/api/expert/...`, `/api/admin/...` and `/api/me` get 401, without a `Location` or `WWW-Authenticate: Basic` header.
- [x] A user with only `CUSTOMER` gets 403 under `/api/expert` and `/api/admin`, and the same holds for the other roles.
- [x] A user with `EXPERT` and `ADMIN` passes both of those prefixes and gets 403 under `/api/customer`.
- [x] Anonymous `GET /` and `GET /offerings/anything` pass security.
- [x] The startup log shows no generated password.
- [x] security.md lists the SPA paths as public.

## Tests

- `RoleMatrixTest`, as `@IntegrationTest`

## Out of scope

- Login, logout, `/api/me`, session settings and the CSRF cookie ([UC-01](../../use-cases/uc-01-login.md)).
- Ownership tests: added with the first resource that has an owner.

## Notes

- **Starters:** Boot 4 needs `spring-boot-starter-security` and, for tests, `spring-boot-starter-security-test`. The test starter brings `spring-boot-security-test`, which applies Spring Security to the auto-configured MockMvc.
- **No default user:** a `SecurityFilterChain` bean alone doesn't stop Boot from creating its in-memory user with a generated password. `CleanLearningApplication` excludes `UserDetailsServiceAutoConfiguration`, and `CleanLearningApplicationTest` checks that no `UserDetailsService` exists. Checked by hand: the `poc` startup log has no password line.
- **No session on 401:** Spring's request cache saved every rejected request in a new session, so each anonymous 401 set a `JSESSIONID` cookie. `SecurityConfig` uses a `NullRequestCache`, and the role matrix checks that a 401 creates no session.
- **CSRF default:** a `POST` without a CSRF token gets 403 before authorization is checked, even when anonymous, and the session-based token repository creates a session. `RoleMatrixTest` therefore sends a valid token with every request, so a mutating row tests roles, not CSRF. The cookie setup comes with UC-01.
- **For UC-01:** Spring's default logout filter still answers `POST /logout` (redirect to `/login?logout`); UC-01 configures `/api/logout`. The 401 has an empty body and the 403 has Boot's `/error` JSON; neither goes through `ApiExceptionHandler`, because security rejects the request before a controller runs.
