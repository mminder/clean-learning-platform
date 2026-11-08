# 02 Manage consultation offerings

| | |
|---|---|
| Status | Done |
| Use cases | [UC-01](../use-cases/uc-01-login.md), PoC main flow and log out; [UC-16](../use-cases/uc-16-manage-consultation-offering.md), all steps |
| Tasks | [tasks/02-manage-consultation-offerings](../tasks/02-manage-consultation-offerings/README.md) |

## Why

Increment 01 proved the read path: the shop shows what the seed holds. Nobody can add or change an offering yet, so the platform can only ever sell Career Coaching.

UC-16 is the first writing use case, and a good one to start with. It brings what every later writing use case needs, without money, dates or another user's data in the way:

- a logged-in user with a role, which needs UC-01's PoC login, and CSRF protection on mutating requests;
- request validation that answers 400 with a field list, shown next to the form fields;
- business rules that answer 409 with a stable code;
- a row lock on a contended write ([D-27](../decisions.md#architecture));
- forms with React Hook Form, mutations and cache invalidation;
- image upload and serving ([D-21](../decisions.md#cross-cutting)).

Later increments copy these patterns, as increment 01's tasks copied the shop list.

## What changes

- A visitor logs in by picking one of the six seeded users, and logs out from the header. The header shows who is logged in and, for admins, a **Consultations** link.
- An admin sees all consultation offerings in every status, with their number of packages and eligible experts.
- An admin creates a draft offering and edits its name, description, session duration, cancellation deadline, packages, eligible experts and image.
- An admin publishes, unpublishes, archives and deletes offerings, each after a confirmation. The shop follows at once.
- The shop shows an offering's image instead of the placeholder when it has one.

## User stories

1. As a visitor, I want to log in as one of the seeded users, so that I can try every role in the PoC.
2. As a user, I want to see who I'm logged in as and log out, so that I can switch users.
3. As an admin, I want to see every consultation offering with its status, so that I know what's in the shop and what's being prepared.
4. As an admin, I want to create an offering as a draft, so that I can prepare it before customers see it.
5. As an admin, I want to set an offering's packages and eligible experts, so that customers can buy the right number of sessions and pick a fitting expert.
6. As an admin, I want to upload an image for an offering, so that it stands out in the shop.
7. As an admin, I want publishing to refuse an incomplete offering, so that nothing half-built reaches the shop.
8. As an admin, I want to unpublish, archive or delete an offering, so that the shop only sells what we still offer.
9. As an admin, I want to confirm every status change, so that a misclick doesn't change the shop.
10. As a developer, I want one finished example of a writing use case, with validation, conflicts, a lock and a form, so that later use cases follow one pattern.

## Requirements

### Requirement: Log in with the user picker

UC-01's PoC main flow and log out. The PoC login exists only in the `poc` profile ([D-25](../decisions.md#architecture)). Until the landing pages of UC-11, UC-18 and UC-05 exist, a user with `ADMIN` lands on **Consultations** and everyone else on the shop (decision 3).

#### Scenario: An admin logs in

- **GIVEN** the `poc` seed
- **WHEN** a visitor clicks **Log in** and picks Admin 1
- **THEN** the consultation offerings list opens, and the header shows "Admin 1", **Consultations** and **Log out**

#### Scenario: A customer logs in

- **WHEN** a visitor logs in as Customer 1
- **THEN** the shop opens, and the header shows "Customer 1" and **Log out**, but not **Consultations**

#### Scenario: Login from a protected page

- **WHEN** a visitor opens `/admin/consultations`
- **THEN** the login page opens, and after logging in as Admin 1 the visitor is back on `/admin/consultations`

#### Scenario: The current user

- **WHEN** a logged-in user calls `GET /api/me`
- **THEN** the response holds the user's id, name, email and roles
- **AND** without a login, the response is 401

#### Scenario: Log out

- **WHEN** a logged-in user clicks **Log out**
- **THEN** the shop opens, the header shows **Log in**, and `GET /api/me` answers 401

#### Scenario: CSRF

- **WHEN** a mutating request under `/api` carries no CSRF token
- **THEN** the response is 403
- **AND** the SPA's own requests pass, including the first login in a fresh browser

#### Scenario: No PoC login without the poc profile

- **WHEN** the `poc` profile isn't active
- **THEN** `GET /api/poc/users` and `POST /api/poc/login` answer 404

### Requirement: List consultation offerings

UC-16 step 1: every consultation offering in every status, sorted by name.

#### Scenario: An admin sees every status

- **GIVEN** Career Coaching, a draft and an archived offering
- **WHEN** Admin 1 opens **Consultations**
- **THEN** the list shows all three, sorted by name, each with its status, its number of packages and its number of eligible experts
- **AND** Career Coaching shows Published, 2 packages and 2 experts

#### Scenario: Only admins

- **WHEN** Customer 1 opens `/admin/consultations`
- **THEN** the SPA shows "Page not found", and `GET /api/admin/consultation-offerings` answers 403

### Requirement: Create and edit an offering

UC-16 steps 2–4. One form and one **Save**: `POST` creates the offering, and `PUT` replaces its name, description, session duration, cancellation deadline, packages and eligible experts (decision 6). The image has its own requirement below.

#### Scenario: Create a draft

- **WHEN** Admin 1 creates "Agile Coaching" with 45-minute sessions, a 48 h deadline, the packages 1 session for CHF 160.00 and 4 sessions for CHF 600.00, and Expert 2
- **THEN** it's saved as `DRAFT`, appears in the list, and isn't in the shop

#### Scenario: Invalid values

- **WHEN** the admin saves a blank name, a session duration of 0, a deadline of −1, a package with 0 sessions, or a price of −1 or 12.345
- **THEN** the response is 400 with a `ProblemDetail` that names each invalid field, and the form shows each message next to its field
- **AND** an empty description is accepted

#### Scenario: Edits are live

- **GIVEN** a published offering
- **WHEN** the admin changes its name and a package price and saves
- **THEN** the shop shows the new name and price at once

#### Scenario: Packages keep their identity

- **WHEN** the admin changes one package, removes another, adds a third and saves
- **THEN** the changed package keeps its id, the removed one is gone, and the new one gets a new id

#### Scenario: Removing the last package

- **GIVEN** a published offering
- **WHEN** the admin removes all its packages and saves
- **THEN** the save succeeds, and the offering disappears from the shop until it has a package again ([UC-16](../use-cases/uc-16-manage-consultation-offering.md#business-rules))

#### Scenario: Removing every eligible expert

- **GIVEN** a published offering
- **WHEN** the admin removes all its eligible experts and saves
- **THEN** the save succeeds, and the offering stays in the shop

#### Scenario: Eligible experts are users with EXPERT

- **WHEN** the admin opens the form
- **THEN** it offers Expert 1 and Expert 2 to choose from
- **AND** a request that names a user without `EXPERT` answers 400

### Requirement: Offering image

UC-16 step 3, following [D-21](../decisions.md#cross-cutting). The image is uploaded, replaced or removed at once, apart from **Save** (decision 6), so a new offering gets its image after its first save.

#### Scenario: Upload

- **WHEN** the admin uploads a PNG for Agile Coaching
- **THEN** the form shows it, and once the offering is published, the shop shows it on the card and the detail page

#### Scenario: Replace and remove

- **WHEN** the admin replaces the image
- **THEN** the new image is served under a new id, and the old id answers 404
- **AND** after **Remove image**, the shop shows the placeholder again

#### Scenario: Invalid image

- **WHEN** the admin uploads a GIF, a text file named `photo.png`, or a JPEG larger than 2 MB
- **THEN** the response is 400, and the offering keeps its image

#### Scenario: Served to everyone, cached for good

- **WHEN** a visitor's browser calls `GET /api/images/{id}`
- **THEN** the response has the image's content type and a `Cache-Control` header with a max-age of one year

### Requirement: Publish, unpublish, archive and delete

UC-16 step 5 and its business rules. Each action asks for confirmation first and states its consequence (decision 4).

#### Scenario: Publish

- **GIVEN** a draft with a package and an eligible expert
- **WHEN** the admin clicks **Publish** and confirms
- **THEN** the offering is `PUBLISHED` and in the shop

#### Scenario: Publishing an incomplete offering

- **GIVEN** a draft without packages or without eligible experts
- **WHEN** the admin publishes it
- **THEN** the response is 409 with code `OFFERING_INCOMPLETE`, and the page shows what's missing

#### Scenario: Unpublish

- **WHEN** the admin unpublishes a published offering
- **THEN** it's a `DRAFT` again and gone from the shop

#### Scenario: Archive

- **WHEN** the admin archives a published offering
- **THEN** it's `ARCHIVED`, gone from the shop, and its page is read-only
- **AND** a `PUT`, an image change or any status change on it answers 409 with code `WRONG_OFFERING_STATUS`

#### Scenario: Delete

- **WHEN** the admin deletes a draft
- **THEN** it's gone from the list, and its id answers 404

#### Scenario: A change the status doesn't allow

- **WHEN** a stale page deletes a published offering, or publishes one that is already published
- **THEN** the response is 409 with code `WRONG_OFFERING_STATUS`, and the page shows the message and reloads the offering

#### Scenario: Two admins at once

- **GIVEN** a published offering
- **WHEN** one admin archives it while another unpublishes it
- **THEN** exactly one of them succeeds, and the other gets 409

#### Scenario: The admin changes their mind

- **WHEN** the admin cancels a confirmation dialog
- **THEN** nothing changes

### Requirement: Admin routes are for admins

Every new route has a row in the role matrix ([security](../architecture/security.md#testing)).

#### Scenario: Wrong role or no login

- **WHEN** a customer or an expert without `ADMIN` calls a route under `/api/admin`
- **THEN** the response is 403, and 401 without a login

#### Scenario: Images are public

- **WHEN** a visitor calls `GET /api/images/{id}`
- **THEN** the request passes security

### Requirement: Error bodies are part of the contract

[D-34](../decisions.md#architecture): the first error body the frontend reads brings one shared `ProblemDetail` schema into `openapi.json`.

#### Scenario: The frontend reads 400 and 409 through generated types

- **WHEN** the form shows a field error or a page shows a conflict
- **THEN** the frontend reads the body as the generated `ProblemDetail` type, with `errors` on a 400 and `code` on a 409

## Decisions

Agreed with the product owner while planning (2026-11-08). The ones marked *planning* were taken while writing the plan, following the docs; object to them before the first task starts.

1. **PoC login first.** Admin pages need a logged-in admin, so task 01 builds UC-01's PoC flow (user picker, `GET /api/me`, logout) and the CSRF cookie setup that spec 01 deferred to the first mutating request.
2. **Images are in**, as the last task, following D-21. Expert photos stay with [UC-14](../use-cases/uc-14-manage-expert-profile.md); they'll reuse the `image` table, `ImageStorePort` and `GET /api/images/{id}`.
3. **Landing page until the real ones exist:** a user with `ADMIN` lands on **Consultations**, everyone else on the shop. UC-01's priority (Expert → Admin → Customer) takes over as UC-11, UC-18 and UC-05 add their pages. A login started from a protected page returns to it.
4. **Every status change is confirmed.** Publish, Unpublish, Archive and Delete each open a dialog that states the consequence. UC-16 and the [cross-cutting rules](../use-cases/README.md#cross-cutting-rules) now say so.
5. **No purchase guard yet** (*planning*). Unpublish and delete are allowed only without purchases ([D-07](../decisions.md#catalog)), but purchases don't exist before [UC-03](../use-cases/uc-03-purchase-consultation-package.md). The UC-03 increment adds the check, with its 409 `OFFERING_HAS_PURCHASES`, together with the case table. Unpublishing requires no purchases, so a draft never has any; delete's check will only be a safety net.
6. **One form, one save** (*planning*). `PUT` carries the whole offering, packages and eligible experts included, as UC-16's "the admin saves" and [frontend.md](../architecture/frontend.md#stack)'s `useFieldArray` suggest. Packages are matched by id, so an edit keeps the ids that UC-03 will buy by. The image is the exception: a file doesn't fit a JSON form, so it has its own routes and takes effect at once.
7. **Delete is for drafts only** (*planning*), as UC-16's diagram shows. A published offering is unpublished first, or archived.
8. **Concurrent edits: the last save wins** (*planning*). Every change locks the offering row first (D-27, extended), so two status changes can't both pass and an archived offering stays read-only. There's no optimistic version check: two admins saving the same form at the same moment is unlikely in the PoC.
9. **The server writes the 409 message** (*planning*). A 409's `detail` is written for the user, and the frontend shows it; `code` is stable, for code that reacts to one conflict and for tests (D-34, extended). UC-16's "Offering has purchases. Archive it instead." is such a `detail`.
10. **Packages may share a session count** (*planning*). UC-16 has no rule against it, and none is added.
11. **Executed as in [D-33](../decisions.md#scope-and-docs):** one implementing subagent per task, then a review subagent, all on one pull request. The tasks build one form on one aggregate and touch the same files, so they run one after another.

## Out of scope

- The purchase guard on unpublish and delete: with UC-03 (decision 5).
- Training offerings: [UC-17](../use-cases/uc-17-manage-training-offering.md).
- Expert photos and the Teams link: [UC-14](../use-cases/uc-14-manage-expert-profile.md).
- The landing pages "My cases", "Cases" and "My purchases": UC-11, UC-18 and UC-05.
- OIDC login, Keycloak and user management: MVP ([UC-19](../use-cases/uc-19-manage-users.md)).
- **Buy**: UC-03.
- Optimistic locking (decision 8).
- Offering categories, scheduled publishing, price history and discounts ([UC-16](../use-cases/uc-16-manage-consultation-offering.md#out-of-scope)).
- Resizing or cropping images ([D-21](../decisions.md#cross-cutting)).

## Demo

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=poc
```

```bash
cd frontend && npm run dev
```

1. Open `http://localhost:5173/admin/consultations`: the login page opens. Pick Admin 1: the list shows Career Coaching as Published, with 2 packages and 2 experts.
2. Click **New offering**, leave the name empty and save: a message appears next to the name.
3. Enter "Agile Coaching", a description, 45 minutes and 48 hours, add the packages 1 × CHF 160 and 4 × CHF 600, choose Expert 2, and save: the offering opens as a draft.
4. Upload a PNG: the page shows it.
5. Click **Publish** and confirm. Open the shop: Agile Coaching shows its image and "from CHF 160.00", before Career Coaching.
6. Back on Agile Coaching, **Unpublish** and confirm: the shop no longer shows it. **Publish** again, then **Archive**: the page is read-only, and the shop doesn't show it.
7. Create "Test" without packages and click **Publish**: the page says what's missing. **Delete** it and confirm: it's gone from the list.
8. **Log out**, log in as Customer 1 and open `/admin/consultations`: "Page not found".

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

9. Open `http://localhost:8080`, log in as Admin 2, and repeat steps 3 to 5 with another name.

## Risks

| Risk | Mitigation |
|---|---|
| Spring Security 7's session handling and SPA CSRF support differ from older examples, and a wrong setup often shows only in a real browser. | Task 01 checks the current reference docs. Its acceptance criteria include the first login in a fresh browser, checked with Playwright. |
| Tests now write to the shared test database, so one test's rows can break another's assertions. | The plan's [test data](../tasks/02-manage-consultation-offerings/README.md#tests) rules: tests never change seed rows, and one cleanup script removes every non-seed offering after each writing test. |
| Role matrix rows for mutating routes could change the seed. | Those rows target an unknown id or send no body, so an allowed caller stops at 404 or 400. |
| Writing through JPA with ids set by the application: Spring Data's `save()` merges, with a select before each insert. | The plan says how adapters write ([writes](../tasks/02-manage-consultation-offerings/README.md#writes)). |
| An edit could give packages new ids and break UC-03, which buys by package id. | Packages are matched by id. A REST test checks that ids survive an edit. |
| Spring's 1 MB multipart limit and Spring Security's `no-cache` headers work against D-21. | Task 07 names both and tests the limit and the headers. |
| Seven tasks in a row on one form and one aggregate take longer than parallel waves. | Each task is a thin slice, reviewed before the next one starts. |
