# Backend architecture

A Spring Boot modular monolith with a hexagonal architecture, kept pragmatic. The domain is DDD-inspired, without strict aggregate rules. Use-case-specific objects are preferred over one shared domain model that couples every use case.

## Tooling

Java 25 · Spring Boot 4.x · Maven (wrapper) · Spring Data JPA / Hibernate · Flyway · Bean Validation · springdoc-openapi · JUnit Jupiter 6 · AssertJ · Testcontainers · ArchUnit · `MockMvcTester`.

There's no Lombok and no MapStruct. Value objects, commands and DTOs are Java `record`s (D-28).

## Packages

The layers are the root packages (D-22), and the features are sub-packages within each layer (D-23). Within a feature package, the supporting types sit in a sub-package per role (D-37): `entity` and `row` in `adapters.database`, `response` and `request` in `api`, `port` in `application`, `value` in `domain`.

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
├─ application                use cases + commands, outgoing ports (in port/)
│  ├─ consultation            e.g. BookConsultationAppointmentUseCase; port: LoadCaseForBookingPort
│  ├─ training
│  ├─ offering
│  ├─ availability
│  ├─ user
│  └─ image
├─ domain                     models + value objects (in value/), no framework imports
│  ├─ shared                  NotFoundException, BusinessTimeZone; value: Money, ...
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
| `shared` | `NotFoundException`, `BusinessTimeZone`, and the value objects several features use, in `domain.shared.value` (domain only) | — |

UC-10 (join call) has no feature of its own. Link visibility is part of the case and enrollment detail queries, using a shared `LinkWindow` domain rule.

## Layer rules

Enforced by ArchUnit.

```
 api ──────────► use cases ──────────► domain
  │                  │                  ▲  ▲
  │                  ▼                  │  │
  │                ports ───────────────┘  │
  │                  ▲                     │
  │                  │ implements          │
  └──────► domain    adapters.database ────┘
```

`application` is two layers: the use cases (and their commands) at the top of `application.<feature>`, and the ports in `application.<feature>.port`. Controllers reach only the use cases, adapters only the ports.

| Package | May depend on | Must not depend on |
|---|---|---|
| `domain` | `java.*`, other `domain` packages | Spring, JPA, `application`, `api`, `adapters` |
| `application.<feature>` (use cases) | ports, `domain`, Spring (`@Service`, `@Transactional`) | `api`, `adapters`, JPA |
| `application.<feature>.port` (ports) | `domain` | use cases, `api`, `adapters`, JPA |
| `api` | use cases, `domain`, Spring Web | ports, `adapters` |
| `adapters.database` | ports, `domain`, JPA | use cases, `api` |

Features aren't isolated from each other (D-23). A use case may use domain classes of another feature, and a database adapter may join tables of several features.

## Use cases

- One use case = one class `<Verb><Noun>UseCase` at the top of `application.<feature>`, annotated `@Service`. Its command `record` sits next to it.
- **One public method, named `invoke`** (ArchUnit checks the name and that it is the only one). It takes the `CurrentUser` (if the use case needs it) and a command `record`, and returns a result `record`.
- `@Transactional` sits on the use-case method. Read-only use cases use `@Transactional(readOnly = true)`.
- **Flow only:** load through ports, call domain logic, save through ports. Business decisions (is it bookable, refund-eligible, …) live in the domain.
- There are **no incoming port interfaces**. Controllers call the use case directly.
- Controllers don't contain logic. They map HTTP to the command, call the use case, and map the result to the response DTO.

