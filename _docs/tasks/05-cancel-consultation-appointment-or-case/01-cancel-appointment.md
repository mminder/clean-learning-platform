# 01 Cancel an appointment

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

A customer cancels an upcoming appointment from the case detail. In time, the credit comes back; late, after a warning, it's forfeited. The appointment leaves the case, and its time is bookable again. Every count of open credits subtracts the forfeited ones.

## What to build

Read first: [UC-07](../../use-cases/uc-07-cancel-consultation-appointment.md), the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), [D-11](../../decisions.md#consultation), [D-27](../../decisions.md#architecture), the spec's decisions 1 to 4, 8, 11 and 12, its requirements [Cancel an appointment](../../specs/05-cancel-consultation-appointment-or-case.md#requirement-cancel-an-appointment) and [Open credits count forfeited credits](../../specs/05-cancel-consultation-appointment-or-case.md#requirement-open-credits-count-forfeited-credits), and the plan's [schema](README.md#schema), [API](README.md#api), [credits and completion](README.md#credits-and-completion), [cancelling an appointment](README.md#cancelling-an-appointment), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it), [the cancellation dialogs](README.md#the-cancellation-dialogs) and [tests](README.md#tests) sections. Increment 04's booking is the pattern for every layer: `BookConsultationAppointmentUseCase`, `CaseForBooking`, `ConsultationCaseBookingTest`, `BookAppointmentDialog` and `booking-failure.ts`.

Work in this order, test first:

1. **A failing REST test:** with the clock at Mon 9 Nov 2026, 15:00 (Zurich), Customer 1 buys Career Coaching and books Expert 2 on 17 Nov, 10:00–11:00. With the clock at Sun 15 Nov, 12:00, the case's upcoming appointment has `canCancel`, `freeCancellationUntil` 16 Nov, 10:00, and `creditRefundedIfCancelledNow`; `POST …/appointments/{appointmentId}/cancel` with `{"creditRefunded": true}` answers 200 with the case: 3 of 3 credits open, no appointment listed. The slots of Expert 2 offer 17 Nov, 10:00 again.
2. **Domain:** `Appointment`'s cancellation rules and `openCredits` with forfeited credits; `AppointmentCancellation`; `CaseForCancellation` with `cancelAppointment` and its codes; `CustomerUpcomingAppointment`; `CaseForBooking`, `CustomerConsultationCase` and `CustomerConsultationCaseSummary` count forfeited credits, all through `Appointment.openCredits`; the summary's Past key falls back to the purchase time (plan).
3. **Schema and adapter:** `V9__cancel_appointment.sql`; `StoredAppointmentStatus.CANCELLED` and `AppointmentEntity.creditRefunded`; `ConsultationCaseAdapter` implements `LockCaseForCancellationPort` and `SaveAppointmentCancellationPort`, and counts the forfeited credits for the detail, the list and the booking.
4. **Use case and API:** `CancelConsultationAppointmentUseCase` (lock, time, decide, write, answer); the route with `CancelConsultationAppointmentRequest`, `CustomerUpcomingAppointmentResponse` in the case detail; a role matrix row. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** `useCancelConsultationAppointment`; `cancellation-failure.ts` with the appointment's codes; `CancelAppointmentDialog`, rendered once by `ConsultationCasePage`; **Cancel** on each upcoming appointment with `canCancel`.
6. **Check by hand** on the dev server with Playwright: the dialog's texts in time and late (a 72-hour deadline makes a booking late from the start, as the spec's demo does), **Keep appointment**, the cancellation, the credits, and the freed time in the booking dialog; then cancel one appointment in two tabs, and see the second tab's message and reload.

## Acceptance criteria

- [ ] In time: 200, the credit is back, the appointment is no longer listed, and its time is offered again (REST).
- [ ] Late (clock at 16 Nov, 11:00, `{"creditRefunded": false}`): 200, 2 of 3 credits open, and the appointment is no longer listed (REST).
- [ ] At the deadline exactly the cancellation is late, and on both days of the summer-time changes the deadline is 24 real hours before the start (domain tests).
- [ ] 409 `DEADLINE_PASSED` for `{"creditRefunded": true}` at 16 Nov, 10:01; 409 `APPOINTMENT_STARTED` at 17 Nov, 10:00; 409 `APPOINTMENT_CANCELLED` for a second cancellation; each with the plan's `detail`, and nothing changes (REST).
- [ ] With every credit booked, a cancellation in time enables booking again (`canBook`); a case of 3 sessions with one consumed, one cancelled in time and one cancelled late has 1 open credit and lists only the consumed one (REST).
- [ ] A case of 1 session whose appointment was cancelled late has 0 of 1 credits open and is `PAST` on **My purchases**, sorted by its purchase time (REST and domain).
- [ ] A case of an archived offering can still cancel (REST).
- [ ] Customer 2 cancelling Customer 1's appointment, and Customer 1 cancelling an appointment of another of their cases under this case's path, answer 404, and nothing changes; the role matrix has the route.
- [ ] The dialog follows the plan's table and error table: it reloads its preview when it opens, names the appointment, says **Keep appointment** and **Cancel appointment**, stays open on a 409 and shows what's current, and a reload that removes the appointment doesn't close it (Playwright).
- [ ] The case detail shows no free-cancellation time (spec decision 1) and no cancelled appointment (spec decision 3).
- [ ] `openapi.json` and `schema.d.ts` have the new route and fields; every check passes.

## Tests

- Domain: `Appointment` (the cancellation rules, `openCredits`), `CaseForCancellation.cancelAppointment` (each outcome and code, in order), `CustomerConsultationCase` (upcoming appointments' preview, credits), `CustomerConsultationCaseSummary` (credits, the Past key without an appointment), `CaseForBooking` (credits with forfeited ones)
- REST: the route's happy path, late, the three 409s, credits and lists after cancellations, the freed time, an archived offering's case
- Authorization: the role matrix row; ownership with both seeded customers, and an appointment of another case
- Frontend: Vitest for `cancellationFailure`

## Out of scope

- Cancelling a case, its status and `CASE_CANCELLED`: [03](03-cancel-case.md).
- Refund eligibility: [02](02-refund-eligibility.md).
- Cancelled cases on **My purchases**: [04](04-cancelled-cases-on-my-purchases.md).
- The race of a case cancelled during a booking: [05](05-cancellation-race.md). Two cancellations of one appointment need no concurrency test (spec decision 10).

## Notes
