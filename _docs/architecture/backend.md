# Backend architecture

A Spring Boot modular monolith with a hexagonal architecture, kept pragmatic. The domain is DDD-inspired, without strict aggregate rules. Use-case-specific objects are preferred over one shared domain model that couples every use case.

## Tooling

Java 25 · Spring Boot 4.x · Maven (wrapper) · Spring Data JPA / Hibernate · Flyway · Bean Validation · springdoc-openapi · JUnit Jupiter 6 · AssertJ · Testcontainers · ArchUnit · `MockMvcTester`.

There's no Lombok and no MapStruct. Value objects, commands and DTOs are Java `record`s (D-28).

## Packages

The layers are the root packages (D-22), and the features are sub-packages within each layer (D-23). Within a feature package, the supporting types sit in a sub-package per role (D-37): `entity` and `row` in `adapters.database`, `response` and `request` in `api`, `value` in `domain`.

```
com.cleanlearning
├─ config                     Spring configuration (security, clock, web, openapi)
├─ api                        REST controllers + request/response DTOs
│  ├─ shop                    public catalog endpoints
│  ├─ consultation
│  ├─ training
│  ├─ availability
│  ├─ offering                admin catalog endpoints
│  ├─ user                    /api/me, poc login, (MVP) user mgmt
│  └─ image
├─ application                use cases + outgoing ports
│  ├─ consultation            e.g. BookConsultationAppointmentUseCase, LoadCaseForBookingPort
│  ├─ training
│  ├─ offering
│  ├─ availability
│  ├─ user
│  └─ image
├─ domain                     models + value objects (in value/), no framework imports
│  ├─ shared                  NotFoundException; value: Money, TimeRange, ...
│  ├─ consultation            e.g. SlotFinder, CaseCredits, CaseCancellation
│  ├─ training
│  ├─ offering
│  ├─ availability
│  └─ user
└─ adapters.database          JPA entities, Spring Data repositories, port implementations
   ├─ consultation
   ├─ training
   ├─ offering
   ├─ availability
   ├─ user
   └─ image
```

### Features

| Feature | Responsibility | Use cases |
|---|---|---|
| `user` | users, roles, expert profile, current user, PoC login | UC-01, UC-14, UC-19 |
| `offering` | consultation and training offerings, packages, eligible experts, day templates, runs, lifecycle, shop queries | UC-02, UC-16, UC-17 |
| `consultation` | cases, booking, slot finding, cancellation, notes, credit adjustments | UC-03, UC-05 (cases), UC-06–08, UC-11, UC-15, UC-18 |
| `training` | enrollments, enrollment cancellation, expert training views | UC-04, UC-05 (enrollments), UC-09, UC-12 |
| `availability` | expert availability | UC-13 |
| `image` | image upload and serving | (D-21) |
| `shared` | `NotFoundException`, and the value objects several features use, in `domain.shared.value` (domain only) | — |

UC-10 (join call) has no feature of its own. Link visibility is part of the case and enrollment detail queries, using a shared `LinkWindow` domain rule.

## Layer rules

Enforced by ArchUnit.

```
 api ──────────► application ──────────► domain
  │                   ▲                     ▲
  │                   │ implements ports    │
  └──────► domain     adapters.database ────┘
```

| Package | May depend on | Must not depend on |
|---|---|---|
| `domain` | `java.*`, other `domain` packages | Spring, JPA, `application`, `api`, `adapters` |
| `application` | `domain`, Spring (`@Service`, `@Transactional`) | `api`, `adapters`, JPA |
| `api` | `application`, `domain`, Spring Web | `adapters` |
| `adapters.database` | `application` (ports), `domain`, JPA | `api` |

Features aren't isolated from each other (D-23). A use case may use domain classes of another feature, and a database adapter may join tables of several features.

## Use cases

- One use case = one class `<Verb><Noun>UseCase` in `application.<feature>`, annotated `@Service`.
- **One public method, named `invoke`** (ArchUnit checks the name and that it is the only one). It takes the `CurrentUser` (if the use case needs it) and a command `record`, and returns a result `record`.
- `@Transactional` sits on the use-case method. Read-only use cases use `@Transactional(readOnly = true)`.
- **Flow only:** load through ports, call domain logic, save through ports. Business decisions (is it bookable, refund-eligible, …) live in the domain.
- There are **no incoming port interfaces**. Controllers call the use case directly.
- Controllers don't contain logic. They map HTTP to the command, call the use case, and map the result to the response DTO.

