# 04 Book a consultation appointment: technical plan

Spec: [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same change. Everything from the plans of increments [01](../01-shop-tracer-bullet/README.md), [02](../02-manage-consultation-offerings/README.md) and [03](../03-buy-consultation-package/README.md) still holds: toolchain, test setup, error conventions, writes, locks, forms, mutations, customer-owned data and `lib/date.ts`.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [Manage availability](01-manage-availability.md) | AFK | – |
| 02 | [Bookable slots](02-bookable-slots.md) | AFK | 01 |
| 03 | [Book an appointment](03-book-appointment.md) | AFK | 01, 02 |
| 04 | [My purchases with Upcoming and Past](04-my-purchases-groups.md) | AFK | 03 |
| 05 | [Booking races](05-booking-races.md) | AFK | 03 |

```
                                                  ┌─► 04 My purchases with Upcoming and Past
01 Manage ──► 02 Bookable ──► 03 Book an ─────────┤
availability   slots          appointment         └─► 05 Booking races
```

01 builds the `availability` feature from the schema to the page, the first data an expert owns, with local dates and times in the API, and the test clock. 02 is the slot rule, in the domain only. 03 builds booking from the schema to the dialog and the case detail, and sets the pattern for a two-row lock: review it with extra care, as task 01 of increment 03. 04 and 05 then run in parallel, each in its own git worktree ([D-33](../../decisions.md#scope-and-docs)):

| Task | Touches | Shared with another task |
|---|---|---|
| 04 | the list route: `ListCustomerConsultationCasesUseCase`, `LoadCustomerConsultationCasesPort`, `CustomerConsultationCaseSummary`, its row, query and response; `MyPurchasesPage`, `ConsultationCaseCard`; `openapi.json` | – |
| 05 | a new test class, `ConsultationCaseConcurrencyTest` | – |

Only 01, 03 and 04 change `openapi.json` and `schema.d.ts`, one after the other.

## Backend

### Packages and names

| Package | Classes | Task |
|---|---|---|
| `domain.shared` | `BusinessTimeZone`: `ZONE`, `Europe/Zurich`. `ClockConfig`, today's date and the slot rule use it | 01 |
| `domain.availability` | `Availability`: an expert's dates and time ranges, with UC-13's rules | 01 |
| `domain.availability.value` | `TimeRange`: a start before an end, Zurich wall-clock times | 01 |
| `application.availability` | `GetAvailabilityUseCase`, `SaveAvailabilityUseCase` with `SaveAvailabilityCommand` | 01 |
| `application.availability.port` | `LoadAvailabilityPort` (`load(UserId expertId, LocalDate from)`), `ReplaceAvailabilityPort` (`replaceFrom(UserId expertId, LocalDate from, Availability)`) | 01 |
| `application.user.port` | `LockExpertPort` (`lock(UserId)`) | 01 |
| `adapters.database.availability` | `AvailabilityAdapter` and its repository; `entity`: `AvailabilityDateEntity`, `AvailabilityTimeRangeEntity` | 01 |
| `adapters.database.user` | `UserAdapter` implements `LockExpertPort` too | 01 |
| `api.availability` | `AvailabilityController` (`/api/expert/availability`); `request`: `AvailabilityRequest`, `TimeRangeRequest`, the constraint `@ValidTimeRanges` and its validator; `response`: `AvailabilityResponse`, `TimeRangeResponse` | 01 |
| `domain.consultation` | `SlotFinder`: [D-10](../../decisions.md#consultation)'s constants and the bookable slot | 02 |
| `domain.consultation.value` | `BusyTime`: a start before an end, instants | 02 |
| `domain.consultation` | `CaseForBooking` (the case as booking needs it; holds UC-06's rules), `Booking` (a new appointment, and whether it assigns the case's expert), `Appointment` (a stored appointment), `CustomerAppointment` (an appointment as its customer sees it, with its status), `AssignedExpert`; `CustomerConsultationCase` gains the expert, the appointments and `canBook()` | 03 |
| `domain.consultation.value` | `AppointmentId`, `AppointmentStatus` (`BOOKED`, `CONSUMED`), `BookableSlot` (start, end, free-cancellation time) | 03 |
| `domain.offering.value` | `CancellationDeadline.freeUntil(Instant start)` | 03 |
| `application.consultation` | `ListEligibleExpertsForCaseUseCase`, `ListBookableSlotsUseCase`, `BookConsultationAppointmentUseCase` with `BookConsultationAppointmentCommand` | 03 |
| `application.consultation.port` | `LoadCaseForBookingPort`, `LockCaseForBookingPort`, `LoadBusyTimesPort` (`load(UserId expertId, Instant from)`), `SaveBookingPort` (`save(Booking)`); `LoadCustomerConsultationCasePort.load` takes `asOf` | 03 |
| `adapters.database.consultation` | `ConsultationCaseAdapter` implements the new ports; `entity`: `AppointmentEntity`, and `ConsultationCaseEntity` maps `assignedExpertId` | 03 |
| `api.consultation` | `CustomerConsultationCaseController` gains three routes; `request`: `BookConsultationAppointmentRequest`; `response`: `BookableSlotResponse`, `CustomerAppointmentResponse`, `AssignedExpertResponse` | 03 |
| `api.response` | `EligibleExpertResponse` moves here from `api.shop.response`: the shop and the case both answer it | 03 |
| `domain.consultation` | `CustomerConsultationCaseSummary` gains the expert's name, the appointments, `group()` and `nextAppointment()`, and `MY_PURCHASES_ORDER` | 04 |
| `domain.consultation.value` | `PurchaseGroup` (`UPCOMING`, `NEEDS_BOOKING`, `PAST`) | 04 |
| `application.consultation.port` | `LoadCustomerBusyTimesPort` (`loadCustomerBusyTimes(UserId customerId, Instant from)`; the method isn't `load`, which `ConsultationCaseAdapter` already has with the same parameters for `LoadBusyTimesPort`, as `LoadExpertsPort.loadExperts` sits next to `LoadUsersPort.loadAll`), implemented by `ConsultationCaseAdapter` | [spec decision 22](../../specs/04-book-consultation-appointment.md#decisions) |

- **Names** follow the [glossary](../../glossary.md): availability, time range, appointment, busy time, bookable slot, assigned expert, eligible expert. The customer's records carry the audience, `Customer…`, as in increment 03; the expert's (UC-11) will be different records.
- **Other features' types.** Booking uses `domain.availability`'s `Availability`, `application.availability.port`'s `LoadAvailabilityPort` and `application.user.port`'s `LockExpertPort` where they are ([D-23](../../decisions.md#architecture)): a booking needs exactly what the availability page loads, and exactly the lock that saving availability takes. `TimeRange` stays in `domain.availability.value`; nothing moves to `domain.shared.value`. backend.md's tree loses `TimeRange` from its shared examples (01).
- **Today** is `LocalDate.ofInstant(clock.instant(), BusinessTimeZone.ZONE)`, never the clock's own zone, so a test clock in another zone can't shift it.
- **The use cases take the `CurrentUser`**: the expert's availability is the current user's, and every case query is scoped to the customer.

### Schema

| Migration | Task | Content |
|---|---|---|
| `V7__create_availability.sql` | 01 | `availability_date` and `availability_time_range`, with their `updated_at` triggers |
| `V8__create_appointment.sql` | 03 | `consultation_case.assigned_expert_id`; the `appointment` table with its `updated_at` trigger and index |

```
availability_date        expert_id     uuid not null → app_user,
                         available_on  date not null,
                         created_at, updated_at
                         primary key (expert_id, available_on)

availability_time_range  expert_id     uuid not null → app_user,
                         start_time    time not null,
                         end_time      time not null, check (start_time < end_time),
                         created_at, updated_at
                         primary key (expert_id, start_time)

consultation_case        + assigned_expert_id uuid → expert_profile (user_id), null until the first booking
indexes                  + (assigned_expert_id)

appointment              id                    uuid PK,
                         consultation_case_id  uuid not null → consultation_case,
                         starts_at             timestamptz not null,
                         ends_at               timestamptz not null, check (starts_at < ends_at),
                         status                text not null, check (status in ('BOOKED')),
                         created_at, updated_at
indexes                  (consultation_case_id)
```

- **Availability belongs to the user** (`app_user`), not to the expert profile: it needs `EXPERT`, not a profile. A booking reaches only an eligible or assigned expert, who has a profile.
- **`TimeRange`'s rule** is the check `start_time < end_time` ([D-36](../../decisions.md#architecture)). That ranges neither overlap nor touch is a rule across rows; `Availability` holds it, without a database constraint.
- **The assigned expert** refers to `expert_profile`, as `eligible_expert` does. The case keeps it after the expert stops being eligible (UC-06).
- **The appointment has no expert column**: its expert is its case's assigned expert, which never changes. Busy time joins the case.
- **`status`** holds only `BOOKED`; UC-07 adds `CANCELLED` and `credit_refunded` (spec decision 15). Every query of appointments filters by `status = 'BOOKED'` from the start. The entity maps it with its own enum, `StoredAppointmentStatus` in `adapters.database.consultation.entity`, which holds only the stored statuses; the domain's `AppointmentStatus` also has the derived `CONSUMED`, which can't be written.
- **`starts_at` and `ends_at`**, because `end` is reserved in SQL; `purchased_at` set the pattern.
- No seed (spec decision 8). The test cleanup deletes the new tables' rows ([tests](#tests)).

### API

| Route | Answer | Task |
|---|---|---|
| `GET /api/expert/availability` | 200 `AvailabilityResponse`: the current expert's dates from today on, and their time ranges | 01 |
| `PUT /api/expert/availability` | `AvailabilityRequest` → 200 `AvailabilityResponse`; 400; 415 | 01 |
| `GET /api/customer/consultation-cases/{id}/eligible-experts` | 200 `EligibleExpertResponse[]`, by display name; 400 for a malformed id; 404 | 03 |
| `GET /api/customer/consultation-cases/{id}/bookable-slots?expertId=…` | 200 `BookableSlotResponse[]`, by start; 400 for a malformed or missing id; 404; 409 (`EXPERT_CHANGED`, `EXPERT_NOT_ELIGIBLE`) | 03 |
| `POST /api/customer/consultation-cases/{id}/appointments` | `BookConsultationAppointmentRequest` → 201 `CustomerAppointmentResponse`, without a `Location` (spec decision 17); 400; 404; 409 (`NO_OPEN_CREDITS`, `EXPERT_CHANGED`, `EXPERT_NOT_ELIGIBLE`, `SLOT_NOT_AVAILABLE`); 415 | 03 |
| `GET /api/customer/consultation-cases/{id}` | as before, plus the expert, the appointments and `canBook` | 03 |
| `GET /api/customer/consultation-cases` | as before, plus the expert's name, the next appointment and the group, in the order of **My purchases** | 04 |

401 and 403 come from security: `/api/expert/**` needs `EXPERT`, `/api/customer/**` needs `CUSTOMER` ([security.md](../../architecture/security.md#roles-by-route-prefix)). Each method declares its errors with `ProblemResponses` and its success with `@ResponseStatus`. operationIds: `getAvailability`, `saveAvailability`, `listEligibleExpertsForCase`, `listBookableSlots`, `bookConsultationAppointment`.

```jsonc
// AvailabilityRequest (PUT) and AvailabilityResponse (GET, PUT)                                        01
{
  "dates": ["2026-11-17", "2026-11-18"],   // not null, no null element; a date named twice counts once;
                                           // the response: from today on, ascending
  "timeRanges": [                          // not null, no null element, may be empty; the response: by start
    { "start": "09:00", "end": "12:00" },  // TimeRangeRequest / TimeRangeResponse: both not null
    { "start": "13:00", "end": "17:00" }
  ]
}

// EligibleExpertResponse, moved to api.response                                                        03
{ "id": "…0202", "displayName": "Expert 2", "bio": "Leadership coach …" }

// BookableSlotResponse                                                                                 03
{
  "start": "2026-11-17T09:00:00Z",                  // 10:00 in Zurich
  "end": "2026-11-17T10:00:00Z",
  "freeCancellationUntil": "2026-11-16T09:00:00Z"   // start − the case's cancellation deadline
}

// BookConsultationAppointmentRequest                                                                   03
{
  "expertId": "…0202",               // not null; the expert the customer picked, or the case's
  "start": "2026-11-17T09:00:00Z"    // not null; one of the slots' start
}

// CustomerAppointmentResponse                                                                          03
{ "id": "…", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED" }  // or CONSUMED from the start on

// CustomerConsultationCaseResponse: increment 03's fields, plus                                        03
{
  "expert": { "id": "…0202", "displayName": "Expert 2", "bio": "…" },  // AssignedExpertResponse; @Nullable:
                                                                        // null before the first booking
  "upcomingAppointments": [ /* CustomerAppointmentResponse, not ended yet, soonest first; a running one is CONSUMED */ ],
  "pastAppointments": [ /* CustomerAppointmentResponse, ended, CONSUMED, newest first */ ],
  "canBook": true                                                       // an open credit is left
}

// CustomerConsultationCaseSummaryResponse: increment 03's fields, plus                                 04
{
  "expertDisplayName": "Expert 2",   // @Nullable: no expert yet
  "nextAppointment": { /* CustomerAppointmentResponse */ },  // @Nullable: the soonest upcoming one, CONSUMED while it runs
  "group": "UPCOMING"                // or NEEDS_BOOKING, PAST
}
```

- **Local dates and times** (availability) are ISO-8601 without offset; **instants** (slots, appointments) with offset ([backend.md](../../architecture/backend.md#rest-conventions)). Whether Jackson writes a time as `09:00` or `09:00:00`, the frontend's time input takes `HH:mm`, so the form cuts a time to its first five characters (01).
- **The 400s of `PUT /api/expert/availability`** name each invalid range. `@ValidTimeRanges`, a class-level constraint on `AvailabilityRequest`, adds a violation per invalid range: at `timeRanges[i].end`, "must be after the start", for a range whose end isn't after its start; at `timeRanges[i].start`, "must not overlap or touch another time range", for the later-starting of two ranges that overlap or touch. The frontend shows them next to the fields (`toFormPath`). A date before today is the domain's `IllegalArgumentException`: 400 with the `detail` "A date before today can't be changed: 2026-11-08." and no `errors`; the page shows it above the form. The calendar never sends one.
- **`expertId`** is a required query parameter of the slots; without it Spring answers 400.
- **The booking's answer** is the new appointment (spec decision 17). The dialog reloads the case.
- **`canBook`, `freeCancellationUntil`, `status` and `group`** are computed by the server (spec decision 18).

### Availability

01, following UC-13 and [D-09](../../decisions.md#consultation).

- **`Availability(SortedSet<LocalDate> dates, List<TimeRange> timeRanges)`** copies its inputs, sorts the ranges by start, and throws `IllegalArgumentException` if two ranges overlap or touch (spec decision 9). `requireNoDateBefore(LocalDate today)` throws `IllegalArgumentException` for a date before today. `TimeRange(LocalTime start, LocalTime end)` throws unless start is before end.
- **`GetAvailabilityUseCase.invoke(CurrentUser)`**: `LoadAvailabilityPort.load(expertId, today)`: the dates from today on and every range. Without a saved availability, both are empty.
- **`SaveAvailabilityUseCase.invoke(CurrentUser, SaveAvailabilityCommand)`**:
  1. **Lock** the expert with `LockExpertPort.lock(user.id())`: `PESSIMISTIC_WRITE` on the `app_user` row, as the purchase locks the offering ([D-27](../../decisions.md#architecture), spec decision 13). The current user always exists; a missing row is an `IllegalStateException`.
  2. **The domain decides**: `new Availability(dates, timeRanges)`, then `requireNoDateBefore(today)`.
  3. **Write** with `ReplaceAvailabilityPort.replaceFrom(expertId, today, availability)`: delete the expert's dates from today on and all their ranges with bulk JPQL `delete` statements, then persist the new rows. A bulk delete runs at once, while Hibernate flushes inserts before deletes: deleting entities and persisting rows with the same keys in one flush would fail with a duplicate key. Dates before today stay as they are.
  4. **Answer** the saved availability.
- **The controller** maps the request to `SaveAvailabilityCommand(Set<LocalDate> dates, List<TimeRange> timeRanges)`. The value objects stay the authority: a range the constraint let through and `TimeRange` rejects is a 400.

### Bookable slots

02, following [D-10](../../decisions.md#consultation) and spec decision 11. Pure domain.

`SlotFinder(Availability availability, List<BusyTime> busyTimes)`, with D-10's constants `GRID = 30 minutes` and `MINIMUM_NOTICE = 24 hours`:

- **`List<Instant> startTimes(SessionDuration duration, Instant now)`**, ascending. For each date of the availability and each of its time ranges, every wall-clock time on the grid (minute of the day a multiple of 30) from the range's start, rounded up to the grid, to before its end, is a candidate. A candidate is a start time if:
  - it exists exactly once on that date in `BusinessTimeZone.ZONE` (`ZoneRules.getValidOffsets(LocalDateTime)` has one offset): this skips the hour that doesn't exist in spring and the hour that happens twice in autumn;
  - its instant plus the duration ends at or before the range's end, as an instant (`ZonedDateTime.of(date, end, ZONE)`, Java's rule for an end in the gap or the overlap);
  - it's at least `MINIMUM_NOTICE` after `now`;
  - the session `[start, start + duration)` overlaps no busy time `[start, end)`: touching is fine, as there's no buffer.
- **`boolean isBookable(Instant start, SessionDuration duration, Instant now)`**: `start` is one of `startTimes(duration, now)`. One rule for showing and for booking.
- **`BusyTime(Instant start, Instant end)`** throws unless start is before end.
- **The busy times are the expert's and the customer's together** (spec decision 22): the use cases pass both lists into one `SlotFinder`, which doesn't tell them apart, so one rule decides showing and booking. `SlotFinder` needs no change, and no test of its own for it: it already takes several busy times in any order.

### Booking

03, following UC-06, spec decisions 2, 12 and 15, and [D-27](../../decisions.md#architecture).

- **`CaseForBooking`**: the case's id, its purchase terms, its assigned expert (`Optional<UserId>`), the number of its booked appointments, and its offering's current eligible experts (`List<EligibleExpert>`, by display name). Its rules, with the codes as constants next to them:

  | Method | Throws | `detail` |
  |---|---|---|
  | `requireOpenCredit()` | `NO_OPEN_CREDITS`: open credits are 0 | This case has no open credits. |
  | `expertFor(UserId named)` | `EXPERT_CHANGED`: the case has another expert | This case already has an expert. Please choose a time with them. |
  | | `EXPERT_NOT_ELIGIBLE`: the case has no expert, and the named one isn't eligible | This expert is no longer available for this offering. Please choose another. |
  | `bookableSlots(SlotFinder, Instant now)` | – | the start times for the case's session duration, each as a `BookableSlot` with its end and its free-cancellation time (`CancellationDeadline.freeUntil`) |
  | `book(UserId expertId, Instant start, SlotFinder, Instant now)` | each of the above, then `SLOT_NOT_AVAILABLE`: `start` isn't bookable | This time is no longer available. |

  Open credits are the session count minus the booked appointments (spec decision 15). `book` checks every rule itself, so it's right on its own; it returns a `Booking`: a new `Appointment` (a new id, the case, `start`, `start` + the session duration) and whether it assigns the expert (the case had none).
- **`ListEligibleExpertsForCaseUseCase.invoke(CurrentUser, ConsultationCaseId)`**: `LoadCaseForBookingPort.load(id, customerId)`, 404 if empty, then its eligible experts.
- **`ListBookableSlotsUseCase.invoke(CurrentUser, ConsultationCaseId, UserId expertId)`**: load the case (404), `expertFor(expertId)`, `LoadAvailabilityPort.load(expert, today)`, `LoadBusyTimesPort.load(expert, now)` and `LoadCustomerBusyTimesPort.loadCustomerBusyTimes(customer, now)`, then `bookableSlots(new SlotFinder(availability, both busy times), now)`. No lock: it only shows.
- **`BookConsultationAppointmentUseCase.invoke(CurrentUser, BookConsultationAppointmentCommand)`**:
  1. **Lock the case** with `LockCaseForBookingPort.lock(id, customerId)`: a query scoped to the customer with `@Lock(PESSIMISTIC_WRITE)`, so only the customer's own case is locked; then the rest of `CaseForBooking`. Empty is a `NotFoundException` (404).
  2. **Choose the expert to lock**: `requireOpenCredit()`, then `expertFor(command.expertId())`. Credits come first, so a second booking on a case's last credit is `NO_OPEN_CREDITS` whichever expert it names.
  3. **Lock the expert** with `LockExpertPort.lock(expertId)`, after the case (D-27: case → expert).
  4. **Load** the availability and the busy times, the expert's and the customer's, as the slots do. Under the expert's lock, nobody else books this expert or changes their availability meanwhile. The customer isn't locked: a booking by the same customer on another case, with another expert, could run at the same instant, and two such bookings could overlap; spec decision 22 accepts that.
  5. **The domain decides**: `book(expertId, command.start(), new SlotFinder(…), now)`.
  6. **Write** with `SaveBookingPort.save(booking)`: persist the appointment; on the first booking, set the managed case entity's `assignedExpertId`.
  7. **Answer** the new appointment as a `CustomerAppointment` (`BOOKED`).
- **`LoadBusyTimesPort.load(expertId, from)`**: the booked appointments of every case assigned to the expert that end after `from`, joined through `consultation_case.assigned_expert_id`. Runs add their time slots here with the trainings (spec decision 14).
- **`LoadCustomerBusyTimesPort.loadCustomerBusyTimes(customerId, from)`**: the booked appointments of all the customer's cases, with any expert, that end after `from`, joined through `consultation_case.customer_id` (`findBookedAppointmentsOfCustomerEndingAfter`). A customer's appointments never overlap (spec decision 22).
- **No offering lock.** Booking never locks the offering: eligibility is read when the booking runs, and one that passes just before an admin removes the expert is valid (UC-06). The case of an archived offering books as any other (D-07).

### Ownership

- **Scoped queries**, as in increment 03: `LoadCaseForBookingPort` and `LockCaseForBookingPort` filter by the case's `customer_id` in SQL. Another customer's case is empty, like an unknown id, and the use case throws `NotFoundException` (404) on all three new routes ([security.md](../../architecture/security.md#ownership-in-use-cases)).
- **The slots show no one else's data**: each slot is a start, an end and a free-cancellation time.
- **The customer's busy time is scoped to the customer** in SQL, by the case's `customer_id`: another customer's appointments take none of their times (spec decision 22).
- **Availability has no id in its route**: it's always the current user's.

### The case as its customer sees it

03 for the detail, 04 for the list.

- **The time a case is shown at** is a parameter: `LoadCustomerConsultationCasePort.load(id, customerId, Instant asOf)` and `LoadCustomerConsultationCasesPort.loadAll(customerId, Instant asOf)`, with `asOf` from the `Clock`. The adapter only passes it into the domain records, which derive the statuses from it. So the derived states stay in the domain ([D-11](../../decisions.md#consultation)), and a response maps a model without a clock.
- **`Appointment(AppointmentId, ConsultationCaseId, Instant start, Instant end)`** is the stored, booked appointment. Its status at `asOf` is `BOOKED` while its start is after `asOf`, and `CONSUMED` from its start on ([glossary](../../glossary.md#consultation)); `CustomerAppointment` is the appointment with that status. It's upcoming while its end is after `asOf`: a running appointment is `CONSUMED` and upcoming at once (spec decision 21).
- **`CustomerConsultationCase`** (03) gains `Optional<AssignedExpert> expert`, `List<Appointment> appointments` and `Instant asOf`. `openCredits()` subtracts every booked appointment, consumed or not. `canBook()` is `openCredits() > 0`; UC-08 adds "and active". `upcomingAppointments()` are the ones that haven't ended, soonest first, a running one among them as `CONSUMED`; `pastAppointments()` the ones that have ended, newest first.
- **`CustomerConsultationCaseSummary`** (04) gains `Optional<DisplayName> expertDisplayName`, the appointments and `asOf`. Its `group()`: `UPCOMING` with an upcoming appointment, one that hasn't ended (spec decision 21); otherwise `NEEDS_BOOKING` with an open credit; otherwise `PAST`, a completed case ([UC-05](../../use-cases/uc-05-view-my-purchases.md), [D-17](../../decisions.md#views-and-calls)). `MY_PURCHASES_ORDER` sorts by group in that order, then Upcoming by the next appointment's start (the soonest upcoming one), Needs booking by purchase time, Past by the last appointment's start, newest first, then by id. `ListCustomerConsultationCasesUseCase` sorts with it.
- **The appointments' rules live once**: the summary and the detail share them (through `Appointment`, or one small model both use), and the review checks there's no copy. They're on `Appointment` (03): `statusAt`, `isUpcomingAt`, `at`, the static `upcoming` and `past`, and the static `openCredits(sessionCount, bookedAppointments)`, which `CaseForBooking` counts with, too.
- **The adapter loads the appointments** of the case, or of all the customer's cases with one query, and groups them in Java; no rule in SQL beyond `status = 'BOOKED'`. The list's adapter query reads the expert's display name with the cases (`left join` of the expert profile) and `findBookedAppointmentsOfCustomer` reads the appointments, through the case's `customer_id`.

## Frontend

```
frontend/src
├─ app/               App: /expert/availability under RequireRole role="EXPERT" (01);
│                     Layout: Availability for users with EXPERT, between My purchases and Consultations (01)
├─ components/ui/     calendar (01)
├─ lib/               date.ts: formatLocalDate, formatTime, localDateOf, todayInZurich,
│                     toCalendarDay, fromCalendarDay (01, 03)
└─ features/
   ├─ availability/   queries.ts: useAvailability, useSaveAvailability; AvailabilityPage, AvailabilityForm (01)
   └─ consultation/   queries.ts: useEligibleExperts, useBookableSlots, useBookConsultationAppointment (03);
                      BookAppointmentDialog, with booking-failure.ts (the error table's kinds) and
                      bookable-slots.ts (slotsByDate) (03); appointment-times.ts: formatTimes, formatDateAndTimes (03);
                      ConsultationCasePage (03); MyPurchasesPage, ConsultationCaseCard (04)
```

| Route | Page | Access | Task |
|---|---|---|---|
| `/expert/availability` | `AvailabilityPage` | expert | 01 |

### Availability page

01. A form, following [frontend.md](../../architecture/frontend.md#forms), with `AvailabilityRequest` as its values:

- **Heading** "Availability", and "Customers can book you on the selected dates, within your daily time ranges (Zurich time)."
- **Dates:** a month calendar (shadcn `calendar`, `mode="multiple"`, `timeZone="Europe/Zurich"`) behind a `Controller` on `dates`. Days before today (`todayInZurich()`) are disabled (spec decision 9); it opens on the current month. Below it, the selected dates as text (`formatLocalDate`), "Selected: 17 Nov 2026, 18 Nov 2026", or "No dates selected.": the calendar shows one month, and this shows the dates of the others (01).
- **Daily time ranges:** `useFieldArray` on `timeRanges`, one row per range with two time inputs (start, end) and **Remove**, and **Add time range** below. A new row starts empty. Field errors land on the row (`timeRanges.1.start`).
- **Save** calls `mutateAsync`; "Saved" until the next change; `showSaveError` for a 400. After a save, the saved availability becomes the form's defaults.
- Without availability: no date selected, no range, and the text "No time ranges yet. Add one so that customers can book you."

### Booking dialog

03. `BookAppointmentDialog` (shadcn `dialog`) opens from **Book appointment** on the case detail. Its steps (spec decision 4):

| Step | Content | Buttons |
|---|---|---|
| Expert, only while the case has no expert | "Choose your expert."; one radio item per eligible expert: `ImagePlaceholder`, name, bio. Without experts: "No experts are available for this offering yet. Please try again later." (spec decision 2) | **Cancel**; **Continue**, once an expert is chosen |
| Date | "Choose a date."; the calendar, single selection, only dates with slots enabled, opening on the month of the first slot. Without slots: "No free times available at the moment." | **Back** (if there was an expert step) or **Cancel**; **Continue**, once a date is chosen |
| Time | "Choose a time (Zurich time)."; one radio item per slot of the date: "10:00–11:00" | **Back**; **Continue**, once a time is chosen |
| Confirm | "Book Expert 2 on 17 Nov 2026, 10:00–11:00 (60 min, Zurich time)?" and "Free cancellation until 16 Nov 2026, 10:00." (spec decision 7) | **Back**; **Book** |

- **The slots** come from `useBookableSlots(caseId, expertId)` once the expert is known; the dialog groups them by `localDateOf(start)` (`slotsByDate`). The duration comes from the case's purchase terms.
- **The expert step's Continue** loads the picked expert's slots before the dates show (`fetchQuery`), so a 409 of the slots route (`EXPERT_CHANGED`, `EXPERT_NOT_ELIGIBLE`) and a 404 follow the error table below, as the booking's do; the case, its experts and its slots reload first after a 409. Anything else stays on **Expert** with "The times could not be loaded. Please try again." (03)
- **The expert** the dialog books is the case's as the dialog opened, or the one picked; a reload of the case meanwhile doesn't change it, so the request names the expert the confirmation stated. Only `EXPERT_CHANGED` switches to the reloaded case's expert (03).
- **After the booking** the dialog closes; the case detail shows the new appointment and the credits.
- **Errors** show in the dialog (`role="alert"`):

  | Answer | Message | Then |
  |---|---|---|
  | 409 `SLOT_NOT_AVAILABLE` | the `detail` | back to **Date**, with the slots reloaded and no date chosen |
  | 409 `EXPERT_NOT_ELIGIBLE` | the `detail` | back to **Expert**, with the experts reloaded and none chosen |
  | 409 `EXPERT_CHANGED` | the `detail` | the case reloads; on to **Date** with the case's expert |
  | 409 `NO_OPEN_CREDITS` | the `detail` | only **Close** is left; the case reloads |
  | 404, of the booking or of the times | "This case was not found." | only **Close** is left |
  | anything else | "Booking failed. Please try again." | stays on **Confirm**; for the times, "The times could not be loaded. Please try again." on **Expert** |

  The case reloads after a 409 while the dialog stays open: unlike the buy dialog, the dialog isn't a child of something the reload removes.

### Case detail

03. `ConsultationCasePage` gains:

| Part | Content |
|---|---|
| Expert, in the terms | the expert's name, or "Not assigned yet" |
| **Book appointment** | under the terms; disabled unless `canBook`. While the case has no appointment, "Book your first session" next to it |
| Your expert | with an expert: `ImagePlaceholder`, name, bio, as the shop's offering detail shows experts (spec decision 3) |
| Upcoming appointments | each "17 Nov 2026, 10:00–11:00", soonest first, with "(Zurich time)" once; a `CONSUMED` one, which is running, says "Now"; hidden when empty |
| Past appointments | each with "Consumed", newest first, with "(Zurich time)" once; hidden when empty |

### My purchases

04. `MyPurchasesPage` shows the groups the API returns, in its order: **Upcoming**, **Needs booking**, and **Past** collapsed (`<details>` with the summary "Past (2)"). An empty group isn't shown; without any case, the empty state stays. `ConsultationCaseCard` shows the expert's name or "Not assigned yet", "Next: 17 Nov 2026, 10:00–11:00" with an upcoming appointment, or "Now: …" while it runs, the credits and the purchase date. "(Zurich time)" sits next to the **Upcoming** heading, the only group with a time on its cards (a Past case shows none), as the case detail puts it next to its appointments' headings ([frontend.md](../../architecture/frontend.md#dates-times-and-money)).

### Queries

- **Query keys:** `['expert', 'availability']` (01). Under the case's key, so that invalidating the cases reloads them too: `['customer', 'consultation-cases', id, 'eligible-experts']` and `['customer', 'consultation-cases', id, 'bookable-slots', expertId]` (03).
- **`useSaveAvailability`** puts the answer into `['expert', 'availability']`.
- **`useBookConsultationAppointment(caseId)`** invalidates `['customer', 'consultation-cases']`: the list, every case, its experts and its slots, after a booking and after a 409, as [frontend.md](../../architecture/frontend.md#forms)'s mutations do. So on a 409 the case reloads, and with it what the error table says, before the dialog changes its step.

### Dates

`lib/date.ts` stays the one place that turns API dates and instants into text and back, always in `Europe/Zurich`:

| Function | Example | Task |
|---|---|---|
| `formatLocalDate(localDate)` | `2026-11-17` → 17 Nov 2026 | 01 |
| `todayInZurich()` | 2026-11-09 | 01 |
| `toCalendarDay(localDate)`, `fromCalendarDay(date)` | `2026-11-17` ↔ the calendar's `Date` for that day in Zurich | 01 |
| `formatTime(instant)` | 10:00 | 03 |
| `localDateOf(instant)` | `2026-11-17T23:30:00Z` → 2026-11-18 | 03 |

Each gets Vitest tests in summer and winter time and around midnight UTC; `localDateOf` and `formatTime` also on the days of the summer-time changes. [frontend.md](../../architecture/frontend.md#dates-times-and-money) gets the functions.

### Components

shadcn/ui: `calendar` (01), added with the CLI and then `npm run format`. It brings `react-day-picker`, whose `timeZone` prop shows the days in Zurich whatever the browser's zone.

## Tests

| What | How |
|---|---|
| Test clock | `TestClock` (01), a `Clock` in Zurich whose instant a test sets (`set(Instant)`), registered as the `@Primary` clock by a test configuration that `@IntegrationTest` imports. `@IntegrationTest` resets it to the system clock after each test, so a test that forgets doesn't move the others' time. Domain tests pass `now` in, as before (spec decision 19). |
| Test data | No seed availability (spec decision 8). Tests save availability through the API as Expert 1 or Expert 2, buy through the API, and book through the API. `delete-test-data.sql` deletes every appointment (03), then every case, then every availability date and range (01), then the test offerings, as before. |
| Times | A REST test that books sets the clock first, for example to Mon 9 Nov 2026, 15:00 (Zurich), and makes the expert available on later dates. A test of consumed appointments or completed cases books, then moves the clock past the appointments. |
| Role matrix | Rows for the two availability routes, `EXPERT` (01); the `GET /api/expert/probe` row goes. Rows for the three case routes, `CUSTOMER`, on an unknown case (03). `PUT` and `POST` send no body (400). |
| Ownership | Expert 2's `GET` doesn't show Expert 1's availability (01). Customer 2 on Customer 1's case: the experts, the slots and the booking answer 404, and nothing is booked (03). Customer 2's list doesn't show Customer 1's appointments (04). Customer 2's appointment takes none of Customer 1's times (spec decision 22). |
| Concurrency | Two tests (05), deterministic as `ConsultationOfferingConcurrencyTest`: two customers book one expert's slot at once, and must fail without the expert's lock; two bookings on a case's last credit at once, with different experts so that only the case's lock can order them, and must fail without the case's lock. Where a request without its lock would wait nowhere, the test also holds the row a foreign key check of the request waits for, as the purchase's test does. The customer's own appointments aren't locked, so they have no concurrency test (spec decision 22). |
| Domain | `TimeRange`, `Availability` (overlap, touch, past dates) (01). `SlotFinder`: the grid, the fit, two ranges, the notice, busy time with and without touching, the session durations 30, 60 and 90 minutes, a range not on the grid, both days of the summer-time changes, no availability (02). `CaseForBooking`: each code, credits before the expert, the first booking assigns the expert, the slot's end and free-cancellation time (03). `CustomerConsultationCase`: credits, `canBook`, upcoming and past at a given time, also at an appointment's start and end (03). `CustomerConsultationCaseSummary`: each group and the order, also at an appointment's start and end (04). |
| Frontend | Vitest for logic only ([D-29](../../decisions.md#architecture)): the new `lib/date.ts` functions (01, 03), and the dialog's grouping of slots by date if it's a function of its own (03). Pages and the dialog are checked by hand with Playwright. |

## Commands

No new commands. `npx shadcn@latest add calendar` is the existing command for a shadcn/ui component. A task that adds a command also adds it to the **Commands** section of [AGENTS.md](../../../AGENTS.md#commands).
