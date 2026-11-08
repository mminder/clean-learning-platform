# 03 Create and edit a consultation offering

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [02](02-consultation-offering-list.md) |
| Status | Todo |

## Goal

An admin creates a draft consultation offering and edits its name, description, session duration and cancellation deadline in one form. Invalid values come back as 400 with a field list and show next to their fields. This task sets the pattern for every later writing use case and form, so follow the plan closely.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) steps 2–4, [backend.md](../../architecture/backend.md), [frontend.md](../../architecture/frontend.md), [D-34](../../decisions.md#architecture), and the plan's [API](README.md#api), [errors](README.md#errors), [writes](README.md#writes), [forms and mutations](README.md#forms-and-mutations) and [tests](README.md#tests) sections.

Packages and eligible experts aren't part of this task: the request and the response leave them out, and `PUT` leaves them untouched.

Work in this order, test first:

1. **A failing REST test:** `POST /api/admin/consultation-offerings` with a valid request answers 201 with a `Location`, and `GET` on that location returns the offering as `DRAFT`.
2. **Schema:** `V4__set_updated_at.sql` as in the plan's [schema](README.md#schema). Add the rule to [backend.md](../../architecture/backend.md#persistence).
3. **Domain**, test first: `ConsultationOffering` with id, name, description, session duration, cancellation deadline and status. A factory creates it as `DRAFT` with a new id; `edit(...)` returns it with new values.
4. **Ports and adapter:** `LoadConsultationOfferingPort`, `LockConsultationOfferingPort` and `SaveConsultationOfferingPort` (`create`, `update`) in `ConsultationOfferingAdapter`. Writes follow the plan's [writes](README.md#writes) section: new rows are persisted, the entities gain what writing needs, and the lock is `select … for update`.
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

- [ ] On the dev server, Admin 1 creates "Agile Coaching" with 45 minutes and 48 hours: its page opens as a draft, the list shows it, and the shop doesn't (Playwright).
- [ ] Saving a blank name, a duration of 0, a deadline of −1, or an empty number field answers 400 with each field in `errors`, and the form shows each message next to its field. An empty description is saved.
- [ ] A name of only no-break spaces answers 400, not 500.
- [ ] `PUT` on a published offering changes its name in the shop at once (REST test on an offering the test inserts).
- [ ] `updated_at` changes on an update, and `created_at` doesn't.
- [ ] `GET` and `PUT` with an unknown id answer 404, and with a malformed id 400, as `ProblemDetail`.
- [ ] `openapi.json` has the `ProblemDetail` schema with `errors`, and the frontend reads 400 bodies only through its generated type.
- [ ] No writing test changes a seed row, and the cleanup script runs after each one.
- [ ] The role matrix has the three routes, and every check passes.

## Tests

- Domain: `ConsultationOffering` (a new one is a draft, an edit keeps id and status)
- REST: create, get, edit; each invalid value; unknown and malformed ids; the shop shows an edit at once
- Authorization: `RoleMatrixTest` rows for `POST`, `GET {id}` and `PUT {id}`
- Frontend: `lib/problem.ts`

## Out of scope

- Packages ([04](04-packages.md)), eligible experts ([05](05-eligible-experts.md)), status changes and the archived rule ([06](06-offering-lifecycle.md)), the image ([07](07-offering-image.md)).
- Validation in the browser beyond input types: the backend's 400 is the one source of the rules.

## Notes
