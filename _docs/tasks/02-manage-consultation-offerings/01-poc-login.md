# 01 PoC login

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

A visitor logs in by picking a seeded user and logs out again. The SPA knows who is logged in and sends the CSRF token on mutating requests, so every later task can call protected routes from the browser.

## What to build

Read first: [UC-01](../../use-cases/uc-01-login.md), [security.md](../../architecture/security.md), [D-25](../../decisions.md#architecture) and [D-26](../../decisions.md#architecture), and the plan's [security and login](README.md#security-and-login), [API](README.md#api) and [auth in the SPA](README.md#auth-in-the-spa) sections.

Work in this order, test first:

1. **A failing REST test:** `POST /api/poc/login` as Admin 1, then `GET /api/me` with the returned session, expects Admin 1 with `ADMIN`.
2. **Domain:** `Role`, `CurrentUser` and `User`, as in the plan's [names](README.md#packages-and-names). Keep the name and the email as `String`s unless a rule needs a value object ([D-36](../../decisions.md#architecture)).
3. **Ports and adapter:** `LoadUsersPort` (every user with their roles, sorted by name) and `LoadUserPort` (one user with their roles). `UserAdapter` reads `app_user` and `user_role` through new `UserEntity` and `UserRoleEntity`, with one query per call (no N+1).
4. **Use cases:** `ListPocUsersUseCase`, `LogInPocUserUseCase` (`NotFoundException` for an unknown id), `GetCurrentUserUseCase`.
5. **Controllers:** `PocLoginController` (`@Profile("poc")`) with `GET /api/poc/users` and `POST /api/poc/login`, and `MeController` with `GET /api/me`, shaped as in the plan's [API](README.md#api) section. The login stores the authentication in the session as the plan says: new session id, new CSRF token.
6. **`SecurityConfig`:** the SPA's CSRF setup, logout at `POST /api/logout` with 204, `/api/poc/**` public, and the session cookie settings. Spring's logout filter answers before authorization, so an anonymous logout is a harmless 204; record that in [security.md](../../architecture/security.md#roles-by-route-prefix) rather than working around it.
7. **`LayerRulesTest`:** `PocLoginRequest` is the first request record, so add the rule that request records live in a `request` package ([backend.md](../../architecture/backend.md#rest-conventions) promised it with the first one).
8. **Test helper and role matrix:** `LoggedIn.as(userId, roles…)` from the plan's [tests](README.md#tests). `RoleMatrixTest` switches its callers to it, gets rows for the four routes, and turns its `/api/me` probe into the real route.
9. **Contract:** regenerate `openapi.json` and `schema.d.ts`.
10. **Frontend:**
    - `api/client.ts`: a middleware that sends `X-XSRF-TOKEN`, read from the `XSRF-TOKEN` cookie, on `POST`, `PUT`, `PATCH` and `DELETE`;
    - `features/user/queries.ts`: `useMe` (a 401 is `null`), `usePocUsers`, `useLogin`, `useLogout`;
    - `LoginPage` at `/login`: the seeded users as a list, each with their name and roles; a click logs in and opens `redirect` or `landingPath(roles)`;
    - `landing.ts`: `landingPath` (the shop for now) and the check that `redirect` is a local path;
    - the header: **Log in** (a link to `/login?redirect=<current path>`), or the user's name and **Log out**, which opens the shop;
    - after login and logout, no cached data of the previous user remains.
11. **Docs:** [security.md](../../architecture/security.md) gets the CSRF and logout details as built.

## Acceptance criteria

- [ ] In a fresh browser on the dev server (Playwright): **Log in**, pick Admin 1, the header shows "Admin 1" and **Log out**; **Log out** shows **Log in** again. No request fails with 403.
- [ ] `GET /api/poc/users` lists the six seed users with their UC-01 roles, sorted by name.
- [ ] `POST /api/poc/login` starts a session with a new session id, and `GET /api/me` then returns id, name, email and roles. Without a login, `GET /api/me` answers 401 with an empty body.
- [ ] `POST /api/poc/login` with an unknown user id answers 404 as `ProblemDetail`.
- [ ] `POST /api/logout` answers 204, and the same session gets 401 from `GET /api/me` afterwards.
- [ ] A `POST` without a CSRF token answers 403.
- [ ] Without the `poc` profile, `GET /api/poc/users` and `POST /api/poc/login` answer 404.
- [ ] `/login?redirect=//example.com` opens the shop after login, not another site.
- [ ] The role matrix has the four routes, and its callers use `LoggedIn`.
- [ ] `openapi.json` and `schema.d.ts` are regenerated, and every check passes.

## Tests

- REST: the login flow, `GET /api/me` with and without a login, logout, the unknown user, the missing CSRF token
- Authorization: `RoleMatrixTest` rows; one test without the `poc` profile for `/api/poc/**`. Keep it cheap: it shouldn't start a second Postgres container.
- Frontend: `landingPath` and the redirect check, the CSRF header

`SeedDataTest` checks the seed users with SQL "since nothing reads users through the API yet". `GET /api/poc/users` reads them now, so its REST test may take over that check.

## Out of scope

- **Consultations** in the header, `RequireRole` and the admin landing page ([02](02-consultation-offering-list.md), with the first protected page).
- OIDC, Keycloak and just-in-time provisioning (MVP).
- The landing pages of experts and customers (UC-11, UC-05).

## Notes
