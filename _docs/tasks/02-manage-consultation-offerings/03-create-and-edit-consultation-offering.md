# 03 Create and edit a consultation offering

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [02](02-consultation-offering-list.md) |
| Status | Done |

## Goal

An admin creates a draft consultation offering and edits its name, description, session duration and cancellation deadline in one form. Invalid values come back as 400 with a field list and show next to their fields. This task sets the pattern for every later writing use case and form, so follow the plan closely.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) steps 2–4, [backend.md](../../architecture/backend.md), [frontend.md](../../architecture/frontend.md), [D-34](../../decisions.md#architecture), and the plan's [API](README.md#api), [errors](README.md#errors), [writes](README.md#writes), [forms and mutations](README.md#forms-and-mutations) and [tests](README.md#tests) sections.

Packages and eligible experts aren't part of this task: the request and the response leave them out, and `PUT` leaves them untouched.

Work in this order, test first:

1. **A failing REST test:** `POST /api/admin/consultation-offerings` with a valid request answers 201 with a `Location`, and `GET` on that location returns the offering as `DRAFT`.
2. **Schema:** `V4__set_updated_at.sql` as in the plan's [schema](README.md#schema). Add the rule to [backend.md](../../architecture/backend.md#persistence).
3. **Domain**, test first: `ConsultationOffering` with id, name, description, session duration, cancellation deadline and status. A factory creates it as `DRAFT` with a new id; `edit(...)` returns it with new values.
4. **Ports and adapter:** `LoadConsultationOfferingPort`, `LockConsultationOfferingPort` and `SaveConsultationOfferingPort` (`create`, `update`) in `ConsultationOfferingAdapter`. Writes follow the plan's [writes](README.md#writes) section: new rows are persisted, the entities gain what writing needs, and the lock is a row lock on the `offering` row (see the note on the lock).
5. **Use cases:** `CreateConsultationOfferingUseCase`, `GetConsultationOfferingUseCase` (`NotFoundException` for an unknown id) and `EditConsultationOfferingUseCase`, which locks first.
6. **API:**
   - `ConsultationOfferingRequest` with Bean Validation for the UC-16 rules (`@NotBlank` name, `@NotNull` description, `@NotNull @Positive` duration, `@NotNull @PositiveOrZero` deadline), and `ConsultationOfferingResponse` without `packages`, `eligibleExpertIds` and `imageId`;
   - the three routes on `ConsultationOfferingController`, with 201 and `Location` for `POST`;
   - `ApiExceptionHandler`: the 400 with `errors` for Bean Validation, and `IllegalArgumentException` → 400, as in the plan's [errors](README.md#errors) section;
   - the shared `ProblemDetail` schema in `openapi.json`, with `errors` ([D-34](../../decisions.md#architecture));
   - role matrix rows, following the plan's rule for mutating rows.
7. **Test data:** the shared cleanup script from the plan's [tests](README.md#tests).
8. **Frontend:**
   - `react-hook-form`, and the shadcn/ui components the form needs;
   - `lib/problem.ts`: read a 400 or 409 body through the generated `ProblemDetail` type, and turn `errors` into form paths (`packages[1].price` → `packages.1.price`), with a Vitest test;
   - `ConsultationOfferingForm`: name, description (a textarea), session duration in minutes, cancellation deadline in hours;
   - `NewConsultationOfferingPage` at `/admin/consultations/new`: **Save** creates the offering and opens its page;
   - `ConsultationOfferingPage` at `/admin/consultations/:id`: the name, a status badge and the filled-in form; **Save** sends `PUT` and says "Saved"; a 404, or a 400 for a malformed id, shows "Offering not found" with a link back to the list, as the shop's detail does;
   - on the list: each name links to its page, and a **New offering** button opens the new page;
   - mutations invalidate the queries named in the plan.
9. **Docs:** [backend.md](../../architecture/backend.md#errors) gets the 400 rules from the plan; [frontend.md](../../architecture/frontend.md) gets the form pattern if it adds something the doc doesn't say yet.

## Acceptance criteria

- [x] On the dev server, Admin 1 creates "Agile Coaching" with 45 minutes and 48 hours: its page opens as a draft, the list shows it, and the shop doesn't (Playwright).
- [x] Saving a blank name, a duration of 0, a deadline of −1, or an empty number field answers 400 with each field in `errors`, and the form shows each message next to its field. An empty description is saved.
- [x] A name of only no-break spaces answers 400, not 500.
- [x] `PUT` on a published offering changes its name in the shop at once (REST test on an offering the test inserts).
- [x] `updated_at` changes on an update, and `created_at` doesn't.
- [x] `GET` and `PUT` with an unknown id answer 404, and with a malformed id 400, as `ProblemDetail`.
- [x] `openapi.json` has the `ProblemDetail` schema with `errors`, and the frontend reads 400 bodies only through its generated type.
- [x] No writing test changes a seed row, and the cleanup script runs after each one.
- [x] The role matrix has the three routes, and every check passes.

## Tests

- Domain: `ConsultationOffering` (a new one is a draft, an edit keeps id and status)
- REST: create, get, edit; each invalid value; unknown and malformed ids; the shop shows an edit at once
- Authorization: `RoleMatrixTest` rows for `POST`, `GET {id}` and `PUT {id}`
- Frontend: `lib/problem.ts`

## Out of scope

- Packages ([04](04-packages.md)), eligible experts ([05](05-eligible-experts.md)), status changes and the archived rule ([06](06-offering-lifecycle.md)), the image ([07](07-offering-image.md)).
- Validation in the browser beyond input types: the backend's 400 is the one source of the rules.

## Notes

- **`ProblemDetail` in `openapi.json`:** `ApiExceptionHandler`'s methods carry `@ResponseStatus` (404 for `NotFoundException`, 400 for `IllegalArgumentException`), so springdoc lists both answers on **every** route, as `application/problem+json`. The schema springdoc derives from Spring's `ProblemDetail` class has a `properties` map and no `errors`, so `ProblemDetailSchemaCustomizer` replaces it with the body as sent: `title`, `status`, `instance` required; `detail`, `type` and `errors` optional. 06 adds `code` there, and its 409 handler gets `@ResponseStatus(CONFLICT)`. Listing 400 and 404 on routes that never give them (`GET /api/me`) is the price of springdoc's own mechanism; the alternative was per-method annotations. backend.md says so. Changed in the overall review: each route now declares the error answers it can give, with `ProblemResponses` ([backend.md](../../architecture/backend.md#errors)), and the advice's methods carry no `@ResponseStatus`.
- **`"type": "about:blank"`** (task 01's note): Spring leaves it out, so the schema's `type` is optional, and the plan's examples no longer show it. The REST tests compare whole bodies strictly, so they'd notice a change.
- **Messages in English** (found while testing): Spring Boot interpolates Bean Validation messages in the request's locale, so a browser sending `Accept-Language: de-CH` got "darf nicht leer sein" next to an English form. `application.yml` now fixes the locale (`spring.web.locale: en`, `locale-resolver: fixed`, D-20); a REST test sends `de-CH`, and it fails without the setting.
- **`errors` are sorted by field path,** so the body is stable. Because of that, the form doesn't focus "the first" invalid field: the first by path isn't the first in the form (tried, removed).
- **The lock** is `EntityManager.find(OfferingEntity, id, PESSIMISTIC_WRITE)`. Hibernate 7 sends it to Postgres as `select … for no key update of …` (checked in the SQL log), not `for update`: it waits for every other lock of the row, which is what D-27 needs, but not for a foreign key check's key-share lock. The plan's writes section and backend.md's concurrency section say so. Worth a look in 06, whose concurrency test is the first that relies on it.
- **Writes:** the adapter persists new entities with `EntityManager.persist` (`offering` first, then `consultation_offering`), and `update` sets the managed entities' fields, which the lock loaded in the same transaction; no `save()`. `OfferingEntity` and `ConsultationOfferingEntity` gained a constructor, getters and setters. backend.md's persistence section has the rule.
- **`ConsultationOfferingCommand`** (in `application.offering`) carries the form's values, with value objects, for both use cases; the controller builds it, so a value object's `IllegalArgumentException` (a name of only no-break spaces) answers 400 before the use case runs. 04 and 05 add packages and expert ids to it. The plan's package table lists it.
- **201 in the contract:** `ResponseEntity.created` sets 201 at runtime, but springdoc documented 200, so `createConsultationOffering` also carries `@ResponseStatus(CREATED)`. The `Location` is relative (`/api/admin/consultation-offerings/{id}`), so it's right behind the Vite proxy, too.
- **operationIds:** `list` became `listConsultationOfferings`; the new ones are `createConsultationOffering`, `getConsultationOffering` and `editConsultationOffering`. The shop's `listOfferings` and `getOffering` are unique and stay.
- **`updated_at`:** `V4__set_updated_at.sql` puts the trigger on all seven tables. `UpdatedAtTriggersTest` checks that every table has it, so 07's `image` table can't forget it. The REST test checks both rows of an edited offering.
- **Test data:** `delete-test-offerings.sql` (`SeedData.DELETE_TEST_OFFERINGS`) deletes every offering but Career Coaching, with its packages and eligible experts. It replaced `delete-extra-offerings.sql` in the shop and list tests. `delete-more-experts-and-packages.sql` stays: it also removes users and a package of the seeded offering. Every test that sends `POST` or `PUT` with a body that could be saved runs the cleanup.
- **Frontend:** `ApiError` gained `problem`; mutation hooks pass `problemOf(response.status, error)`, which keeps only a 400's or 409's body, since openapi-fetch types every error body as `ProblemDetail` though a 403's isn't. `showSaveError` (`lib/problem.ts`, with tests) is the form glue every later form can reuse: `setError` per field, or a message for above the form. The form uses the generated `ConsultationOfferingRequest` as its values; a new form starts with `NaN` in the number fields. Number inputs keep the browser's own check of their type (whole numbers, from the default `step`), but have no `min`. The status badge moved into `OfferingStatusBadge`, shared by the list and the offering page. shadcn: `input`, `textarea`, `label`. frontend.md has a new **Forms** section.
- **Browser check** (Playwright, dev server, `poc` backend, fresh browser): a visitor at `/admin/consultations` logs in as Admin 1 and gets the list; **New offering**, then **Save** with everything empty: "must not be blank" next to the name and "must not be null" next to both number fields, none next to the description. Duration 0 and deadline −1: "must be greater than 0" and "must be greater than or equal to 0" next to them. Agile Coaching with a two-line description, 45 and 48: its page opens with a Draft badge and the filled-in form; the list shows it as Draft, the shop doesn't. Renaming it shows "Saved" and the new heading; "Saved" goes away with the next change. A name of two no-break spaces shows "An offering name is not blank" above the form. An emptied duration on the edit page shows "must not be null". The list follows the rename. Career Coaching renamed shows in the shop at once (renamed back). An unknown and a malformed id show "Offering not found" with a link back. The only API answers ≥ 400 were the first `GET /api/me` (401) and the intended 400s and 404; no 403. The dev database was cleaned up afterwards.
- **A decimal number answers 400** (decided in review). Jackson's `ACCEPT_FLOAT_AS_INT` is on by default, so `"sessionDurationMinutes": 45.5` was saved as 45. The form can't send it, because the browser blocks 45.5 in a number input, but the API is the contract, and a silently changed value is worse than a missing one. `application.yml` turns it off API-wide (`spring.jackson.deserialization.accept-float-as-int: false`): the answer is Spring's 400 "Failed to read request" without `errors`, which the form would show above itself. A REST test covers it; the plan's API section and backend.md say so.
