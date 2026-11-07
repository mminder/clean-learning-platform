# Decision log

The design decisions agreed on 2026-11-07, when the [initial spec](archive/initial-spec.md) was refined. Each entry gives the decision and the reason for it. When a decision changes, edit its entry and note the date.

## Access and roles

- **D-01 Public shop.** Visitors can browse the shop and offering details without logging in; buying requires login. Customers are always individuals buying for themselves (no B2B seats). *Why:* this is how a public internet shop works, and it costs nothing extra.
- **D-02 Multiple roles per user.** A user can hold any combination of `CUSTOMER`, `EXPERT` and `ADMIN`; Admin + Expert is the typical case. *Why:* small organisation, one person often has several jobs.
- **D-03 Union navigation.** The header shows a section for every role the user holds; there's no role switcher. The landing page follows the priority Expert → Admin → Customer. Every role combination is allowed. *Why:* no mode switching, and nothing special in the backend.
- **D-04 Roles live in our DB.** In the MVP, a first OIDC login creates the user with `CUSTOMER` (just-in-time provisioning), and an admin grants or revokes `EXPERT`/`ADMIN` ([UC-19](use-cases/uc-19-manage-users.md)). The PoC has seeded users only. *Why:* the IdP only answers "who is this"; authorization stays inside the app and testable there.

## Catalog

- **D-05 Packages and prices.** A consultation offering has packages (session count + price). A training has one price for all its runs. "Buy" on a card opens the same picker-plus-confirmation dialog as on the detail page. Prices are CHF including VAT, modelled as a `Money` value object. *Why:* matches "session counts" in the spec, and treats packages and runs alike.
- **D-06 Eligible experts.** The admin assigns eligible experts to each consultation offering, and the customer picks from them on the first booking. An expert who is also a customer may pick themself. *Why:* a career coach shouldn't appear in an architecture consultation. Self-selection is handy for testing, and it replaces an earlier "not your own case" guard.
- **D-07 Offering lifecycle.** `DRAFT → PUBLISHED → ARCHIVED`. Only published offerings appear in the shop. Publishing checks completeness. Unpublish (back to draft) and hard delete are allowed only without purchases; otherwise the admin archives. A run can be deleted only without enrollments. *Why:* no half-built offerings in the shop, and purchased data is never destroyed.
- **D-08 Purchase terms and run schedules.** At purchase, the terms are copied into the case or enrollment (price paid; for consultations also session count, session duration and cancellation deadline). Creating a run copies the day template into a concrete dated schedule. A run's dates are locked once anyone is enrolled. Name, description and image are always read live. *Why:* customers get what they bought, and later edits never rewrite history.

## Consultation

- **D-09 Availability.** An expert selects calendar dates plus one set of daily time ranges that applies to all of them. Free time = availability − booked appointments − time slots of assigned runs. Removing availability never cancels appointments. Assigning an expert to a run is blocked if it overlaps the expert's busy time. *Why:* matches "calendar" in the spec, handles vacations without exception logic, and prevents double-booking.
- **D-10 Slot generation.** Start times on a 30-minute grid, no buffer between sessions, at least 24 h notice, and any number of bookings up to the open credits. These are constants in one domain class. *Why:* simple, and easy to test and change.
- **D-11 Cancellation deadline and consumption.** The cancellation deadline is a duration in hours configured per offering (examples: 24 h for career coaching, 168 h for trainings). Cancellation is free before `start − deadline`. An appointment is consumed once its start has passed, and attendance isn't tracked. Open credits = total − booked − forfeited, always computed. The current time comes from an injected `Clock`. *Why:* clear definitions for the refund rules, and no counter that can drift.
- **D-12 Case lifecycle.** `ACTIVE` and `CANCELLED` are stored, and `COMPLETED` is derived. Credits never expire. Cancelling an active case is always allowed, with a warning when it's not refund-eligible. Refund-eligible means no consumed appointment, no forfeited credit, and every future booked appointment still before its deadline. *Why:* the same "cancel with warning" model as for appointments.
- **D-13 Goodwill credit adjustments.** An admin adds adjustment entries: a ± delta, a mandatory reason, the admin, and a timestamp. Open credits can't drop below 0. Cancelled cases can't be adjusted. *Why:* an audit trail for support cases at almost no cost.
- **D-14 Notes.** Notes belong to the case and are plain text. The assigned expert can create, edit and delete them and switch between private and public. The customer sees public notes; the admin sees no notes. A case appears in an expert's list only after the first booking. *Why:* the spec's privacy rule, made explicit for every role.

## Training

- **D-15 Buying a run.** A run can be bought until its first time slot starts while seats are free, and a customer can enroll in a run only once. The shop shows "N seats left" or "Sold out". Seats are set per run, with the default taken from the training, and can't be set below the enrolled count. Selling doesn't require an assigned expert ("to be announced"). A cancellation frees the seat. *Why:* simple and predictable; no waitlist.
- **D-16 Cancelling an enrollment.** Allowed until the run starts. It's refund-eligible only before the deadline, which is measured from the first time slot. `ENROLLED` and `CANCELLED` are stored, and `COMPLETED` is derived. Cancelling a whole run is out of scope. *Why:* mirrors the consultation rules; a started run can't be resold anyway.

## Views and calls

