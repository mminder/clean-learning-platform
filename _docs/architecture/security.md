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

- **Session cookie:** `HttpOnly`, `Secure` (outside local development), `SameSite=Lax`.
- **CSRF:** Spring Security's SPA support. The token is sent in the `XSRF-TOKEN` cookie and returned in the `X-XSRF-TOKEN` header on state-changing requests.
- **`GET /api/me`** returns `{ id, name, email, roles }`, or 401. The SPA uses it identically in the PoC and the MVP.
- **Logout:** `POST /api/logout` invalidates the session. In the MVP it also performs OIDC RP-initiated logout.
- **PoC login** (`/api/poc/**`) is registered only in the `poc` profile, so it doesn't exist in production.
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
| `/api/me`, `/api/logout`, every other `/api/**` | authenticated |
| `/api/customer/**` | `CUSTOMER` |
| `/api/expert/**` | `EXPERT` |
| `/api/admin/**` | `ADMIN` |
| `/api/poc/**` | public, `poc` profile only |
| `/v3/api-docs/**` (the OpenAPI document, [D-29](../decisions.md#architecture)) | public |
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
3. **Profile guard:** `/api/poc/**` returns 404 when the `poc` profile isn't active.

## Configuration and secrets

- The IdP client id and secret and the database credentials come from environment variables. Nothing secret is committed.
- The local Keycloak realm and the docker-compose credentials are dev-only values and may be committed.