```java
@Service
class BookConsultationAppointmentUseCase {
    // ports injected via constructor
    @Transactional
    BookedAppointment invoke(CurrentUser user, BookAppointmentCommand cmd) {
        var caseForBooking = loadCase.lockForCustomer(cmd.caseId(), user.id()); // 404 if not theirs
        var expertId = caseForBooking.expertOr(cmd.expertId());
        lockExpert.lock(expertId);
        var busy = loadBusyTimes.forExpert(expertId, ...);
        var availability = loadAvailability.forExpert(expertId, ...);
        var appointment = caseForBooking.book(cmd.start(), availability, busy, clock); // domain decides
        saveAppointment.save(appointment);
        return BookedAppointment.from(appointment);
    }
}
```

*(Illustrative only. It shows the shape, not the final API.)*

## Ports

- Outgoing ports are interfaces named `<Purpose>Port` (e.g. `LoadCaseForBookingPort`, `LoadExpertBusyTimesPort`, `SaveAppointmentPort`), living in `application.<feature>` next to the use cases that need them.
- Ports are **narrow and use-case specific**. They return exactly the domain object that use case needs. Two use cases share a port only if they need the identical thing.
- Ports are implemented in `adapters.database.<feature>`. One adapter class may implement several ports.
- `ImageStorePort` (`application.image`: `save`, `load`, `delete`) is the port for image bytes (D-21). Its implementation, `ImageAdapter`, is a Postgres table, replaceable by S3 later. An image is never changed: a use case that replaces one saves the new image, points its owner at it and deletes the old one, in one transaction (`UploadConsultationOfferingImageUseCase`).

## Domain

- POJOs and `record` value objects without framework imports. All business rules live here and are unit tested.
- **Value objects** (examples): `Money` (CHF), `CancellationDeadline` (hours), `SessionDuration`, `SessionCount` (at least 1), `OfferingName` and `DisplayName` (never blank), `TimeRange`, `LocalTimeSlot`, `Seats`, `EmailAddress`, typed ids (`CaseId`, `UserId`, …).
- **When a value is a value object** (D-36): when it has a rule beyond not null, a unit or behavior, or could be mixed up with another value of the same type. Other values stay Java types, like an offering's description and an expert's bio (`String`s that may be empty). A column that holds a value object repeats its rule as a check constraint.
- **Use-case-specific domain objects** are encouraged, e.g. `CaseForBooking` and `CaseForCancellation` instead of one big `Case`.
- **Models and value objects.** In `domain.<feature>`, the models a use case works with sit at the top, and the value objects in `value`. A value object describes one thing by its value (an amount, a duration, an id, a type) and depends only on Java and other value objects; ArchUnit checks this, so a class that needs a model is a model itself. Typed ids are value objects, so several models can share one, like `OfferingId`. Shared value objects live in `domain.shared.value`.
- **Derived state is computed, never stored:** `CONSUMED`, `COMPLETED`, open credits, refund eligibility before cancellation, link visibility.
- **Rules as constants in one place:** slot grid 30 min, minimum notice 24 h, link window 1 h (D-10, D-19).
- Methods that depend on the current time take a `Clock` or an `Instant now`.

## Persistence

- **One PostgreSQL schema** for all features (D-23). JPA entities live only in `adapters.database` and are mapped to domain objects by hand-written mappers.
- **Entities and rows.** `adapters.database.<feature>` holds the adapter classes and the Spring Data repositories. Its `entity` sub-package holds the JPA entities, one per table, mapping only the columns the queries read. Its `row` sub-package holds the `*Row` records a query selects into with `select new` (a DTO projection): shaped for that one query, possibly spanning several tables, and never managed by JPA. The adapter maps rows to domain objects. Entities and rows are public so the parent package can use them; the layer rules keep them inside `adapters.database`, and ArchUnit checks both sub-packages.
- **Flyway:** the schema is in `db/migration`. Seed data in `db/seed` is only on the Flyway path in the `poc` and `dev` profiles. Seed dates are relative to the migration date (e.g. `current_date + 14`), so the demo data stays in the future. A seed migration is numbered right after the schema migration it needs (`V2__create_user.sql` in `db/migration`, then `V2_1__seed_user.sql` in `db/seed`). Merged migrations, schema and seed alike, are never edited; a change is a new migration.
- **Seed content:** the six users from [UC-01](../use-cases/uc-01-login.md); Career Coaching (packages 3 × CHF 450 and 5 × CHF 700, 60 min, 24 h deadline, both experts eligible); Software Architecture (2 days) and AI-Augmented Software Engineering (1 day), each with time slots 09:00–12:00 and 13:00–17:00, a 168 h deadline and two future runs; availability for both experts.
- **Ids:** UUIDs generated in the application. Seed rows use fixed, readable UUIDs that tests refer to.
- **Tables:** singular snake_case names. The user table is `app_user`, because `user` is reserved in PostgreSQL.
- **Columns:** points in time are `timestamptz`, local times are `time`, dates are `date`, money is `numeric(10,2)` (currency is always CHF). Every table has `created_at` and `updated_at` (D-20).
- **`updated_at`** is the time of the last change, and the database sets it: a `before update` trigger with the function `set_updated_at()` (`V4__set_updated_at.sql`) sets it to the transaction's `now()`, like `created_at`'s default. Entities map neither column. A new table adds the trigger in its create migration; `UpdatedAtTriggersTest` fails for a table without it.
- **Writing with JPA:** the application sets the ids, so an adapter inserts a new row with `EntityManager.persist`; Spring Data's `save()` would merge it, with a select first. A changed row is written by changing its managed entity, loaded in the same transaction. Entities gain a constructor and setters for this, and still map only the columns the adapter uses. A list of child rows (an offering's packages) is written as a difference: the adapter loads the managed entities, deletes the ones the domain dropped, updates the ones it kept and persists the new ones; a kept row is never deleted and inserted again.
- **Sorting text:** a name column the UI sorts by is declared `collate "und-x-icu"` (ICU root collation), so `order by` is alphabetical regardless of case and umlauts, and the same in dev, test and production; the database's default collation can sort byte-wise.
- **Purchase terms** are columns on `consultation_case` and `enrollment` (D-08). A run's schedule is stored in `run_day` and `run_time_slot`.
- **Images:** an `image` table with the bytes (`bytea`) and the content type; checks keep the content type to JPEG, PNG and WebP and the size between 1 byte and 2 MB (D-21). The domain's `ImageType` detects the type from the file's first bytes, which the database doesn't check. An owner refers to its image (`offering.image_id`, unique), so an image belongs to one owner at most, and the image row is deleted after the owner stops referring to it.

