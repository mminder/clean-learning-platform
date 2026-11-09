# 03 Book an appointment

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [01](01-manage-availability.md), [02](02-bookable-slots.md) |
| Status | Done |

## Goal

A customer clicks **Book appointment** on a case, picks the expert on the first booking, then a date and a time, confirms, and sees the appointment and the reduced credits on the case detail. The server re-checks every rule under the case's and the expert's locks. This task builds booking from the schema to the dialog, and sets the pattern for a two-row lock.

## What to build

Read first: [UC-06](../../use-cases/uc-06-book-consultation-appointment.md), the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), [D-06](../../decisions.md#catalog), [D-07](../../decisions.md#catalog), [D-10](../../decisions.md#consultation), [D-11](../../decisions.md#consultation), [D-27](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), the spec's decisions 2–7, 12, 15–18 and 21, and the plan's [packages and names](README.md#packages-and-names), [schema](README.md#schema), [API](README.md#api), [booking](README.md#booking), [ownership](README.md#ownership), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it), [booking dialog](README.md#booking-dialog), [case detail](README.md#case-detail), [queries](README.md#queries) and [dates](README.md#dates) sections. Task 01 of [increment 03](../03-buy-consultation-package/01-buy-from-detail-page.md) is the pattern for a customer route, scoped queries and a dialog with steps.

Work in this order, test first:

1. **A failing REST test:** with the clock at Mon 9 Nov 2026, 15:00 (Zurich), Expert 2 available on 17 Nov, 09:00–12:00, and Customer 1's Career Coaching case: the eligible experts are Expert 1 and Expert 2; the slots of Expert 2 are the five of the spec's "Grid and fit"; booking 10:00 answers 201; the case answers Expert 2, 2 of 3 credits open, and the appointment under `upcomingAppointments`.
2. **Schema:** `V8__create_appointment.sql`, as the plan's schema section says, with the `updated_at` trigger.
3. **Domain**, test first:
   - `AppointmentId`, `AppointmentStatus`, `BookableSlot`; `CancellationDeadline.freeUntil`;
   - `Appointment`, `CustomerAppointment`, `AssignedExpert`, `Booking`;
   - `CaseForBooking` with its four methods and codes, as the plan's booking table says: credits before the expert; `book` checks everything itself;
   - `CustomerConsultationCase`: the expert, the appointments at `asOf`, the credits and `canBook()`.
4. **Ports and adapter:** `LoadCaseForBookingPort`, `LockCaseForBookingPort` (`@Lock(PESSIMISTIC_WRITE)` on a query scoped to the customer), `LoadBusyTimesPort`, `SaveBookingPort`; `LoadCustomerConsultationCasePort.load` takes `asOf`, and its adapter loads the expert and the appointments. `ConsultationCaseEntity` maps `assignedExpertId`; `AppointmentEntity` is new.
5. **Use cases:** `ListEligibleExpertsForCaseUseCase`, `ListBookableSlotsUseCase`, `BookConsultationAppointmentUseCase` (lock the case, choose the expert, lock the expert, load, decide, write, answer), as the plan's booking section says. `GetCustomerConsultationCaseUseCase` and `PurchaseConsultationPackageUseCase` pass `asOf` from the `Clock`.
6. **API:**
   - three routes on `CustomerConsultationCaseController`, the request and the responses as the plan shows them, `ProblemResponses` per route; the booking's 201 has no `Location`;
   - `CustomerConsultationCaseResponse` gains `expert`, `upcomingAppointments`, `pastAppointments` and `canBook`;
   - `EligibleExpertResponse` moves to `api.response`;
   - role matrix rows for the three routes;
   - regenerate `openapi.json`, then `npm run generate:api`.
7. **Test data:** `delete-test-data.sql` deletes every appointment before the cases.
8. **Frontend:**
   - `lib/date.ts`: `formatTime` and `localDateOf`, test first;
   - `features/consultation/queries.ts`: `useEligibleExperts`, `useBookableSlots`, `useBookConsultationAppointment`;
   - `BookAppointmentDialog`, as the plan's booking dialog section says, with the error table;
   - `ConsultationCasePage`: the expert, **Book appointment** with the hint, the upcoming and past appointments, as the plan's case detail section says.
9. **Docs:** [backend.md](../../architecture/backend.md#rest-conventions): a `POST` that creates answers a `Location` when the resource has a route of its own, and the booking's appointment hasn't; [frontend.md](../../architecture/frontend.md#dates-times-and-money) gets `formatTime` and `localDateOf`; check that UC-06's error flows match the plan's `detail`s (aligned while planning).

## Acceptance criteria

- [x] On the dev server, after Expert 2 saved availability, Customer 1 buys Career Coaching, sees "Book your first session", books Expert 2 through all four steps, and the case shows Expert 2, 2 of 3 credits open and the appointment in Zurich time; the confirmation reads as the spec's "The confirmation states the terms" (Playwright).
- [x] A second booking skips the expert step; the booked time and the times overlapping it aren't offered; touching ones are.
- [x] Closing the dialog at any step books nothing and assigns no expert.
- [x] With three appointments booked, **Book appointment** is disabled, and a booking request answers 409 `NO_OPEN_CREDITS`.
- [x] A time booked meanwhile by Customer 2 answers 409 `SLOT_NOT_AVAILABLE`, and the dialog goes back to the dates with the times reloaded (Playwright with a second browser context).
- [x] An expert removed from the offering meanwhile answers 409 `EXPERT_NOT_ELIGIBLE` on a first booking, and the dialog shows the current experts; a case whose expert was removed later still books them.
- [x] A first booking naming another expert than the case's answers 409 `EXPERT_CHANGED`.
- [x] An offering without eligible experts shows "No experts are available for this offering yet. Please try again later."; an expert without slots shows "No free times available at the moment."
- [x] Expert 1, who is also a customer, books themself.
- [x] A case of an archived offering books.
- [x] An invalid booking request (no expert, no start) answers 400 with each field in `errors`; a start that isn't a slot answers 409 `SLOT_NOT_AVAILABLE`.
- [x] Customer 2 gets 404 on Customer 1's case from all three routes, and nothing is booked.
- [x] With the clock moved into an appointment, the case answers it under `upcomingAppointments` as `CONSUMED`, and the page says "Now"; moved to its end, under `pastAppointments`; either way it counts against the credits (spec decision 21).
- [x] `openapi.json` and `schema.d.ts` have the three routes and the new fields; the role matrix has the three rows; every check passes.

## Tests

- Domain: `CaseForBooking` (each code; credits before the expert; the first booking assigns; the end and the free-cancellation time), `CustomerConsultationCase` (credits, `canBook`, upcoming and past at `asOf`, at an appointment's start and end), `Appointment`, `BookableSlot`, `CancellationDeadline.freeUntil`
- REST: the happy path of the three routes and the extended detail; the 409s, 404s and 400s above; the archived offering; self-booking; removing eligibility after the assignment; consumed after moving the clock into and past an appointment
- Authorization: `RoleMatrixTest` rows; Customer 2 on Customer 1's case → 404 on each route
- Frontend: `formatTime`, `localDateOf`

## Out of scope

- The list: the expert, the next appointment and the groups on **My purchases**: [04](04-my-purchases-groups.md). Until then the card still says "Not assigned yet".
- The concurrency tests: [05](05-booking-races.md). This task's booking already takes both locks.
- Cancelling an appointment, the **Join** link, notes: later increments.

## Notes

- **The shared appointment rules** are on `Appointment`: `statusAt`, `isUpcomingAt`, `at(asOf)` (the `CustomerAppointment`), and the static `Appointment.upcoming(appointments, asOf)` (not ended, soonest first) and `Appointment.past(appointments, asOf)` (ended, newest first). `CustomerConsultationCase` only calls them; task 04's `nextAppointment()` is the first of `upcoming`, and Past's order the first of `past`, without a copy.
- **Open credits** are one rule, the static `Appointment.openCredits(sessionCount, bookedAppointments)`: `CaseForBooking.openCredits()` passes the counted `BOOKED` appointments, `CustomerConsultationCase.openCredits()` the size of its appointments, so `canBook` and `NO_OPEN_CREDITS` can't drift apart (moved there in the review; the implementation first wrote the subtraction in both). Until task 04, `CustomerConsultationCaseSummary` still counts every credit as open, so **My purchases** shows "3 of 3 credits open" after a booking: task 04 gives the summary its appointments and counts with the same rule (a TODO on `CustomerConsultationCaseSummary.openCredits()` names it).
- **`Booking`** is `(Appointment appointment, UserId expertId, boolean assignsExpert)`: `SaveBookingPort` needs the expert's id to assign them.
- **Persistence:** the lock is a Spring Data query with `@Lock(PESSIMISTIC_WRITE)`, scoped to the customer; Hibernate sends `select … from consultation_case … where id = ? and customer_id = ? for no key update`, then the booking's `select … from app_user … for no key update` (checked in the SQL log). `AppointmentEntity` maps `status` with the domain's `AppointmentStatus` (`@Enumerated(STRING)`), and only `BOOKED` is ever written; every appointment query filters `status = BOOKED`. The first booking updates the managed case entity, so Hibernate writes the case's row with all its columns; only `assigned_expert_id` changes, so only its foreign key is checked (`expert_profile`, which task 05's first test holds). The eligible experts' query selects into the offering adapter's `EligibleExpertRow`, the same shape the shop reads.
- **The use case reads the clock after both locks**, so a booking that waited for them checks the 24 hours' notice at the time it decides.
- **The dialog**, beyond the plan's table, now recorded in the plan's [booking dialog](README.md#booking-dialog): the expert step's **Continue** loads the expert's slots first, so a 409 of the slots route (`EXPERT_CHANGED`, `EXPERT_NOT_ELIGIBLE`) follows the same table; and the dialog keeps the expert it opened with, or the picked one, so a refetch of the case on window focus can't swap the expert under the confirmation (the spec's two-tabs scenario then answers `EXPERT_CHANGED`, as it should). The mutation reloads the cases after a 409, as frontend.md's mutations do; the plan's [queries](README.md#queries) says so now. `booking-failure.ts`, `bookable-slots.ts` (`slotsByDate`) and `appointment-times.ts` (`formatTimes`, `formatDateAndTimes`) have Vitest tests.
- **"(Zurich time)"** is next to both appointment headings, Past too (plan's case detail updated).
- **Tests:** `ConsultationCaseBookingTest` (23 REST tests: the happy path of the three routes and the extended detail, every 409, 404 and 400, the archived offering, self-booking, eligibility removed before and after the assignment, no experts, no slots, removed availability keeps the appointment, consumed and past after moving the clock); domain: `AppointmentTest`, `CaseForBookingTest`, `CustomerConsultationCaseTest`, `BookableSlotTest`, `CancellationDeadlineTest.freeUntil`; `OpenApiContractTest` checks the three routes' answers.
- **Playwright** (by hand, against the dev server on the real clock, 12 and 13 Nov 2026): Expert 2 saves availability; Customer 1 buys Career Coaching, sees "Book your first session" and "Not assigned yet", and books through all four steps: Expert 1 and Expert 2 with bios, only 12 and 13 Nov enabled, twelve times on 12 Nov, the confirmation "Book Expert 2 on 12 Nov 2026, 10:00–11:00 (60 min, Zurich time)?" and "Free cancellation until 11 Nov 2026, 10:00."; the case then shows Expert 2, "Your expert", 2 of 3 open and the appointment under "Upcoming appointments (Zurich time)". A second booking starts at the date with **Cancel**, and 12 Nov offers 09:00–10:00 and 11:00–12:00 but not 09:30, 10:00 or 10:30. Closing at the expert, date, time and confirm steps (**Cancel**, Escape, the close button) books nothing and assigns no expert. Customer 2 in a second browser context: Expert 1 without availability shows "No free times available at the moment."; at the confirmation of 11:00–12:00 while Customer 1 books it, **Book** shows "This time is no longer available." and goes back to the dates with none chosen, and the reloaded times lack 11:00–12:00. With Admin 1 removing Expert 2 from Career Coaching meanwhile, **Book** shows the `EXPERT_NOT_ELIGIBLE` detail and the experts reloaded (Expert 1 only, none chosen); Customer 1's case still books Expert 2, and at 0 of 3 **Book appointment** is disabled. Without eligible experts the dialog says "No experts are available for this offering yet. Please try again later.". Two tabs of Customer 2: after Expert 1 is booked in one, **Book** with Expert 2 in the other shows the `EXPERT_CHANGED` detail and goes on to Expert 1's dates, and the confirmation names Expert 1. Expert 1 books themself. Customer 2 sees "Case not found" for Customer 1's case. "Now" and "Consumed" on the page were checked with the case's answer intercepted, as at a moved clock; the API's statuses are REST-tested.
- **The bundle** is 573 kB (Vite's 500 kB warning was there before).
