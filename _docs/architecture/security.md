# Security architecture

## Authentication

The backend-for-frontend (BFF) pattern (D-25): Spring Boot holds the session, and the SPA never sees tokens.

```
PoC (profile poc)                              MVP (OIDC)
 SPA                    Spring Boot             SPA          Spring Boot              IdP
  │ GET /api/poc/users ►│                        │ click Log in │                        │
  │◄─── seeded users ───│                        │──── /oauth2/authorization/idp ───────►│
  │ POST /api/poc/login►│ create session         │              │◄── code (redirect) ────│
  │◄── Set-Cookie ──────│                        │              │── token exchange ─────►│
  │ GET /api/me ───────►│ user + roles from DB   │◄─ Set-Cookie ─│ find/create user by sub│
                                                 │ GET /api/me ─►│ user + roles from DB   │
```

- **Session cookie:** `JSESSIONID`, `HttpOnly`, `SameSite=Lax` (`server.servlet.session.cookie` in `application.yml`); `Secure` comes with production (MVP).
- **Principal:** the session holds an authenticated token whose principal is a `CurrentUser` (id and roles) and whose authorities are `ROLE_CUSTOMER`, `ROLE_EXPERT` and `ROLE_ADMIN` (`CurrentUserAuthentication`). Controllers take it with `@AuthenticationPrincipal CurrentUser`. The roles are read from the database at login and kept for the session.
- **CSRF:** Spring Security 7's SPA support, `csrf.spa()` in `SecurityConfig`:
  - The token is in the `XSRF-TOKEN` cookie, which JavaScript can read (`CookieCsrfTokenRepository`, not `HttpOnly`, path `/`). Every `POST`, `PUT`, `PATCH` and `DELETE` sends it back in the `X-XSRF-TOKEN` header; without it, or with another value, the answer is 403.
  - The token isn't deferred: any response to a request without the cookie sets it, a 401 included. The SPA's first call, `GET /api/me`, gets it before the first mutating request.
  - A login replaces the token, and a logout deletes the cookie; the next response sets a new one. The SPA reads the cookie on each request ([frontend](frontend.md#navigation-and-auth)).
- **`GET /api/me`** returns `{ id, name, email, roles }`, or 401 with an empty body. Name, email and roles are read fresh from the database. The SPA uses it identically in the PoC and the MVP.
- **Logout:** Spring Security's logout filter at `POST /api/logout` (it needs the CSRF token): it invalidates the session, deletes the `JSESSIONID` and `XSRF-TOKEN` cookies and answers 204. It runs before authorization, so a visitor's logout is a harmless 204. `/logout` is nothing special. `openapi.json` lists the route (`OpenApiConfig`), so the SPA calls it through the generated client. In the MVP it also performs OIDC RP-initiated logout.
- **PoC login** (`/api/poc/**`) is registered only in the `poc` profile, so it doesn't exist in production. `POST /api/poc/login` does what Spring Security's own logins do: a new session id (session fixation) and a new CSRF token, through the `SessionAuthenticationStrategy` bean, then it saves the authentication through the `SecurityContextRepository` bean, the one the filter chain reads. An OIDC login (MVP) ends in the same session.
- **Known gaps (hardening, by MVP hosting at the latest):**
  - The `XSRF-TOKEN` cookie has no `SameSite` attribute.
  - The public, cacheable `GET /api/images/{id}` response can carry `Set-Cookie: XSRF-TOKEN` on a visitor's first request. Before a CDN or shared cache sits in front, keep the CSRF cookie off `/api/images/**` or keep the cache from storing `Set-Cookie` ([task 07 of increment 02](../tasks/02-manage-consultation-offerings/07-offering-image.md#notes)).
  - `HEAD /api/images/{id}` answers 401: the public matcher allows only `GET`.
- **IdP:** Keycloak locally (docker-compose, realm import). The production IdP is open; only standard OIDC is used, so switching needs only configuration.

### Just-in-time provisioning (MVP)

On a successful OIDC login, the backend looks up the user by `(issuer, subject)`:
- **found:** name and email are updated from the ID token;
- **not found:** the user is created with role `CUSTOMER`.

Roles are always read from our database, never from token claims (D-04).

## Authorization

### Roles by route prefix

Configured declaratively in the Spring Security config (D-26).

| Prefix | Access |
|---|---|
| `/api/shop/**`, `GET /api/images/**` | public |
| `POST /api/logout` | public: the logout filter answers before authorization, with 204 for a visitor, too |
| `/api/me`, every other `/api/**` | authenticated |
| `/api/customer/**` | `CUSTOMER` |
| `/api/expert/**` | `EXPERT` |
| `/api/admin/**` | `ADMIN` |
| `/api/poc/**` | public, `poc` profile only |
| `/v3/api-docs/**` (the OpenAPI document, [D-29](../decisions.md#architecture)) | public, in every profile including production |
| everything outside `/api` (the SPA) | public |

A user with several roles can call every prefix their roles allow. An anonymous caller gets 401 without a redirect, login page or Basic challenge; a missing role gets 403.

### Ownership in use cases

Controllers pass `CurrentUser` (id, roles) into the use case. Ports load data **scoped to that user**. If the resource doesn't exist, or exists but isn't the user's, the result is **404**, so nobody can probe which ids exist.

| Resource | Customer | Expert | Admin |
|---|---|---|---|
| Case | own cases | cases where they are the assigned expert | all |
| Appointment | in own cases | in assigned cases | all |
| Note | `PUBLIC` notes of own cases | all notes of assigned cases | **none** |
| Credit adjustment | – (sees resulting credits) | – (sees resulting credits) | all |
| Enrollment | own | participants of own runs | all |
| Run | runs of own enrollments | own runs | all |
| Availability | – (only derived slots) | own | – |
| Expert profile | public fields | own (edit) | public fields |
| Teams link | inside the link window (UC-10) | always for own calls | runs |

### Shaped responses

- The admin case response **has no notes field**, and the customer case response contains only public notes. The data can't leak by accident because the field doesn't exist.
- Customer responses contain the Teams link only inside the link window.

## Testing

Authorization is tested through REST integration tests against Testcontainers Postgres, because the real filtering happens in SQL (D-26).

1. **Role matrix:** a parameterized test over every route. Anonymous gets 401 (except public routes), and a user without the required role gets 403.
2. **Ownership:** using the seed users (two per role):
   - Customer 2 on Customer 1's case or enrollment → 404;
   - Expert 2 on Expert 1's case or run → 404;
   - the admin case detail contains no notes;
   - the customer case detail contains no private notes;
   - the customer's Teams link is absent outside the link window (fixed `Clock`).
3. **Public images:** `GET /api/images/{id}` passes security without a login, and its `Cache-Control` stays the controller's (`ImageControllerTest`).
4. **Profile guard:** `/api/poc/**` returns 404 when the `poc` profile isn't active (`PocLoginProfileTest`, a web slice without a database).

Test helpers: `LoggedIn.as(userId, roles…)` makes a request with the authentication a PoC login keeps in the session. `Csrf.token()` sends a CSRF token the way the SPA does, cookie and header. Spring Security Test's `csrf()` isn't used: it swaps the CSRF filter's token repository for a session-based one in the Spring context all integration tests share.

## Configuration and secrets

- The IdP client id and secret and the database credentials come from environment variables. Nothing secret is committed.
- The local Keycloak realm and the docker-compose credentials are dev-only values and may be committed.