## Concurrency

Pessimistic row locks through port methods, at the start of the use-case transaction, always in the order **offering → case → expert → run** (D-27). A lock port loads the row with JPA's `PESSIMISTIC_WRITE` (`LockConsultationOfferingPort`, `EntityManager.find` with the lock mode), which Hibernate sends to Postgres as `for no key update`: it waits for every other lock of the row, but not for the key-share lock a foreign key check takes. So every writer that must wait takes the lock itself.

A concurrency test makes its race certain, not likely: it holds the contended row lock in its own transaction (`select … for no key update`), starts both requests, waits until `pg_stat_activity` shows both waiting for a lock, and only then commits. With the lock, both wait at the lock and run one after the other; without it, both read the row at once and wait only at their update. `ConsultationOfferingConcurrencyTest` is the pattern, and it fails without the lock.

| Use case | Locks |
|---|---|
| Edit an offering, change its status or image, delete it (UC-16, UC-17) | offering |
| Book appointment (UC-06) | case → expert (user row) |
| Cancel appointment / case (UC-07, UC-08) | case |
| Adjust credits (UC-18) | case |
| Assign expert to run (UC-17) | expert → run |
| Purchase run, cancel enrollment (UC-04, UC-09) | run |
| Change run seats (UC-17) | run |

## Errors

Domain and application exceptions are mapped centrally (`@RestControllerAdvice`) to `ProblemDetail` (RFC 9457).

| Situation | Status |
|---|---|
| Malformed request, bean validation failure | 400 (with a field list) |
| Not authenticated | 401 |
| Missing role for the route | 403 |
| Resource doesn't exist **or isn't yours** | 404 |
| Business rule violated (slot taken, sold out, deadline passed, offering has purchases, …) | 409 (with a stable `code` property for the frontend) |

Not every error body is a `ProblemDetail`:

- **`ProblemDetail`** (`application/problem+json`): every error raised inside Spring MVC, through `ApiExceptionHandler` and its base class `ResponseEntityExceptionHandler`: 400, 404, 405 and 409.
- **Not `ProblemDetail`:** errors that never reach a controller or the advice.
  - Spring Security's 401: an empty body.
  - Spring Security's 403, including the CSRF rejection: Boot's error JSON.
  - Unexpected 500s: Boot's error JSON.

