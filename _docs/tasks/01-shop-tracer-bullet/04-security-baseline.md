# 04 Security baseline

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [02](02-backend-skeleton.md) |
| Status | Todo |

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

- [ ] An anonymous request to `/api/shop/...` passes security (404 for a probe, not 401).
- [ ] Anonymous requests to `/api/customer/...`, `/api/expert/...`, `/api/admin/...` and `/api/me` get 401, without a `Location` or `WWW-Authenticate: Basic` header.
- [ ] A user with only `CUSTOMER` gets 403 under `/api/expert` and `/api/admin`, and the same holds for the other roles.
- [ ] A user with `EXPERT` and `ADMIN` passes both of those prefixes and gets 403 under `/api/customer`.
- [ ] Anonymous `GET /` and `GET /offerings/anything` pass security.
- [ ] The startup log shows no generated password.
- [ ] security.md lists the SPA paths as public.

## Tests

- `RoleMatrixTest`, as `@IntegrationTest`

## Out of scope

- Login, logout, `/api/me`, session settings and the CSRF cookie ([UC-01](../../use-cases/uc-01-login.md)).
- Ownership tests: added with the first resource that has an owner.

## Notes
