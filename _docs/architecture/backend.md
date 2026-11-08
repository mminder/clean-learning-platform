# Backend architecture

A Spring Boot modular monolith with a hexagonal architecture, kept pragmatic. The domain is DDD-inspired, without strict aggregate rules. Use-case-specific objects are preferred over one shared domain model that couples every use case.

## Tooling

Java 25 · Spring Boot 4.x · Maven (wrapper) · Spring Data JPA / Hibernate · Flyway · Bean Validation · springdoc-openapi · JUnit Jupiter 6 · AssertJ · Testcontainers · ArchUnit · `MockMvcTester`.

There's no Lombok and no MapStruct. Value objects, commands and DTOs are Java `record`s (D-28).

## Packages

The layers are the root packages (D-22), and the features are sub-packages within each layer (D-23).

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
├─ domain                     POJOs + value objects, no framework imports
│  ├─ shared                  Money, CancellationDeadline, TimeRange, ...
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
| `shared` | cross-feature value objects (domain only) | — |

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
- `ImageStore` is the port for image bytes (D-21). Its implementation is a Postgres table, replaceable by S3 later.

## Domain

- POJOs and `record` value objects without framework imports. All business rules live here and are unit tested.
- **Value objects** (examples): `Money` (CHF), `CancellationDeadline` (hours), `SessionDuration`, `TimeRange`, `LocalTimeSlot`, `Seats`, `EmailAddress`, typed ids (`CaseId`, `UserId`, …).
- **Use-case-specific domain objects** are encouraged, e.g. `CaseForBooking` and `CaseForCancellation` instead of one big `Case`.
- **Derived state is computed, never stored:** `CONSUMED`, `COMPLETED`, open credits, refund eligibility before cancellation, link visibility.
- **Rules as constants in one place:** slot grid 30 min, minimum notice 24 h, link window 1 h (D-10, D-19).
- Methods that depend on the current time take a `Clock` or an `Instant now`.

## Persistence

- **One PostgreSQL schema** for all features (D-23). JPA entities live only in `adapters.database` and are mapped to domain objects by hand-written mappers.
- **Flyway:** the schema is in `db/migration`. Seed data in `db/seed` is only on the Flyway path in the `poc` and `dev` profiles. Seed dates are relative to the migration date (e.g. `current_date + 14`), so the demo data stays in the future. A seed migration is numbered right after the schema migration it needs (`V2__create_user.sql` in `db/migration`, then `V2_1__seed_user.sql` in `db/seed`). Merged migrations, schema and seed alike, are never edited; a change is a new migration.
- **Seed content:** the six users from [UC-01](../use-cases/uc-01-login.md); Career Coaching (packages 3 × CHF 450 and 5 × CHF 700, 60 min, 24 h deadline, both experts eligible); Software Architecture (2 days) and AI-Augmented Software Engineering (1 day), each with time slots 09:00–12:00 and 13:00–17:00, a 168 h deadline and two future runs; availability for both experts.
- **Ids:** UUIDs generated in the application. Seed rows use fixed, readable UUIDs that tests refer to.
- **Tables:** singular snake_case names. The user table is `app_user`, because `user` is reserved in PostgreSQL.
- **Columns:** points in time are `timestamptz`, local times are `time`, dates are `date`, money is `numeric(10,2)` (currency is always CHF). Every table has `created_at` and `updated_at` (D-20).
- **Sorting text:** a name column the UI sorts by is declared `collate "und-x-icu"` (ICU root collation), so `order by` is alphabetical regardless of case and umlauts, and the same in dev, test and production; the database's default collation can sort byte-wise.
- **Purchase terms** are columns on `consultation_case` and `enrollment` (D-08). A run's schedule is stored in `run_day` and `run_time_slot`.
- **Images:** an `image` table with `bytea`, content type and size (D-21).

## Concurrency

Pessimistic row locks (`SELECT … FOR UPDATE`) through port methods, at the start of the use-case transaction, always in the order **case → expert → run** (D-27).

| Use case | Locks |
|---|---|
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

- **`ProblemDetail`** (`application/problem+json`): every error raised inside Spring MVC, through `ApiExceptionHandler` and its base class `ResponseEntityExceptionHandler`: 400, 404, 405, and later 409.
- **Not `ProblemDetail`:** errors that never reach a controller or the advice.
  - Spring Security's 401: an empty body.
  - Spring Security's 403, including the CSRF rejection: Boot's error JSON.
  - Unexpected 500s: Boot's error JSON.

Clients rely only on the status for the second group, never on its body ([D-34](../decisions.md#architecture)). Error bodies aren't in `openapi.json` yet: the task with the first 409 adds one shared `ProblemDetail` schema, with `code` for 409 and the field list for 400 (D-34).

## REST conventions

- Route prefixes by audience (D-26): `/api/shop`, `/api/customer`, `/api/expert`, `/api/admin`, plus `/api/me`, `/api/images`, `/api/poc` (`poc` profile only).
- **Response DTOs per audience:** customer, expert and admin case responses are different records. This makes "admin never sees notes" a property of the API's shape.
- Request and response records have names that are unique across `api`, because the OpenAPI schema name is the simple class name (a test checks this). A record that several features return, such as `MoneyResponse`, lives in `api` itself.
- JSON with camelCase. Instants are ISO-8601 with offset; local dates and times are ISO-8601 without offset. Money is `{ "amount": "450.00", "currency": "CHF" }`.
- The API is code-first. springdoc generates `backend/openapi.json`, which is committed. A test fails if the committed file differs from the generated one (D-29).

## Testing

| Level | What | Rule |
|---|---|---|
| Domain unit tests | every business rule (slots, deadlines, credits, refund eligibility, seats, lifecycle, link window) | extensive; plain JUnit; time is fixed via `Clock` |
| REST integration tests | `@SpringBootTest` + `MockMvcTester` + Testcontainers Postgres | one happy-path test per route |
| Authorization tests | role matrix over all routes (anonymous → 401, wrong role → 403); ownership with the two seed users per role (other's resource → 404); the admin case response has no notes; private notes never appear in customer responses | mandatory ([security](security.md#testing)) |
| Concurrency tests | parallel requests on each locked race (expert slot, case credits, run seats) | one per race |
| Use-case tests | only where the flow has logic worth isolating | optional |
| Architecture tests | ArchUnit layer rules | mandatory |
| Contract test | the committed `openapi.json` is up to date | mandatory |