```java
@Service
public class BookConsultationAppointmentUseCase {
    // ports and the Clock injected via constructor
    @Transactional
    public CustomerAppointment invoke(CurrentUser user, BookConsultationAppointmentCommand command) {
        CaseForBooking consultationCase = lockCase.lock(command.consultationCaseId(), user.id()) // scoped: 404 if not theirs
                .orElseThrow(() -> new NotFoundException(...));
        consultationCase.requireOpenCredit();
        UserId expertId = consultationCase.expertFor(command.expertId());
        lockExpert.lock(expertId);                                       // D-27: case → expert
        Instant now = clock.instant();
        SlotFinder slotFinder = new SlotFinder(loadAvailability.load(expertId, today), loadBusyTimes.load(expertId, now));
        Booking booking = consultationCase.book(expertId, command.start(), slotFinder, now); // the domain decides
        saveBooking.save(booking);
        return booking.appointment().at(now);
    }
}
```

*(Shortened from the real use case: lock, decide, write, answer.)*

## Ports

- Outgoing ports are interfaces named `<Purpose>Port` (e.g. `LoadCaseForBookingPort`, `LoadBusyTimesPort`, `SaveBookingPort`), living in `application.<feature>.port`, below the use cases that need them (D-37). A port's signature uses domain types, never a command; ArchUnit checks that, and that only use cases and adapters use ports. A use case may use another feature's port, such as `LoadExpertsPort` in `application.user.port` (D-23).
- Ports are **narrow and use-case specific**. They return exactly the domain object that use case needs. Two use cases share a port only if they need the identical thing.
- Ports are implemented in `adapters.database.<feature>`. One adapter class may implement several ports.
- `ImageStorePort` (`application.image.port`: `save`, `load`, `delete`) is the port for image bytes (D-21). Its implementation, `ImageAdapter`, is a Postgres table, replaceable by S3 later. An image is never changed: a use case that replaces one saves the new image, points its owner at it and deletes the old one, in one transaction (`UploadConsultationOfferingImageUseCase`).

## Domain

- POJOs and `record` value objects without framework imports. All business rules live here and are unit tested.
- **Value objects** (examples): `Money` (CHF), `CancellationDeadline` (hours), `SessionDuration`, `SessionCount` (at least 1), `OfferingName` and `DisplayName` (never blank), `TimeRange`, `LocalTimeSlot`, `Seats`, `EmailAddress`, typed ids (`ConsultationCaseId`, `UserId`, …).
- **When a value is a value object** (D-36): when it has a rule beyond not null, a unit or behavior, or could be mixed up with another value of the same type. Other values stay Java types, like an offering's description and an expert's bio (`String`s that may be empty). A column that holds a value object repeats its rule as a check constraint.
- **Use-case-specific domain objects** are encouraged, e.g. `CaseForBooking` and `CaseForCancellation` instead of one big `Case`.
- **Models and value objects.** In `domain.<feature>`, the models a use case works with sit at the top, and the value objects in `value`. A value object describes one thing by its value (an amount, a duration, an id, a type) and depends only on Java and other value objects; ArchUnit checks this, so a class that needs a model is a model itself. Typed ids are value objects, so several models can share one, like `OfferingId`. Shared value objects live in `domain.shared.value`.
- **Derived state is computed, never stored:** `CONSUMED`, `COMPLETED`, open credits, refund eligibility before cancellation, link visibility.
- **Rules as constants in one place:** slot grid 30 min, minimum notice 24 h, link window 1 h (D-10, D-19).
- Methods that depend on the current time take a `Clock` or an `Instant now`. A local date, such as today, is taken in `BusinessTimeZone.ZONE` (`Europe/Zurich`), never in the clock's own zone: `LocalDate.ofInstant(clock.instant(), BusinessTimeZone.ZONE)`.

## Persistence

