# 03 Book an appointment

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [01](01-manage-availability.md), [02](02-bookable-slots.md) |
| Status | Todo |

## Goal

A customer clicks **Book appointment** on a case, picks the expert on the first booking, then a date and a time, confirms, and sees the appointment and the reduced credits on the case detail. The server re-checks every rule under the case's and the expert's locks. This task builds booking from the schema to the dialog, and sets the pattern for a two-row lock.

## What to build

Read first: [UC-06](../../use-cases/uc-06-book-consultation-appointment.md), the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), [D-06](../../decisions.md#catalog), [D-07](../../decisions.md#catalog), [D-10](../../decisions.md#consultation), [D-11](../../decisions.md#consultation), [D-27](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), the spec's decisions 2–7, 12 and 15–18, and the plan's [packages and names](README.md#packages-and-names), [schema](README.md#schema), [API](README.md#api), [booking](README.md#booking), [ownership](README.md#ownership), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it), [booking dialog](README.md#booking-dialog), [case detail](README.md#case-detail), [queries](README.md#queries) and [dates](README.md#dates) sections. Task 01 of [increment 03](../03-buy-consultation-package/01-buy-from-detail-page.md) is the pattern for a customer route, scoped queries and a dialog with steps.

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

- [ ] On the dev server, after Expert 2 saved availability, Customer 1 buys Career Coaching, sees "Book your first session", books Expert 2 through all four steps, and the case shows Expert 2, 2 of 3 credits open and the appointment in Zurich time; the confirmation reads as the spec's "The confirmation states the terms" (Playwright).
- [ ] A second booking skips the expert step; the booked time and the times overlapping it aren't offered; touching ones are.
- [ ] Closing the dialog at any step books nothing and assigns no expert.
- [ ] With three appointments booked, **Book appointment** is disabled, and a booking request answers 409 `NO_OPEN_CREDITS`.
- [ ] A time booked meanwhile by Customer 2 answers 409 `SLOT_NOT_AVAILABLE`, and the dialog goes back to the dates with the times reloaded (Playwright with a second browser context).
- [ ] An expert removed from the offering meanwhile answers 409 `EXPERT_NOT_ELIGIBLE` on a first booking, and the dialog shows the current experts; a case whose expert was removed later still books them.
- [ ] A first booking naming another expert than the case's answers 409 `EXPERT_CHANGED`.
- [ ] An offering without eligible experts shows "No experts are available for this offering yet. Please try again later."; an expert without slots shows "No free times available at the moment."
- [ ] Expert 1, who is also a customer, books themself.
- [ ] A case of an archived offering books.
- [ ] An invalid booking request (no expert, no start) answers 400 with each field in `errors`; a start that isn't a slot answers 409 `SLOT_NOT_AVAILABLE`.
- [ ] Customer 2 gets 404 on Customer 1's case from all three routes, and nothing is booked.
- [ ] With the clock moved past an appointment, the case answers it under `pastAppointments` as `CONSUMED`, and it still counts against the credits.
- [ ] `openapi.json` and `schema.d.ts` have the three routes and the new fields; the role matrix has the three rows; every check passes.

## Tests

- Domain: `CaseForBooking` (each code; credits before the expert; the first booking assigns; the end and the free-cancellation time), `CustomerConsultationCase` (credits, `canBook`, upcoming and past at `asOf`), `Appointment`, `BookableSlot`, `CancellationDeadline.freeUntil`
- REST: the happy path of the three routes and the extended detail; the 409s, 404s and 400s above; the archived offering; self-booking; removing eligibility after the assignment; consumed after moving the clock
- Authorization: `RoleMatrixTest` rows; Customer 2 on Customer 1's case → 404 on each route
- Frontend: `formatTime`, `localDateOf`

## Out of scope

- The list: the expert, the next appointment and the groups on **My purchases**: [04](04-my-purchases-groups.md). Until then the card still says "Not assigned yet".
- The concurrency tests: [05](05-booking-races.md). This task's booking already takes both locks.
- Cancelling an appointment, the **Join** link, notes: later increments.

## Notes

