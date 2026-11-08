# 02 Manage consultation offerings: technical plan

Spec: [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same change. Everything from [increment 01's plan](../01-shop-tracer-bullet/README.md) still holds: toolchain, test setup, error conventions and the frontend structure.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [PoC login](01-poc-login.md) | AFK | – |
| 02 | [Consultation offering list](02-consultation-offering-list.md) | AFK | 01 |
| 03 | [Create and edit a consultation offering](03-create-and-edit-consultation-offering.md) | AFK | 02 |
| 04 | [Packages](04-packages.md) | AFK | 03 |
| 05 | [Eligible experts](05-eligible-experts.md) | AFK | 04 |
| 06 | [Publish, unpublish, archive, delete](06-offering-lifecycle.md) | AFK | 05 |
| 07 | [Offering image](07-offering-image.md) | AFK | 06 |

The tasks run one after another:

```
01 PoC login ─► 02 List ─► 03 Create and edit ─► 04 Packages ─► 05 Eligible experts ─► 06 Lifecycle ─► 07 Image
```

They build one form on one aggregate, so they touch the same controller, use cases, adapter, form and generated contract. In parallel, every merge would conflict. Each task is still a vertical slice that can be demonstrated on its own. 03 sets the pattern for writing use cases, as 06 of increment 01 did for reading ones; review it with extra care.

## Backend

### Packages and names

| Package | Classes | Task |
|---|---|---|
| `config` | `SecurityConfig`: session login, CSRF for the SPA, `/api/logout`, `/api/poc/**` public (01), `GET /api/images/**` public (07); `ProblemDetailSchemaCustomizer`: the shared `ProblemDetail` schema (03) | 01, 03, 07 |
| `api` | `ApiExceptionHandler`: 400 with a field list and `IllegalArgumentException` → 400 (03), `BusinessRuleException` → 409 with `code` (06), an upload over the limit → 400 (07); `ProblemResponses`: the error answers a route declares in `openapi.json` (overall review) | 03, 06, 07 |
| `api.response` | `ConsultationPackageResponse`, moved here from `api.shop.response`, because the shop and the admin both return it | 04 |
| `api.user` | `PocLoginController` (`@Profile("poc")`), `MeController`, `ExpertController` | 01, 05 |
| `api.user.request` | `PocLoginRequest` | 01 |
| `api.user.response` | `PocUserResponse`, `MeResponse`, `ExpertResponse` | 01, 05 |
| `api.offering` | `ConsultationOfferingController` (`/api/admin/consultation-offerings`) | 02–07 |
| `api.offering.request` | `ConsultationOfferingRequest`, `ConsultationPackageRequest` | 03, 04 |
| `api.offering.response` | `ConsultationOfferingSummaryResponse` (list item), `ConsultationOfferingResponse` (detail) | 02, 03 |
| `api.image` | `ImageController` | 07 |
| `application.user` | `ListPocUsersUseCase`, `LogInPocUserUseCase`, `GetCurrentUserUseCase`, `ListExpertsUseCase`; `LoadUsersPort`, `LoadUserPort`, `LoadExpertsPort` | 01, 05 |
| `application.offering` | `ListConsultationOfferingsUseCase`, `GetConsultationOfferingUseCase`, `CreateConsultationOfferingUseCase`, `EditConsultationOfferingUseCase`, `PublishConsultationOfferingUseCase`, `UnpublishConsultationOfferingUseCase`, `ArchiveConsultationOfferingUseCase`, `DeleteConsultationOfferingUseCase`, `UploadConsultationOfferingImageUseCase`, `RemoveConsultationOfferingImageUseCase` | 02–07 |
| `application.offering` (commands) | `ConsultationOfferingCommand`: what the form saves, for create and edit; it gains the packages (04) and the eligible expert ids (05) | 03–05 |
| `application.offering` (ports) | `LoadConsultationOfferingSummariesPort`, `LoadConsultationOfferingPort`, `LockConsultationOfferingPort`, `SaveConsultationOfferingPort` (`create`, `update`), `DeleteConsultationOfferingPort` | 02, 03, 06 |
| `application.image` | `GetImageUseCase`, `ImageStorePort` (`save`, `load`, `delete`) | 07 |
| `domain.shared` | `BusinessRuleException`: a business rule says no; carries a stable `code` and a message for the user | 06 |
| `domain.user` | `CurrentUser` (id, roles), `User` (id, name, email, roles), `Expert` (id, display name) | 01, 05 |
| `domain.user.value` | `Role` (`CUSTOMER`, `EXPERT`, `ADMIN`) | 01 |
| `domain.offering` | `ConsultationOfferingSummary`, `ConsultationOffering` (the admin's offering: fields, status, packages, eligible expert ids, image id; holds the UC-16 rules), `ConsultationPackageEntry` (a package as the form entered it: with the id it keeps, or without one for a new package) | 02, 03, 04 |
| `domain.offering.value` | `OfferingStatus` (`DRAFT`, `PUBLISHED`, `ARCHIVED`) | 02 |
| `domain.image` | `Image` (id, type, bytes; at most 2 MB, not empty) | 07 |
| `domain.image.value` | `ImageId`, `ImageType` (`JPEG`, `PNG`, `WEBP`, with the content type and detection from the first bytes) | 07 |
| `adapters.database.user` | `UserAdapter` (users, roles, experts), its repository; `entity`: `UserEntity`, `UserRoleEntity` | 01, 05 |
| `adapters.database.offering` | `ConsultationOfferingAdapter`, which implements the admin's offering ports; the repositories it needs. `ShopOfferingAdapter` stays for the shop. | 02–07 |
| `adapters.database.image` | `ImageAdapter` (through the `EntityManager`, no repository: it only finds, persists and removes by id); `entity`: `ImageEntity` | 07 |

The admin's use cases don't take a `CurrentUser`: an admin may change every offering, and nothing records who did it. `ConsultationPackage` from increment 01 serves the admin too.

### Schema

| Migration | Task | Content |
|---|---|---|
| `V4__set_updated_at.sql` | 03 | the function `set_updated_at()` and a `before update` trigger with it on every table, so `updated_at` is the time of the last change ([D-20](../../decisions.md#cross-cutting)) |
| `V5__create_image.sql` | 07 | the `image` table with its `updated_at` trigger, and `offering.image_id` |

```
image      id uuid PK, content_type text in ('image/jpeg', 'image/png', 'image/webp'),
           data bytea, 1 to 2097152 bytes (octet_length), created_at, updated_at
offering   + image_id uuid null unique → image
```

- **`offering.image_id` is unique** (07): an image belongs to one offering at most, so replacing or removing it may delete it. The unique index also serves the foreign key check when an image is deleted.

- **`updated_at`** is set by the database, in the trigger, to the transaction's `now()`, like `created_at`'s default. Entities don't map either column. A table created later adds the trigger in its create migration. Task 03 adds this rule to [backend.md](../../architecture/backend.md#persistence).
- **2 MB** is 2 × 1024 × 1024 bytes, the same as Spring's `2MB`.
- No new seed. The demo creates its offerings through the UI.

### API

| Route | Answer | Task |
|---|---|---|
| `GET /api/poc/users` | 200 `PocUserResponse[]`, sorted by name; `poc` profile only | 01 |
| `POST /api/poc/login` | `PocLoginRequest` → 200 `MeResponse`, starts the session; 400 without a user id; 404 for an unknown user; `poc` profile only | 01 |
| `GET /api/me` | 200 `MeResponse`; 401 without a login; 404 if the session's user no longer exists | 01 |
| `POST /api/logout` | 204, ends the session; Spring Security's logout filter, listed in `openapi.json` by `OpenApiConfig` | 01 |
| `GET /api/admin/consultation-offerings` | 200 `ConsultationOfferingSummaryResponse[]`: every status, sorted by name, then id | 02 |
| `POST /api/admin/consultation-offerings` | `ConsultationOfferingRequest` → 201 `ConsultationOfferingResponse` with a `Location` header; 400 | 03 |
| `GET /api/admin/consultation-offerings/{id}` | 200 `ConsultationOfferingResponse`; 404; 400 for a malformed id | 03 |
| `PUT /api/admin/consultation-offerings/{id}` | `ConsultationOfferingRequest` → 200 `ConsultationOfferingResponse`; 400; 404; 409 (06) | 03 |
| `GET /api/admin/experts` | 200 `ExpertResponse[]`: users with `EXPERT` and an expert profile, sorted by display name | 05 |
| `POST /api/admin/consultation-offerings/{id}/publish` | 200 `ConsultationOfferingResponse`; 404; 409 | 06 |
| `POST /api/admin/consultation-offerings/{id}/unpublish` | 200 `ConsultationOfferingResponse`; 404; 409 | 06 |
| `POST /api/admin/consultation-offerings/{id}/archive` | 200 `ConsultationOfferingResponse`; 404; 409 | 06 |
| `DELETE /api/admin/consultation-offerings/{id}` | 204; 404; 409 | 06 |
| `PUT /api/admin/consultation-offerings/{id}/image` | multipart, part `file` → 200 `ConsultationOfferingResponse`; 400 (no `file` part, an empty file, not JPEG, PNG or WebP, over 2 MB); 404; 409; 415 for a body that isn't multipart | 07 |
| `DELETE /api/admin/consultation-offerings/{id}/image` | 200 `ConsultationOfferingResponse`; 404; 409 | 07 |
| `GET /api/images/{id}` | 200 with the bytes, their `Content-Type` and `Cache-Control: public, max-age=31536000, immutable`; 404; public | 07 |

A route with an `{id}` also answers 400 for a malformed id. 401 and 403 come from security ([security.md](../../architecture/security.md#roles-by-route-prefix)). Each controller method declares its 400, 404, 409 and 415 in `openapi.json`, and a route without any declares none ([errors](#errors)).

```jsonc
// PocUserResponse
{ "id": "00000000-0000-0000-0000-000000000301", "name": "Admin 1", "roles": ["ADMIN"] }

// PocLoginRequest
{ "userId": "00000000-0000-0000-0000-000000000301" }

// MeResponse
{ "id": "…0301", "name": "Admin 1", "email": "admin1@example.com", "roles": ["ADMIN"] }

// ConsultationOfferingSummaryResponse
{ "id": "…1001", "name": "Career Coaching", "status": "PUBLISHED", "packageCount": 2, "eligibleExpertCount": 2 }

// ConsultationOfferingResponse
{
  "id": "…1001",
  "name": "Career Coaching",
  "description": "…",
  "status": "PUBLISHED",
  "sessionDurationMinutes": 60,
  "cancellationDeadlineHours": 24,
  "packages": [                                              // 04; fewest sessions first, then id
    { "id": "…1011", "sessionCount": 3, "price": { "amount": "450.00", "currency": "CHF" } }
  ],
  "eligibleExpertIds": ["…0201", "…0202"],                   // 05; sorted
  "imageId": null                                            // 07; @Nullable
}

// ConsultationOfferingRequest: the body of POST and PUT
{
  "name": "Agile Coaching",                                  // not blank
  "description": "",                                         // not null, may be empty
  "sessionDurationMinutes": 45,                              // not null, > 0
  "cancellationDeadlineHours": 48,                           // not null, >= 0
  "packages": [                                              // 04; not null, may be empty
    { "id": "…", "sessionCount": 3, "price": "450.00" },     // with id: keeps that package of this offering
    { "sessionCount": 1, "price": "160" }                    // without id: a new package
  ],
  "eligibleExpertIds": ["…0202"]                             // 05; not null, may be empty; users with EXPERT
}

// ExpertResponse
{ "id": "…0201", "displayName": "Expert 1" }
```

- **Roles** are listed in the order `CUSTOMER`, `EXPERT`, `ADMIN`.
- **Price in a request** is a string: an amount in CHF, not negative, with at most two decimals and at most 8 digits before the point (the column is `numeric(10,2)`). One `@Pattern` checks all of it; its message says `must be an amount in CHF with at most two decimals`. `@NotNull` comes with it, because a missing price would pass the pattern, and so does `@NotNull` on each list entry. Responses keep `MoneyResponse`.
- **Numbers in a request** are `Integer` with `@NotNull`, so a missing number is a 400, not a silent 0. A decimal in a whole-number field is a 400 too, not cut off (`spring.jackson.deserialization.accept-float-as-int: false`, API-wide); it's an unreadable body, so the 400 has no `errors` (03).
- **`PUT` replaces** the fields, the packages and the eligible experts. A package with an id must be one of this offering's packages, otherwise 400; on `POST`, any package id is a 400. Duplicate expert ids count once.
- **`imageId`** is added to `ShopOfferingResponse` and `ShopOfferingDetailResponse` too, `@Nullable` (07).
- New request and response names stay unique across `api` (`LayerRulesTest`).

### Errors

[D-34](../../decisions.md#architecture), now with bodies the frontend reads. Task 03 adds one shared `ProblemDetail` schema to `openapi.json` with the field list, and 06 adds `code`. `ProblemDetailSchemaCustomizer` (an `OpenApiCustomizer`) builds it as the body is sent ([backend.md](../../architecture/backend.md#errors)). Each route lists exactly the error answers of the route table above, with that schema: the controller method declares them with `ProblemResponses`' annotations (`@ProblemResponses.Status400`, `Status404`, `Status409` and `Status415`) and its success with `@ResponseStatus`, and springdoc doesn't add the advice's answers to every route (overall review; until then, every route listed 400, 404 and 409). The schema is named `ProblemDetail`, the drift test covers it, and the frontend reads `components['schemas']['ProblemDetail']`.

Spring leaves `"type": "about:blank"` out of the body, so the examples don't show it, and the schema's `type` is optional (RFC 9457: a missing `type` means `about:blank`).

```jsonc
// 400 from Bean Validation; a value object's rule gives the same body without "errors"
{
  "title": "Bad Request", "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/admin/consultation-offerings",
  "errors": [ { "field": "packages[1].price", "message": "must be an amount in CHF with at most two decimals" } ]
}

// 409
{
  "title": "Conflict", "status": 409,
  "detail": "The offering needs at least one eligible expert before it can be published.",
  "instance": "/api/admin/consultation-offerings/…/publish",
  "code": "OFFERING_INCOMPLETE"
}
```

- **400 from Bean Validation** lists every invalid field, sorted by `field`. `field` is the path Spring reports (`name`, `packages[1].price`); `message` is Hibernate Validator's default ("must not be blank") unless the plan says otherwise, in English whatever the browser's `Accept-Language` (03).
- **400 from a value object.** The request annotations repeat UC-16's rules, but a value object stays the authority. A value it rejects anyway, such as a name of only no-break spaces, throws `IllegalArgumentException`, which `ApiExceptionHandler` maps to 400 with the message as `detail`. The same goes for a package id that isn't the offering's and an expert id that isn't an expert. In this code base, `IllegalArgumentException` means "invalid value"; don't throw it for anything else. Task 03 adds this to [backend.md](../../architecture/backend.md#errors).
- **409** comes from `BusinessRuleException` (06). `detail` is written for the user, and the frontend shows it ([D-34](../../decisions.md#architecture)).

| Code | When | Task |
|---|---|---|
| `OFFERING_INCOMPLETE` | publishing without a package or without an eligible expert; `detail` names what's missing | 06 |
| `WRONG_OFFERING_STATUS` | a change the status doesn't allow (table below), usually from a stale page | 06 |
| `OFFERING_HAS_PURCHASES` | unpublishing or deleting with purchases: comes with UC-03 (spec decision 5) | – |

| Status | Edit, image | Publish | Unpublish | Archive | Delete |
|---|---|---|---|---|---|
| `DRAFT` | ✓ | ✓ if complete, else `OFFERING_INCOMPLETE` | 409 | 409 | ✓ |
| `PUBLISHED` | ✓ | 409 | ✓ | ✓ | 409 |
| `ARCHIVED` | 409 | 409 | 409 | 409 | 409 |

Every 409 in this table without a code of its own is `WRONG_OFFERING_STATUS`. Before 06, every status may be edited.

### Writes

- **Lock first.** Every use case that changes an offering (edit, publish, unpublish, archive, delete, image) loads it with `LockConsultationOfferingPort.lock(id)`: a row lock on its `offering` row, at the start of the transaction. It's JPA's `PESSIMISTIC_WRITE`, which Hibernate sends as `select … for no key update` (03): it waits for every other lock of the row, but not for a foreign key check's key-share lock, so every writer that must wait takes this lock itself ([backend.md](../../architecture/backend.md#concurrency)). An unknown id is a 404. This makes concurrent status changes wait for each other and keeps an archived offering read-only. The lock order is now offering → case → expert → run ([D-27](../../decisions.md#architecture)).
- **The domain decides, the adapter writes.** `ConsultationOffering` returns the changed offering; `SaveConsultationOfferingPort.update` writes it.
- **Packages are matched by id** (04). The domain keeps a package whose id the request names (with the new session count and price), creates a package with a new id for each entry without one, and drops the rest. The adapter deletes the dropped packages, updates the changed ones and inserts the new ones. It never deletes and re-inserts a package that's kept. A package named twice is a 400, too. The domain keeps the packages sorted by session count, then by the id as text, which is the database's uuid order.
- **Eligible experts** are a set of ids (05). The adapter writes them like the packages, as a difference: it deletes the dropped rows and inserts the new ones, and leaves a kept row alone. Replacing every row would delete and insert the same key in one flush, and Hibernate inserts first.
- **New rows are persisted, not merged.** The application sets the ids, so Spring Data's `save()` would treat each new entity as existing and select it first. Insert new rows with `EntityManager.persist` (or make the entity `Persistable`). Entities gain writable fields and a constructor for this; they still map only the columns the adapter uses. `update` changes the managed entities, which the lock loaded in the same transaction (03).
- **Delete** (06) removes the offering's `eligible_expert`, `consultation_package`, `consultation_offering` and `offering` rows, in that order, and from 07 its `image` row after the `offering` row.
- **Images** (07). Uploading stores the new image, points `offering.image_id` at it and deletes the old image, in one transaction. A replaced image gets a new id, so `GET /api/images/{id}` can be cached for good.

### Security and login

- **Principal.** The PoC login puts a `CurrentUser` (id and roles) into the session as the principal of an authenticated token, with the authorities `ROLE_CUSTOMER`, `ROLE_EXPERT` and `ROLE_ADMIN`. Controllers take it with `@AuthenticationPrincipal CurrentUser`. The roles are read from the database at login and kept for the session; `GET /api/me` reads name, email and roles fresh.
- **Like a real login.** The controller stores the authentication through Spring Security's `SecurityContextRepository` and changes the session id (session fixation), and the CSRF token changes too, as after a form login. OIDC (MVP) will end in the same session.
- **CSRF.** Spring Security's SPA support: the token is in the `XSRF-TOKEN` cookie, which JavaScript can read, and comes back in the `X-XSRF-TOKEN` header on every mutating request. The cookie must exist before the first mutating request; the SPA's first call, `GET /api/me`, gets it. Spring Security 7 has changed this setup since older examples; check the reference.
- **Logout.** Spring Security's logout at `POST /api/logout`: 204, the session is invalidated, and the session cookie is deleted. `/logout` is no longer special.
- **Session cookie:** `HttpOnly` and `SameSite=Lax` in `application.yml`; `Secure` comes with production (MVP).
- **`/api/poc/**`** is public in `SecurityConfig`. The controller exists only in the `poc` profile, so elsewhere the routes answer 404 ([security.md](../../architecture/security.md#roles-by-route-prefix)).

## Frontend

```
frontend/src
├─ api/client.ts       + middleware: X-XSRF-TOKEN from the cookie on mutating requests (01)
├─ app/                Layout: Log in, or the user's name and Log out (01); Consultations for admins (02);
│                      RequireRole (02); the QueryClient sends a 401 to the login (02)
├─ components/         ConfirmDialog (06), OfferingImage: the image or the placeholder (07)
├─ lib/                problem.ts: read a ProblemDetail (problemOf), map its errors to form fields (showSaveError) (03),
│                      the message for the user (problemMessage) (06);
│                      api-error.ts: ApiError carries the problem of a 400 or 409 (03), isConflict (06)
└─ features/
   ├─ user/            LoginPage, landing.ts (landingPath, safe redirect), queries.ts (useMe, usePocUsers, useLogin, useLogout)
   ├─ offering/        ConsultationOfferingsPage, NewConsultationOfferingPage, ConsultationOfferingPage,
   │                   ConsultationOfferingForm, OfferingStatusBadge (03), BackToConsultations (03),
   │                   OfferingStatusActions (06), OfferingImageSection (07), queries.ts (with toRequest: a saved offering as the form's values)
   └─ shop/            the card and the detail page show the image (07)
```

| Route | Page | Access | Task |
|---|---|---|---|
| `/login` | `LoginPage` | public | 01 |
| `/admin/consultations` | `ConsultationOfferingsPage` | admin | 02 |
| `/admin/consultations/new` | `NewConsultationOfferingPage` | admin | 03 |
| `/admin/consultations/:id` | `ConsultationOfferingPage` | admin | 03 |

### Auth in the SPA

- **`useMe()`**, query key `['me']`: the current user, or `null` for a visitor. A 401 means visitor, not an error.
- **Header:** **Shop**; **Consultations** when the user has `ADMIN` (02); on the right **Log in**, or the user's name and **Log out** (01). **Log in** opens `/login` without a `redirect`, so a login from the header opens the landing page (UC-01, spec scenario "An admin logs in").
- **`RequireRole`** (02) wraps the admin routes. While `useMe` loads, it shows nothing; a visitor goes to `/login?redirect=<path>`; a user without the role sees `NotFoundPage`. The backend stays the authority ([frontend.md](../../architecture/frontend.md#navigation-and-auth)).
- **After login** the SPA opens `redirect` if it's a local path (one leading `/`, not `//`), otherwise `landingPath(roles)`: `/admin/consultations` with `ADMIN` (02), else `/` (spec decision 3). In 01, `landingPath` always returns `/`.
- **After login and logout**, no cached data of the previous user remains.
- **Expired session** (02): a 401 from any query or mutation other than `useMe` refetches `['me']`, and `RequireRole` then sends the user to the login.

### Forms and mutations

- **React Hook Form** (added in 03). One `ConsultationOfferingForm` serves the new and the edit page. 04 adds the packages with `useFieldArray`, 05 the experts as checkboxes.
- **No rules in the form.** Inputs have their types (number, text), but the rules live in the backend: a 400's `errors` go to `setError` on each field (`packages[1].price` becomes `packages.1.price`), and a 400 without `errors` shows its `detail` above the form ([frontend.md](../../architecture/frontend.md#errors)).
- **Number inputs** use `valueAsNumber`. An empty one is `NaN`, which JSON sends as `null`, so the backend answers "must not be null" next to the field.
- **Query keys:** `['admin', 'consultation-offerings']` (list), `['admin', 'consultation-offerings', id]` (detail), `['admin', 'experts']`, `['poc', 'users']`.
- **Every offering mutation** invalidates `['admin', 'consultation-offerings']`, which includes every detail, and `['shop']`, so the shop shows the change at once.
- **A 409** shows its `detail` above the form and refetches the offering (06).

### Confirmations

Each status change opens a dialog (06), with the offering's name in the title and the consequence in the text ([UC-16](../../use-cases/uc-16-manage-consultation-offering.md#main-flow)):

| Action | Title | Text |
|---|---|---|
| Publish | Publish {name}? | It appears in the shop, and customers can buy it. |
| Unpublish | Unpublish {name}? | It disappears from the shop and becomes a draft again. |
| Archive | Archive {name}? | It disappears from the shop for good and can't be changed any more. Customers who bought it keep their cases. |
| Delete | Delete {name}? | The offering is deleted for good. This can't be undone. |

### Components

shadcn/ui components are added when a task first uses them ([frontend.md](../../architecture/frontend.md#structure)), probably `table` (02), `input`, `textarea` and `label` (03), `checkbox` (05) and `alert-dialog` (06).

## Tests

| What | How |
|---|---|
| Logged-in requests | 01 adds a test helper, `LoggedIn.as(userId, roles…)`: a `RequestPostProcessor` with the same principal a PoC login creates. REST tests of admin routes log in as Admin 1. `RoleMatrixTest`'s callers use it too, so a controller that needs `CurrentUser` doesn't fail there. |
| CSRF in tests | A mutating request uses `.with(Csrf.token())` (01): the `XSRF-TOKEN` cookie and the `X-XSRF-TOKEN` header, as the SPA sends them. Not Spring Security Test's `csrf()`, which swaps the CSRF filter's token repository for a session-based one in the shared Spring context ([security](../../architecture/security.md#testing)). |
| Test data | Tests never change seed rows. A writing test creates its own offering, through the API or with `@Sql`, and one shared script, run with `@Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)`, deletes every offering that isn't in the seed, with its packages, eligible experts and (07) image. 03 added it as `delete-test-offerings.sql`, in place of the shop tests' `delete-extra-offerings.sql`; 07 adds the image. |
| Role matrix | A row per new route. An allowed caller must not change data, so a mutating row targets an unknown id (security passes, then 404) or sends no body (400, or 415 for the multipart upload). |
| Upload limit | MockMvc builds multipart requests itself and never meets the servlet container's limit. `@IntegrationTest` also starts the server on a random port, and `ImageUploadLimitTest` uploads over HTTP, logged in through the PoC login: 2 MB passes, one byte more answers 400 (07). |
| Concurrency | One test for the offering lock (06): two status changes on one offering, one succeeds and the other gets 409. It must fail without the lock. |
| Domain | Every cell of the status table, publishing completeness, matching packages by id, `ImageType` detection, `Image` size. |
| Frontend | Vitest for logic only ([D-29](../../decisions.md#architecture)): `landingPath` and the redirect check, the CSRF header, `lib/problem.ts`. Pages are checked by hand with Playwright. |

## Commands

No new commands. A task that adds one also adds it to the **Commands** section of [AGENTS.md](../../../AGENTS.md#commands).