- **D-17 List ordering.** "My …" lists are grouped into Upcoming (by next date), then Needs booking (active case with open credits and nothing booked), then Past (completed or cancelled, collapsed). Expert trainings are sorted by next run date. *Why:* unbooked cases don't get lost.
- **D-18 What experts see.** An expert sees only their own runs, with participants (name and email). On cases, the assigned expert sees the customer's name and email. *Why:* experts need to reach participants, and there's no in-app messaging.
- **D-19 Teams links.** Each run has one Teams link set by the admin. Consultations use the expert's personal Teams link from their profile ([UC-14](use-cases/uc-14-manage-expert-profile.md)). Customers see a link from 1 h before start until the end; experts always see it. *Why:* no MS Graph integration needed.

## Cross-cutting

- **D-20 Defaults.** Time zone `Europe/Zurich` (instants stored as `timestamptz`, local times for slots and availability); English UI only, without i18n; no email notifications; GDPR features are future work; every table has `created_at` and `updated_at`. *Why:* YAGNI for a PoC/MVP.
- **D-21 Images.** Upload per entity (offering image, expert photo), stored in a Postgres `image` table behind an `ImageStore` port and served at `GET /api/images/{id}` with long cache headers (a replaced image gets a new id). JPEG, PNG or WebP, at most 2 MB, no resizing. *Why:* image management is in scope from the PoC, and storing images in the database needs no extra infrastructure.

## Architecture

- **D-22 Layer-first packages.** Root packages `api`, `application`, `domain`, `adapters.database`, with feature sub-packages inside each. ArchUnit enforces the layer rules. No Spring Modulith. *Why:* the layout the spec asked for.
- **D-23 One shared schema with narrow ports.** The features are `user`, `offering`, `consultation`, `training`, `availability`, `image` and `shared`. Each use case defines narrow ports, and database adapters may join across feature tables. Feature isolation isn't enforced. *Why:* coupling lives in SQL behind narrow ports instead of in shared domain classes, and no split into services is planned.
- **D-24 No Order entity yet.** Buying a package creates a case and buying a run creates an enrollment, each with its own price and refund data. An `order` feature arrives together with real payment. *Why:* until payment exists, an Order would only be a pass-through table.
- **D-25 Authentication.** Backend-for-frontend (BFF): Spring Boot is the confidential OIDC client, and the SPA only holds an HttpOnly session cookie. In the PoC, `POST /api/poc/login` creates the same session and exists only in the `poc` profile. `GET /api/me` behaves identically in both modes. Keycloak runs locally; the production IdP is open. *Why:* the current recommendation for browser apps, and swapping in real login doesn't touch the SPA.
- **D-26 Authorization.** Roles are checked per route prefix in Spring Security (`/api/shop` public, then `/api/customer`, `/api/expert`, `/api/admin`). Ownership is enforced in the use cases through scoped queries, returning 404 when a resource isn't yours. Tests are REST integration tests: a role matrix plus ownership checks using the two seeded users per role. *Why:* the real filtering lives in SQL, so only tests against a real database prove it.
- **D-27 Concurrency.** The use-case transaction takes pessimistic row locks on the contended parent row, always in the order case → expert → run. *Why:* this covers every rule, including overlaps across tables, without retry logic.
- **D-28 Backend tooling.** Java 25, Spring Boot 4.x, Maven, Spring Data JPA/Hibernate, hand-written mappers, records without Lombok, Flyway (schema plus seed for `poc`/`dev`), Bean Validation on request DTOs, `ProblemDetail` errors, JUnit 5, AssertJ, Testcontainers, ArchUnit, `MockMvcTester`, and an injected `Clock`.
- **D-29 Frontend tooling and API contract.** Vite, React, TypeScript (strict), npm, React Router, TanStack Query, shadcn/ui, Tailwind, React Hook Form, and date-fns with `@date-fns/tz`. The API is code-first: springdoc generates `backend/openapi.json`, which is committed, and openapi-typescript plus openapi-fetch generate the frontend client. A test fails if the committed spec is stale. Frontend tests use Vitest only where there's logic; Playwright arrives in the MVP.
- **D-30 Repository and runtime.** A monorepo with `backend/`, `frontend/`, `_docs/` and `docker-compose.yml` (Postgres, Keycloak). Production is one container: the Spring Boot jar serves the built SPA. Hosting and CI are decided at MVP time.

## Scope and docs

- **D-31 PoC vs MVP.** The PoC covers UC-01 to UC-18 with the fake login and no money. The MVP adds OIDC login, user management (UC-19), payment and refunds, Playwright E2E, hosting and CI. Two pairs of spec use cases were merged: the customer and expert "takes part in call" became [UC-10](use-cases/uc-10-join-call.md), and "Admin views all trainings" became part of [UC-17](use-cases/uc-17-manage-training-offering.md).
- **D-32 Docs and agent instructions.** The `_docs/` structure is shown in [README.md](README.md), with one file per use case and this single decision log. There is only an `AGENTS.md` and no `CLAUDE.md`, because Claude Code ≥ 2.1.277 reads `AGENTS.md` when no `CLAUDE.md` exists.

## Open decisions (before MVP)

| Topic | Notes |
|---|---|
| Payment provider | e.g. Stripe, or Datatrans for TWINT. Comes with the `order` feature. |
| Production IdP | Any OIDC provider (Keycloak, Entra External ID, Auth0, …). Configuration only. |
| Hosting | Must run one container plus Postgres. |
| CI | Not chosen yet. |