- **One PostgreSQL schema** for all features (D-23). JPA entities live only in `adapters.database` and are mapped to domain objects by hand-written mappers.
- **Entities and rows.** `adapters.database.<feature>` holds the adapter classes and the Spring Data repositories. Its `entity` sub-package holds the JPA entities, one per table, mapping only the columns the queries read. Its `row` sub-package holds the `*Row` records a query selects into with `select new` (a DTO projection): shaped for that one query, possibly spanning several tables, and never managed by JPA. The adapter maps rows to domain objects. Entities and rows are public so the parent package can use them; the layer rules keep them inside `adapters.database`, and ArchUnit checks both sub-packages.
- **Flyway:** the schema is in `db/migration`. Seed data in `db/seed` is only on the Flyway path in the `poc` and `dev` profiles. Seed dates are relative to the migration date (e.g. `current_date + 14`), so the demo data stays in the future. A seed migration is numbered right after the schema migration it needs (`V2__create_user.sql` in `db/migration`, then `V2_1__seed_user.sql` in `db/seed`). Merged migrations, schema and seed alike, are never edited; a change is a new migration.
- **Seed content:** the six users from [UC-01](../use-cases/uc-01-login.md); Career Coaching (packages 3 × CHF 450 and 5 × CHF 700, 60 min, 24 h deadline, both experts eligible); Software Architecture (2 days) and AI-Augmented Software Engineering (1 day), each with time slots 09:00–12:00 and 13:00–17:00, a 168 h deadline and two future runs. No availability, cases or appointments: the experts set their availability in the demo, and tests set it through the API and delete it afterwards ([spec 04](../specs/04-book-consultation-appointment.md#decisions), decision 8).
- **Ids:** UUIDs generated in the application. Seed rows use fixed, readable UUIDs that tests refer to.
- **Tables:** singular snake_case names. The user table is `app_user`, because `user` is reserved in PostgreSQL.
- **Columns:** points in time are `timestamptz`, local times are `time`, dates are `date`, money is `numeric(10,2)` (currency is always CHF). Every table has `created_at` and `updated_at` (D-20).
- **`updated_at`** is the time of the last change, and the database sets it: a `before update` trigger with the function `set_updated_at()` (`V4__set_updated_at.sql`) sets it to the transaction's `now()`, like `created_at`'s default. Entities map neither column. A new table adds the trigger in its create migration; `UpdatedAtTriggersTest` fails for a table without it.
- **Writing with JPA:** the application sets the ids, so an adapter inserts a new row with `EntityManager.persist`; Spring Data's `save()` would merge it, with a select first. A changed row is written by changing its managed entity, loaded in the same transaction. Entities gain a constructor and setters for this, and still map only the columns the adapter uses. A list of child rows (an offering's packages) is written as a difference: the adapter loads the managed entities, deletes the ones the domain dropped, updates the ones it kept and persists the new ones; a kept row is never deleted and inserted again. An expert's availability is replaced instead (`AvailabilityAdapter`): each of its rows is nothing but its key, so a kept one is the same row either way. Bulk JPQL `delete` statements run at once, then the new rows are persisted; deleting entities instead would fail, because Hibernate flushes inserts before deletes.
- **Sorting text:** a name column the UI sorts by is declared `collate "und-x-icu"` (ICU root collation), so `order by` is alphabetical regardless of case and umlauts, and the same in dev, test and production; the database's default collation can sort byte-wise. Known gap: `app_user.name` lacks it, though `GET /api/poc/users` sorts by it; the next migration that touches `app_user` adds it ([task 01 of increment 02](../tasks/02-manage-consultation-offerings/01-poc-login.md#notes)).
- **Purchase terms** are columns on `consultation_case` and `enrollment` (D-08). A run's schedule is stored in `run_day` and `run_time_slot`.
- **Images:** an `image` table with the bytes (`bytea`) and the content type; checks keep the content type to JPEG, PNG and WebP and the size between 1 byte and 2 MB (D-21). The domain's `ImageType` detects the type from the file's first bytes, which the database doesn't check. An owner refers to its image (`offering.image_id`, unique), so an image belongs to one owner at most, and the image row is deleted after the owner stops referring to it.

## Concurrency

Pessimistic row locks through port methods, at the start of the use-case transaction, always in the order **offering → case → expert → run** (D-27). A lock port loads the row with JPA's `PESSIMISTIC_WRITE` (`LockConsultationOfferingPort`, `EntityManager.find` with the lock mode), which Hibernate sends to Postgres as `for no key update`: it waits for every other lock of the row, but not for the key-share lock a foreign key check takes. So every writer that must wait takes the lock itself.

A concurrency test makes its race certain, not likely: it holds the contended row lock in its own transaction (`select … for no key update`), starts both requests, waits until `pg_stat_activity` shows both waiting for a lock, and only then commits. With the lock, both wait at the lock and run one after the other; without it, both read the row at once and wait only at their update. `ConsultationOfferingConcurrencyTest` is the pattern, and it fails without the lock. Where one request writes nothing the other waits for, as a purchase's insert against an unpublish, the test also holds the row that its insert's foreign key checks (`for update`), and lets go of it only after the other request is through or waits for it.

| Use case | Locks |
|---|---|
| Edit an offering, change its status or image, delete it (UC-16, UC-17) | offering |
| Purchase consultation package (UC-03) | offering |
| Book appointment (UC-06) | case → expert (user row) |
| Save availability (UC-13) | expert (user row) |
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
- Known gap: a text value with a NUL character (`\u0000`), such as an offering name, answers 500, because Postgres rejects it in `text`. A value object (or a request check) should refuse it with a 400.

**409s:** a business rule that says no throws `BusinessRuleException` (`domain.shared`) with a stable `code` and a message written for the user. `ApiExceptionHandler` maps it to 409 with the message as `detail` and `code` as a property; the frontend shows the `detail` ([D-34](../decisions.md#architecture)). The codes are constants next to the rule, such as `ConsultationOffering.WRONG_OFFERING_STATUS`.

**In `openapi.json`:** one shared `ProblemDetail` schema (D-34): `title`, `status`, `instance`, `detail`, `errors` for a 400, and `code` for a 409. `type` is optional: Spring leaves it out when it's `about:blank`, as it is for every body so far. `ProblemDetailSchemaCustomizer` builds it as the body is sent; the schema springdoc would derive from Spring's class has a `properties` map and neither `errors` nor `code`. The frontend reads only a 400's and a 409's body ([frontend](frontend.md#errors)).

**Each route lists exactly the error answers it can give** from Spring MVC, as `application/problem+json` with that schema; 401, 403 and 500 stay out (D-34), and so do 405 and 406, which answer a method or an `Accept` header the contract doesn't offer. The controller method declares them next to its mapping, with the meta-annotations of `ProblemResponses` (`api`):

| Annotation | When the route answers it |
|---|---|
| `@ProblemResponses.Status400` | 400: a malformed id in the path, an invalid or unreadable body, a value a value object rejects |
| `@ProblemResponses.Status404` | 404: a `NotFoundException`, such as an unknown id |
| `@ProblemResponses.Status409` | 409: a `BusinessRuleException` |
| `@ProblemResponses.Status415` | 415: a body of another type than the route takes, on every route with a body: JSON sent as text, or an upload that isn't multipart |

```java
@PostMapping("/{id}/publish")
@ResponseStatus(HttpStatus.OK)
@ProblemResponses.Status400
@ProblemResponses.Status404
@ProblemResponses.Status409
ConsultationOfferingResponse publishConsultationOffering(@PathVariable UUID id) { … }
```

- **A new route** gets one annotation per error it can answer, and none if it can't answer any (a list without parameters). The increment plan's route table names them. The annotations are named after the status, not `BadRequest`: `LayerRulesTest` takes a name ending in `Request` for a request record.
- **`@ResponseStatus` with the success status** goes on every method that declares an error, `HttpStatus.OK` included: springdoc lists a route's success by itself only while the route declares no other answer. `OpenApiContractTest` checks that every route lists its success, and the answers of a few routes.
- `ApiExceptionHandler`'s methods carry no `@ResponseStatus`: the returned `ProblemDetail` sets the status. `springdoc.override-with-generic-response: false` (`application.yml`) keeps springdoc from adding the advice's answers to every route.

## REST conventions

- Route prefixes by audience (D-26): `/api/shop`, `/api/customer`, `/api/expert`, `/api/admin`, plus `/api/me`, `/api/images`, `/api/poc` (`poc` profile only).
- **Response DTOs per audience:** customer, expert and admin case responses are different records. This makes "admin never sees notes" a property of the API's shape.
- **Controllers and DTOs.** `api.<feature>` holds the controllers. Its `response` sub-package holds the `*Response` records, each with a public static `from(...)` that maps the domain object; its `request` sub-package holds the `*Request` records. A response that several features return, such as `MoneyResponse`, lives in `api.response`. ArchUnit checks both packages.
- Request and response records have names that are unique across `api`, because the OpenAPI schema name is the simple class name (a test checks this).
- **Controller method names** are the `operationId`s in `openapi.json`, so each names its route uniquely: `listConsultationOfferings`, `getConsultationOffering`, `createConsultationOffering`, `editConsultationOffering`.
- **Uploads** are `multipart/form-data` with one part, `file` (`@RequestPart MultipartFile`, `consumes` multipart, so another body answers 415). The multipart limit is 2 MB per file (`spring.servlet.multipart` in `application.yml`; Spring's default is 1 MB), and a file over it answers 400 like a file of another type (`ApiExceptionHandler`), not Spring's 413. The domain checks the size again, because MockMvc never meets the servlet container's limit.
- **Images** are served by `GET /api/images/{id}` as a `Resource` (so `openapi.json` says `binary`) with `Cache-Control: public, max-age=31536000, immutable`. A header set by the controller keeps Spring Security's `no-cache` headers off the response.
- **A `POST` that creates** answers 201 with the new resource, plus `@ResponseStatus(HttpStatus.CREATED)` on the method, so springdoc lists 201 instead of 200. When the resource has a route of its own, the answer also has its relative `Location` (`ResponseEntity.created`), as a purchase's case has. The booking's appointment has no route of its own, it's part of its case, so `POST /api/customer/consultation-cases/{id}/appointments` answers 201 without a `Location` ([spec 04](../specs/04-book-consultation-appointment.md#decisions), decision 17).
- **Request records** carry Bean Validation annotations that repeat the use case's rules, so a 400 names every invalid field ([errors](#errors)); numbers are boxed (`Integer`) with `@NotNull`, so a missing number is a 400, not a silent 0. A decimal in a whole-number field is a 400 as well, without `errors` (`spring.jackson.deserialization.accept-float-as-int: false` in `application.yml`), not cut off. The controller maps the request to the use case's command, building the value objects.
- JSON with camelCase. Instants are ISO-8601 with offset; local dates and times are ISO-8601 without offset. Money is `{ "amount": "450.00", "currency": "CHF" }`.
- The API is code-first. springdoc generates `backend/openapi.json`, which is committed. A test fails if the committed file differs from the generated one (D-29).

## Testing

| Level | What | Rule |
|---|---|---|
| Domain unit tests | every business rule (slots, deadlines, credits, refund eligibility, seats, lifecycle, link window) | extensive; plain JUnit; time is fixed via `Clock` |
| REST integration tests | `@SpringBootTest` + `MockMvcTester` + Testcontainers Postgres | one happy-path test per route; `@IntegrationTest` also starts the server on a random port, for the few tests that need the real servlet container, such as the upload limit (`ImageUploadLimitTest`); its clock is a `TestClock` in Zurich, the system clock until a test sets an instant (`set(Instant)`), and reset to it after each test |
| Authorization tests | role matrix over all routes (anonymous → 401, wrong role → 403); ownership with the two seed users per role (other's resource → 404); the admin case response has no notes; private notes never appear in customer responses | mandatory ([security](security.md#testing)) |
| Concurrency tests | parallel requests on each locked race (offering status, expert slot, case credits, run seats) | one per race |
| Use-case tests | only where the flow has logic worth isolating | optional |
| Architecture tests | ArchUnit layer rules | mandatory |
| Contract test | the committed `openapi.json` is up to date | mandatory |