Clients rely only on the status for the second group, never on its body ([D-34](../decisions.md#architecture)).

**400s:**

- **Bean Validation** on a `@Valid @RequestBody` answers 400 with every invalid field in `errors`, sorted by path: `field` is the path Spring reports (`name`, `packages[1].price`), `message` is Hibernate Validator's default ("must not be blank") unless a constraint says otherwise. Messages are English whatever the browser's `Accept-Language` (`spring.web.locale` in `application.yml`, D-20).
- **A value object's rule.** The request annotations repeat the use case's rules, but the value object stays the authority. A value it rejects anyway, such as a name of only no-break spaces, throws `IllegalArgumentException`, which `ApiExceptionHandler` maps to 400 with the message as `detail` and no `errors`. **In this code base, `IllegalArgumentException` means "invalid value"; don't throw it for anything else.** A broken invariant or a programming error is an `IllegalStateException` (or a `NullPointerException`), a 500.
- Spring MVC's own 400s (a malformed id, an unreadable body) have no `errors` either.

**409s:** a business rule that says no throws `BusinessRuleException` (`domain.shared`) with a stable `code` and a message written for the user. `ApiExceptionHandler` maps it to 409 with the message as `detail` and `code` as a property; the frontend shows the `detail` ([D-34](../decisions.md#architecture)). The codes are constants next to the rule, such as `ConsultationOffering.WRONG_OFFERING_STATUS`.

**In `openapi.json`:** one shared `ProblemDetail` schema (D-34): `title`, `status`, `instance`, `detail`, `errors` for a 400, and `code` for a 409. `type` is optional: Spring leaves it out when it's `about:blank`, as it is for every body so far. springdoc lists the answers of `ApiExceptionHandler`'s methods that carry `@ResponseStatus` on every route, as `application/problem+json`, and `ProblemDetailSchemaCustomizer` replaces the schema springdoc derives from Spring's class with the body as sent. So every route lists 400, 404 and 409 with `ProblemDetail`, though not every route gives them (`GET /api/me` never answers 409); that's accepted noise of springdoc's own mechanism, and the increment plans' route tables say which answers a route really gives. The frontend reads only a 400's and a 409's body ([frontend](frontend.md#errors)).

## REST conventions

- Route prefixes by audience (D-26): `/api/shop`, `/api/customer`, `/api/expert`, `/api/admin`, plus `/api/me`, `/api/images`, `/api/poc` (`poc` profile only).
- **Response DTOs per audience:** customer, expert and admin case responses are different records. This makes "admin never sees notes" a property of the API's shape.
- **Controllers and DTOs.** `api.<feature>` holds the controllers. Its `response` sub-package holds the `*Response` records, each with a public static `from(...)` that maps the domain object; its `request` sub-package holds the `*Request` records. A response that several features return, such as `MoneyResponse`, lives in `api.response`. ArchUnit checks both packages.
- Request and response records have names that are unique across `api`, because the OpenAPI schema name is the simple class name (a test checks this).
- **Controller method names** are the `operationId`s in `openapi.json`, so each names its route uniquely: `listConsultationOfferings`, `getConsultationOffering`, `createConsultationOffering`, `editConsultationOffering`.
- **Uploads** are `multipart/form-data` with one part, `file` (`@RequestPart MultipartFile`, `consumes` multipart, so another body answers 415). The multipart limit is 2 MB per file (`spring.servlet.multipart` in `application.yml`; Spring's default is 1 MB), and a file over it answers 400 like a file of another type (`ApiExceptionHandler`), not Spring's 413. The domain checks the size again, because MockMvc never meets the servlet container's limit.
- **Images** are served by `GET /api/images/{id}` as a `Resource` (so `openapi.json` says `binary`) with `Cache-Control: public, max-age=31536000, immutable`. A header set by the controller keeps Spring Security's `no-cache` headers off the response.
- **A `POST` that creates** answers 201 with the new resource and a relative `Location` (`ResponseEntity.created`), plus `@ResponseStatus(HttpStatus.CREATED)` on the method, so springdoc lists 201 instead of 200.
- **Request records** carry Bean Validation annotations that repeat the use case's rules, so a 400 names every invalid field ([errors](#errors)); numbers are boxed (`Integer`) with `@NotNull`, so a missing number is a 400, not a silent 0. A decimal in a whole-number field is a 400 as well, without `errors` (`spring.jackson.deserialization.accept-float-as-int: false` in `application.yml`), not cut off. The controller maps the request to the use case's command, building the value objects.
- JSON with camelCase. Instants are ISO-8601 with offset; local dates and times are ISO-8601 without offset. Money is `{ "amount": "450.00", "currency": "CHF" }`.
- The API is code-first. springdoc generates `backend/openapi.json`, which is committed. A test fails if the committed file differs from the generated one (D-29).

## Testing

| Level | What | Rule |
|---|---|---|
| Domain unit tests | every business rule (slots, deadlines, credits, refund eligibility, seats, lifecycle, link window) | extensive; plain JUnit; time is fixed via `Clock` |
| REST integration tests | `@SpringBootTest` + `MockMvcTester` + Testcontainers Postgres | one happy-path test per route; `@IntegrationTest` also starts the server on a random port, for the few tests that need the real servlet container, such as the upload limit (`ImageUploadLimitTest`) |
| Authorization tests | role matrix over all routes (anonymous → 401, wrong role → 403); ownership with the two seed users per role (other's resource → 404); the admin case response has no notes; private notes never appear in customer responses | mandatory ([security](security.md#testing)) |
| Concurrency tests | parallel requests on each locked race (offering status, expert slot, case credits, run seats) | one per race |
| Use-case tests | only where the flow has logic worth isolating | optional |
| Architecture tests | ArchUnit layer rules | mandatory |
| Contract test | the committed `openapi.json` is up to date | mandatory |
