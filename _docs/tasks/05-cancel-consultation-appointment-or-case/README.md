# 05 Cancel a consultation appointment or case: technical plan

Spec: [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same change. Everything from the plans of increments [01](../01-shop-tracer-bullet/README.md) to [04](../04-book-consultation-appointment/README.md) still holds: toolchain, test setup and test clock, error conventions, writes, locks, forms, mutations, customer-owned data, the appointments' rules on `Appointment`, and `lib/date.ts`.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [Cancel an appointment](01-cancel-appointment.md) | AFK | – |
| 02 | [Refund eligibility](02-refund-eligibility.md) | AFK | – |
| 03 | [Cancel a case](03-cancel-case.md) | AFK | 01, 02 |
| 04 | [Cancelled cases on My purchases](04-cancelled-cases-on-my-purchases.md) | AFK | 03 |
| 05 | [A case cancelled during a booking](05-cancellation-race.md) | AFK | 03 |

```
01 Cancel an appointment ──┐                  ┌─► 04 Cancelled cases on My purchases
                           ├─► 03 Cancel a ───┤
02 Refund eligibility ─────┘   case           └─► 05 A case cancelled during a booking
```

01 builds the appointment's cancellation from the schema to the dialog, and with it the forfeited credit in every count: it sets the pattern for a cancellation, a dialog that reloads its preview and confirms again on a changed outcome. Review it with extra care, as task 03 of increment 04. 02 is the refund rule, in the domain only. 01 and 02 run in parallel, each in its own git worktree ([D-33](../../decisions.md#scope-and-docs)); then 03 builds the case's cancellation on both. 04 and 05 then run in parallel, each in its own git worktree:

| Task | Touches | Shared with another task |
|---|---|---|
| 01 | `V9`; `Appointment`, `CaseForBooking`, `CustomerConsultationCase`, `CustomerConsultationCaseSummary` (forfeited credits), new `CaseForCancellation`, `AppointmentCancellation`, `CustomerUpcomingAppointment`; the adapter, repository and entities; the controller; `ConsultationCasePage`, `CancelAppointmentDialog`, `cancellation-failure.ts`, `queries.ts`; `openapi.json` | – |
| 02 | new: `RefundEligibility`, `NonRefundableReason`, their tests | – |
| 04 | the list route: `CustomerConsultationCaseSummary`, its row, query and response; `ConsultationCaseCard`; `openapi.json` | – |
| 05 | `ConsultationCaseConcurrencyTest` | – |

Only 01, 03 and 04 change `openapi.json` and `schema.d.ts`, one after the other.

## Backend

### Packages and names

| Package | Classes | Task |
|---|---|---|
| `domain.consultation` | `Appointment` gains the cancellation's rules; `openCredits` takes the forfeited credits | 01 |
| `domain.consultation` | `CaseForCancellation` (the case as a cancellation needs it; holds UC-07's and UC-08's rules), `AppointmentCancellation` (what cancelling an appointment writes), `CustomerUpcomingAppointment` (an upcoming appointment as its customer sees it, with its cancellation preview); `CaseForBooking`, `CustomerConsultationCase` and `CustomerConsultationCaseSummary` count forfeited credits | 01 |
| `domain.consultation` | `RefundEligibility` (UC-08's rule and its first reason) | 02 |
| `domain.consultation.value` | `NonRefundableReason` (`SESSION_TOOK_PLACE`, `CREDIT_FORFEITED`, `DEADLINE_PASSED`, in UC-08's order) | 02 |
| `domain.consultation` | `CaseCancellation` (what cancelling a case writes); `CaseForCancellation.cancel`; `CaseForBooking` gains the status; `CustomerConsultationCase` gains the cancellation, `canCancel()` and `refundIfCancelledNow()` | 03 |
| `domain.consultation.value` | `CaseStatus` (`ACTIVE`, `CANCELLED`, both stored), `CancellationOutcome` (a cancelled case's `cancelledAt` and `refundEligible`) | 03 |
| `domain.consultation` | `CustomerConsultationCaseSummary` gains `cancelledAt`; cancelled cases are Past | 04 |
| `application.consultation` | `CancelConsultationAppointmentUseCase` with `CancelConsultationAppointmentCommand` | 01 |
| `application.consultation` | `CancelConsultationCaseUseCase` with `CancelConsultationCaseCommand` | 03 |
| `application.consultation.port` | `LockCaseForCancellationPort` (`lockForCancellation(ConsultationCaseId, UserId customerId)`: not `lock`, which `LockCaseForBookingPort` already declares with the same parameters on the same adapter), `SaveAppointmentCancellationPort` (`save(AppointmentCancellation)`) | 01 |
| `application.consultation.port` | `SaveCaseCancellationPort` (`save(CaseCancellation)`) | 03 |
| `adapters.database.consultation` | `ConsultationCaseAdapter` implements the new ports; `entity`: `StoredAppointmentStatus` gains `CANCELLED`, `AppointmentEntity` maps `creditRefunded` | 01 |
| `adapters.database.consultation` | `entity`: `ConsultationCaseEntity` maps `status`, `cancelledAt` and `refundEligible` | 03 |
| `api.consultation` | `CustomerConsultationCaseController` gains the appointment's cancel route; `request`: `CancelConsultationAppointmentRequest`; `response`: `CustomerUpcomingAppointmentResponse` | 01 |
| `api.consultation` | the case's cancel route; `request`: `CancelConsultationCaseRequest`; `response`: `CancellationOutcomeResponse`, `RefundEligibilityResponse`, `DeadlinePassedAppointmentResponse` | 03 |

- **Names** follow the [glossary](../../glossary.md): cancellation, free cancellation, forfeited credit, refund-eligible, case status. The customer's records carry the audience, `Customer…`, as in increments 03 and 04.
- **The domain's `AppointmentStatus` stays `BOOKED` and `CONSUMED`**: the customer never sees a cancelled appointment (spec decision 3), so no customer type carries `CANCELLED`. Only the stored enum, `StoredAppointmentStatus`, gains it. The expert's side (increment 06) adds what it needs.
- **The use cases take the `CurrentUser`**, and every case query is scoped to the customer, as in increments 03 and 04.

### Schema

| Migration | Task | Content |
|---|---|---|
| `V9__cancel_appointment.sql` | 01 | the appointment's `CANCELLED` and `credit_refunded` |
| `V10__cancel_consultation_case.sql` | 03 | the case's `status`, `cancelled_at` and `refund_eligible` |

```
appointment        status           check (status in ('BOOKED', 'CANCELLED'))   -- replaces the check of V8
                   + credit_refunded boolean, null while BOOKED,
                     check ((status = 'CANCELLED') = (credit_refunded is not null))

consultation_case  + status          text not null, check (status in ('ACTIVE', 'CANCELLED')); existing rows ACTIVE
                   + cancelled_at    timestamptz, from the application's Clock
                   + refund_eligible boolean
                     check ((status = 'CANCELLED') = (cancelled_at is not null))
                     check ((status = 'CANCELLED') = (refund_eligible is not null))
```

- **Only what exists is stored** (spec decision 11): `credit_refunded`, `cancelled_at` and `refund_eligible` are set exactly when the row is cancelled, and the checks say so ([D-36](../../decisions.md#architecture)). The appointment stores no cancellation time: nothing shows one.
- **V8's check on `appointment.status`** is replaced, not added to: find its name in the database (Postgres names an inline column check `appointment_status_check`), drop it and add the new one.
- **The case's `status`** gets `default 'ACTIVE'` only to fill the existing rows; the migration drops the default afterwards, and the entity writes `ACTIVE` at the purchase.
- **No new index**: every new query reads a case's appointments through `appointment_consultation_case_id_idx`, or the customer's through their cases.
- The test cleanup already deletes every appointment and case; it doesn't change.

### API

| Route | Answer | Task |
|---|---|---|
| `POST /api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel` | `CancelConsultationAppointmentRequest` → 200 `CustomerConsultationCaseResponse`; 400; 404; 409 (`APPOINTMENT_CANCELLED`, `APPOINTMENT_STARTED`, `DEADLINE_PASSED`); 415 | 01 |
| `POST /api/customer/consultation-cases/{id}/cancel` | `CancelConsultationCaseRequest` → 200 `CustomerConsultationCaseResponse`; 400; 404; 409 (`CASE_CANCELLED`, `CASE_COMPLETED`, `REFUND_ELIGIBILITY_CHANGED`); 415 | 03 |
| `GET /api/customer/consultation-cases/{id}` | as before; the upcoming appointments carry their cancellation preview (01); plus `cancellation`, `canCancel` and `refundIfCancelledNow`, and `canBook` is false for a cancelled case (03) | 01, 03 |
| `GET /api/customer/consultation-cases/{id}/bookable-slots` and `POST /api/customer/consultation-cases/{id}/appointments` | as before, plus 409 `CASE_CANCELLED` | 03 |
| `GET /api/customer/consultation-cases` | as before, the credits count forfeited ones (01); plus `cancelledAt`, and a cancelled case is `PAST` (04) | 01, 04 |

401 and 403 come from security, as for every `/api/customer/**` route. Each method declares its errors with `ProblemResponses` and its success with `@ResponseStatus`. operationIds: `cancelConsultationAppointment`, `cancelConsultationCase`. A request's body is required: without one, Spring answers 400.

```jsonc
// CancelConsultationAppointmentRequest                                                                 01
{ "creditRefunded": true }   // @NotNull Boolean: the outcome the dialog stated (spec decision 4)

// CancelConsultationCaseRequest                                                                        03
{ "refundEligible": true }   // @NotNull Boolean: the outcome the dialog stated (spec decision 4)

// CustomerUpcomingAppointmentResponse: CustomerAppointmentResponse's fields, plus                      01
{
  "canCancel": true,                                // BOOKED at the time of the answer: it hasn't started
  "freeCancellationUntil": "2026-11-16T09:00:00Z",  // start − the case's cancellation deadline, for the late warning
  "creditRefundedIfCancelledNow": true              // now < freeCancellationUntil
}

// CustomerConsultationCaseResponse                                                                     01, 03
{
  /* increment 04's fields; openCredits counts forfeited credits (01) */
  "upcomingAppointments": [ /* CustomerUpcomingAppointmentResponse, soonest first (01) */ ],
  "cancellation": {                    // CancellationOutcomeResponse; @Nullable: an active case (03)
    "cancelledAt": "2026-11-15T11:00:00Z",
    "refundEligible": true
  },
  "canCancel": true,                   // the case is active and not completed (03)
  "refundIfCancelledNow": {            // RefundEligibilityResponse; @Nullable: when canCancel is false (03)
    "refundEligible": false,
    "reason": "DEADLINE_PASSED",       // @Nullable: when refund-eligible; or SESSION_TOOK_PLACE, CREDIT_FORFEITED
    "appointment": {                   // DeadlinePassedAppointmentResponse; @Nullable: only with DEADLINE_PASSED
      "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z",
      "freeCancellationUntil": "2026-11-16T09:00:00Z"
    }
  },
  "canBook": false                     // and the case is active (03)
}

// CustomerConsultationCaseSummaryResponse: increment 04's fields, plus                                 04
{ "cancelledAt": "2026-11-15T11:00:00Z" }   // @Nullable: an active case
```

- **Both cancellations answer the case** as its customer sees it after the cancellation, 200, as the purchase answers its new case; the dialog reloads the case anyway, as after a booking (spec decision 12). The use case loads it with `LoadCustomerConsultationCasePort` after the write: the query flushes the changes first.
- **The past appointments** stay `CustomerAppointmentResponse`, and so do the booking's answer and **My purchases**' `nextAppointment`: only the case detail's upcoming appointments need the preview.
- **`canCancel`, `freeCancellationUntil`, `creditRefundedIfCancelledNow`, `refundIfCancelledNow` and `group`** are computed by the server, as spec 04 decision 18 asks. The frontend never compares a time with a deadline.

### Credits and completion

01 for the forfeited credits, 03 for completion. One rule each, shared by every view, as [increment 04's plan](../04-book-consultation-appointment/README.md#the-case-as-its-customer-sees-it) asks.

- **`Appointment.openCredits(SessionCount sessionCount, int bookedAppointments, int forfeitedCredits)`** replaces the two-argument form: `total − BOOKED − forfeited` ([glossary](../../glossary.md#consultation)). `CaseForBooking`, `CustomerConsultationCase` and `CustomerConsultationCaseSummary` each gain `int forfeitedCredits`, which the adapter counts in SQL (`status = 'CANCELLED' and credit_refunded = false`), and count with it. Nothing else changes in the credits: a booked appointment uses a credit, consumed or not.
- **Completed** (03, spec decision 13): no open credits and no booked appointment that hasn't ended, `openCredits() == 0 && Appointment.upcoming(…, asOf).isEmpty()`. It's the Past group's condition for an active case, so `CustomerConsultationCase`, `CustomerConsultationCaseSummary` and `CaseForCancellation` all decide it through one static method on `Appointment` (`isCompleted(sessionCount, bookedAppointments, forfeitedCredits, asOf)` or similar), not three copies.
- **The Past order's key** (01, spec decision 8): a completed case's last appointment's start, or its purchase time when it has no booked appointment, because late cancellations forfeited every credit. Until 01, `lastAppointmentStart()` assumed an appointment; 01 makes it the fallback, and 04 puts a cancelled case's `cancelledAt` first.

### Cancelling an appointment

01, following UC-07 and spec decisions 3, 4 and 12.

- **`Appointment`'s rules** (01), at a given time, with the case's `CancellationDeadline`:

  | Method | Rule |
  |---|---|
  | `freeCancellationUntil(CancellationDeadline)` | `deadline.freeUntil(start)`: start − deadline, in real hours ([D-11](../../decisions.md#consultation)) |
  | `isCancellableAt(Instant asOf)` | `BOOKED` at `asOf`: the start is after `asOf`. A running appointment is consumed and can't be cancelled |
  | `isCreditRefundedIfCancelledAt(CancellationDeadline, Instant asOf)` | `asOf` is before `freeCancellationUntil`: at the deadline it's late |

- **`CaseForCancellation`** (01): the case's id, its purchase terms, its booked appointments (`List<Appointment>`), and its cancelled ones as `List<AppointmentCancellation>` (their ids and whether the credit was refunded). `forfeitedCredits()` counts the cancelled ones without a refund. Its rules, with the codes as constants next to them:

  | Method | Throws, in this order | `detail` |
  |---|---|---|
  | `cancelAppointment(AppointmentId, boolean creditRefundedConfirmed, Instant now)` | `NotFoundException`: the id is none of the case's appointments | Appointment <id> not found. |
  | | `APPOINTMENT_CANCELLED`: it's among the cancelled ones, in another tab or with its case | This appointment was already cancelled. |
  | | `APPOINTMENT_STARTED`: it isn't cancellable at `now` | This appointment can no longer be cancelled. |
  | | `DEADLINE_PASSED`: whether the credit is refunded at `now` isn't what was confirmed | The free cancellation deadline has just passed. Please confirm again. |

  It returns the `AppointmentCancellation`: the appointment's id, and whether its credit is refunded. From the dialog, only "refunded" can turn into "late", as time moves on and the purchase terms never change; a request that confirms "late" for a cancellation in time is refused with the same code, since the server never does what the customer didn't confirm (spec decision 4).
- **`CancelConsultationAppointmentUseCase.invoke(CurrentUser, CancelConsultationAppointmentCommand)`**, the command holding the case's id, the appointment's id and `creditRefunded`:
  1. **Lock the case** with `LockCaseForCancellationPort.lockForCancellation(id, customerId)`: `PESSIMISTIC_WRITE` through the query scoped to the customer that the booking uses (`lockCustomerCaseEntity`), then, under the lock, every appointment of the case, booked and cancelled. Empty is a `NotFoundException` (404).
  2. **The time** after waiting for the lock: `clock.instant()`.
  3. **The domain decides**: `cancelAppointment(appointmentId, creditRefunded, now)`.
  4. **Write** with `SaveAppointmentCancellationPort.save(cancellation)`: the managed `AppointmentEntity` gets `CANCELLED` and `creditRefunded`.
  5. **Answer** the case with `LoadCustomerConsultationCasePort.load(id, customerId, now)`.
- **No expert lock.** A cancellation only frees time: a booking with the expert that runs meanwhile and doesn't see the freed time yet is still valid. Only the case is locked ([backend.md](../../architecture/backend.md#concurrency)'s lock table), so two cancellations of one appointment, or a cancellation and a booking on one case, run one after the other.
- **Busy time, bookable slots and the appointment lists** read only `BOOKED` appointments since increment 04, so a cancelled appointment frees its time and leaves the customer's views without a change there. A REST test checks both.

### Refund eligibility

02, following UC-08's rule, [D-12](../../decisions.md#consultation) and spec decision 5. Pure domain, and the pattern for the enrollment's refund (UC-09).

- **`RefundEligibility.at(List<Appointment> bookedAppointments, int forfeitedCredits, CancellationDeadline deadline, Instant asOf)`**: refund-eligible when none of the reasons applies; otherwise the first, in this order:
  1. `SESSION_TOOK_PLACE`: a booked appointment is `CONSUMED` at `asOf`, also one that's running;
  2. `CREDIT_FORFEITED`: `forfeitedCredits > 0`;
  3. `DEADLINE_PASSED`: a booked appointment that hasn't started is no longer before its free-cancellation time; the result names the soonest such appointment and that time.
- **The record** holds the reason (empty when refund-eligible) and, only with `DEADLINE_PASSED`, the appointment and its free-cancellation time; the constructor checks that they come together. `isRefundEligible()` is "no reason".
- **Refunded credits don't count**: an appointment cancelled in time isn't in the booked ones and isn't forfeited.
- **It uses only what `Appointment` and `CancellationDeadline` have today** (`statusAt`, `start`, `freeUntil`), so 02 runs in parallel with 01. 03 may switch it to 01's `Appointment` methods; the rule stays the same.
- **One rule decides the preview and the cancellation**: the case detail's `refundIfCancelledNow` and `CaseForCancellation.cancel` both call `RefundEligibility.at` (03).

### Cancelling a case

03, following UC-08 and spec decisions 4, 5, 6, 7 and 13.

- **`CaseForCancellation`** gains the case's `CaseStatus`. Its new rules:

  | Method | Throws, in this order | `detail` |
  |---|---|---|
  | `refundEligibility(Instant now)` | – | `RefundEligibility.at(booked, forfeitedCredits(), deadline, now)` |
  | `cancel(boolean refundEligibleConfirmed, Instant now)` | `CASE_CANCELLED`: the case is `CANCELLED` | This case was cancelled. |
  | | `CASE_COMPLETED`: it's completed at `now` | This case is completed. |
  | | `REFUND_ELIGIBILITY_CHANGED`: whether it's refund-eligible at `now` isn't what was confirmed | Whether the purchase price would be refunded has changed. Please confirm again. |

  `cancel` returns the `CaseCancellation`: the case's id, `cancelledAt = now`, `refundEligible`, and an `AppointmentCancellation` for every booked appointment that is cancellable at `now`, each with its credit refunded as `isCreditRefundedIfCancelledAt(deadline, now)` decides (spec decision 7). A running appointment isn't cancelled: it's consumed.
- **`CancelConsultationCaseUseCase.invoke(CurrentUser, CancelConsultationCaseCommand)`**, the command holding the case's id and `refundEligible`: lock the case with `LockCaseForCancellationPort` (404), take the time, `cancel(refundEligible, now)`, write with `SaveCaseCancellationPort.save(cancellation)` (the managed case entity gets `CANCELLED`, `cancelledAt` and `refundEligible`; each appointment entity `CANCELLED` and `creditRefunded`), and answer the case, as the appointment's cancellation does.
- **A cancelled case can't be booked**: `CaseForBooking` gains the `CaseStatus`, and `requireActive()` throws `CASE_CANCELLED` ("This case was cancelled.", one constant, on `CaseForCancellation`, which `CaseForBooking` uses). `book` checks it first, before the credits; the booking use case calls it first under the case lock, and `ListBookableSlotsUseCase` before `expertFor`. The eligible experts route doesn't check it: it shows the offering's experts and books nothing.
- **Under the case lock** a cancellation sees every booking that came before it, and a booking that comes after sees `CANCELLED` ([D-27](../../decisions.md#architecture), spec decision 10). Task 05 proves it.

### The case as its customer sees it

01 for the upcoming appointments' preview and the credits, 03 for the cancelled case and the case's preview, 04 for the list.

- **`CustomerConsultationCase.upcomingAppointments()`** (01) returns `CustomerUpcomingAppointment`s: the `CustomerAppointment` (from `Appointment.sortedUpcoming`, which selects and sorts as `Appointment.upcoming` does but keeps the `Appointment`s; `CustomerUpcomingAppointment.of(appointment, deadline, asOf)` builds each), `canCancel` (`isCancellableAt(asOf)`; a cancelled case has no such appointment, since its cancellation cancelled every one that hadn't started), `freeCancellationUntil` and `creditRefundedIfCancelledNow` from `Appointment`'s rules with the case's deadline. `pastAppointments()` doesn't change.
- **`CustomerConsultationCase`** (03) gains `Optional<CancellationOutcome> cancellation`, empty for an active case; the adapter maps it from the case's row. `canBook()`: active and an open credit. `canCancel()`: active and not completed. `refundIfCancelledNow()`: `Optional<RefundEligibility>`, present exactly when `canCancel()`, from `RefundEligibility.at` with the case's booked appointments, forfeited credits and deadline at `asOf`.
- **`CustomerConsultationCaseSummary`** (04) gains `Optional<Instant> cancelledAt`. `group()`: `PAST` for a cancelled case, whatever its appointments; otherwise as before. `MY_PURCHASES_ORDER`'s Past key: `cancelledAt` for a cancelled case, otherwise as [above](#credits-and-completion), newest first (spec decision 8).
- **The adapter** (01) counts each case's forfeited credits in SQL: one count (`countForfeitedCredits`) for the detail and the booking; one grouped query for the list, scoped to the customer through the case (`ForfeitedCreditsRow(UUID consultationCaseId, long count)` in `row`). The lists of appointments stay `BOOKED`-only. The cancellation's case (`LockCaseForCancellationPort`) reads all of the case's appointments, booked and cancelled, and `CaseForCancellation.forfeitedCredits()` counts them from its cancelled ones.

### Ownership

- **Scoped queries**, as in increments 03 and 04: `LockCaseForCancellationPort` locks only a case of the customer (`lockCustomerCaseEntity`). Another customer's case is empty, like an unknown id: 404, and nothing is locked or changed ([security.md](../../architecture/security.md#ownership-in-use-cases)).
- **The appointment under its case**: `cancelAppointment` looks the id up among the locked case's own appointments, so an appointment of another case, the customer's own or anyone's, answers 404 under this case's path.

## Frontend

```
frontend/src/features/consultation/
├─ queries.ts                useCancelConsultationAppointment (01), useCancelConsultationCase (03)
├─ cancellation-failure.ts   the error tables' kinds, for both dialogs (01, 03)
├─ refund-texts.ts           the refund outcome's texts (03)
├─ CancelAppointmentDialog   (01)
├─ CancelCaseDialog          (03)
├─ ConsultationCasePage      Cancel on each upcoming appointment (01); Cancel case and the cancelled case (03)
└─ ConsultationCaseCard      "Cancelled" (04)
```

No new route, page or shadcn component: the dialogs use `alert-dialog`, which `ConfirmDialog` already uses.

### The cancellation dialogs

01 builds `CancelAppointmentDialog`; 03 builds `CancelCaseDialog` the same way. Neither is a `ConfirmDialog`: they reload their preview, stay open while the request runs and after a 409, and confirm again.

- **Rendered once by the page, outside the parts a reload changes.** The page keeps which dialog is open, and for the appointment, the appointment it was opened for (its id, start and end). So a reload that removes the appointment, its **Cancel** or **Cancel case** doesn't close the dialog, as the booking dialog isn't a child of what its reload removes. `alert-dialog`, controlled; the action button doesn't close it by itself.
- **The preview reloads when the dialog opens** (spec decision 12): it refetches the case, and until the answer is back it says "Checking the cancellation…", and the action button is disabled. Then it shows the outcome from the reloaded case: for the appointment, from the upcoming appointment with the same id; for the case, from `refundIfCancelledNow`. If the appointment is no longer cancellable, or the case has no `canCancel`, it says "This appointment can no longer be cancelled." or "This case can no longer be cancelled." with only **Close**.
- **The request names the outcome the dialog shows** (`creditRefunded` or `refundEligible`, spec decision 4). While it runs, the action button is disabled.
- **After the cancellation** the dialog closes; the case detail shows the new credits, or the cancelled case. From sending the request until it closes, the dialog shows the outcome it sent, not the reloaded case: the reload that comes with the answer no longer has the appointment, or the case's `canCancel`, and the closing dialog would otherwise say it can no longer be cancelled.

| | Appointment (01) | Case (03) |
|---|---|---|
| Title | "Cancel the appointment with Expert 2 on 17 Nov 2026, 10:00–11:00 (Zurich time)?" | "Cancel this case?" |
| In time / refund-eligible | "The session credit will be returned to your case." | "The purchase price of CHF 450.00 would be refunded." |
| Late / not refund-eligible | "The free cancellation deadline passed on 16 Nov 2026, 10:00. If you cancel, the session credit is **not** refunded. Cancel anyway?" | "The purchase price would **not** be refunded because <reason> Cancel anyway?", with the reasons of spec decision 5 |
| Listed | – | with future appointments, "These appointments are cancelled with the case:" and each "17 Nov 2026, 10:00–11:00 with Expert 2": the upcoming appointments with `canCancel`, which are exactly the ones the case's cancellation cancels |
| Buttons | **Keep appointment**; **Cancel appointment**, destructive | **Keep case**; **Cancel case**, destructive |

- **Errors** show in the dialog (`role="alert"`), as the booking's do. `cancellationFailure(error)` in `cancellation-failure.ts` turns an error into its kind:

  | Answer | Kind | Message | Then |
  |---|---|---|---|
  | 409 `DEADLINE_PASSED`, `REFUND_ELIGIBILITY_CHANGED` | `outcomeChanged` | the `detail` | the case reloads, the dialog shows the new outcome, and the action button asks again |
  | 409 `APPOINTMENT_CANCELLED`, `APPOINTMENT_STARTED`, `CASE_CANCELLED`, `CASE_COMPLETED` | `notCancellable` | the `detail` | only **Close** is left; the case reloads |
  | 404 | `notFound` | "This appointment was not found." or "This case was not found." | only **Close** is left |
  | anything else | `failed` | "Cancelling failed. Please try again." | stays as it is |

- **The booking dialog** (03) shows a 409 `CASE_CANCELLED`, of the times or of the booking, with its `detail` and only **Close**, as `NO_OPEN_CREDITS`: `booking-failure.ts` gains the kind `caseCancelled`.

### Case detail

`ConsultationCasePage`:

| Part | Content | Task |
|---|---|---|
| Upcoming appointments | each with `canCancel` has **Cancel** (outline), which opens `CancelAppointmentDialog`; no free-cancellation time (spec decision 1); a running one says "Now", without **Cancel**, as before | 01 |
| "Book your first session" | only while the case has no appointment and `canBook`: not next to a disabled **Book appointment** after late cancellations forfeited every credit ([UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail)) | 01 |
| Past appointments | as before: only booked ones, "Consumed" (spec decision 3) | – |
| **Cancel case** | next to **Book appointment**, outline, while `canCancel`; opens `CancelCaseDialog` | 03 |
| A cancelled case | the **Credits** term becomes **Status**: "Cancelled on 15 Nov 2026, 12:00 (Zurich time)" and "The purchase price of CHF 450.00 would be refunded." or "The purchase price would not be refunded."; no **Book appointment**, no **Cancel case**, no "Book your first session" (spec decision 6) | 03 |

### My purchases

04. `ConsultationCaseCard` of a case with `cancelledAt`: "Cancelled" in place of the next appointment and the credits (spec decision 6). The page doesn't change: the API puts the case under `PAST`, in its order.

### Queries

- **`useCancelConsultationAppointment(caseId)`** (01) and **`useCancelConsultationCase(caseId)`** (03) post to their routes and invalidate `['customer', 'consultation-cases']`, after the cancellation and after a 409, as `useBookConsultationAppointment` does: the list, every case, its experts and its slots. So the freed time is offered again in the booking dialog, and on a 409 the case reloads before the dialog shows what's current.
- The dialogs refetch the case on opening with the case query's `refetch()`: the page passes its case and that `refetch` to the dialog, which has no query of its own, so an opening sends one request (a second observer would refetch on mounting, too).

### Texts

`refund-texts.ts` (03) holds the case's refund texts, from the case's `refundIfCancelledNow` or `cancellation` and its price paid: the dialog's outcome with its reason, and the cancelled case's line. "**not**" is bold in the dialogs. The reason's dates use `formatDateAndTimes` and `formatDateTime`, in Zurich time. The appointment's texts stay in its dialog: there are two.

## Tests

| What | How |
|---|---|
| Times | The `TestClock` of increment 04. A REST test sets the clock to Mon 9 Nov 2026, 15:00 (Zurich), saves the expert's availability, buys and books through the API, then moves the clock, for example to Sun 15 Nov, 12:00, and cancels. A consumed appointment: book, then move the clock past its start. |
| Role matrix | A row for each cancellation route, `CUSTOMER`, on an unknown case with a valid body (404) (01, 03). |
| Ownership | Customer 2 cancels Customer 1's appointment and Customer 1's case: 404, and both stay as they were (01, 03). Customer 1's appointment of another of their cases, under this case's path: 404 (01). |
| REST | Per route, the happy path (01, 03). Besides: a cancelled appointment's time is offered again, it's no longer listed, and the credits count it (01); each 409 of the routes, the late cancellation and its forfeited credit (01, 03); a cancelled case's slots and booking answer `CASE_CANCELLED` (03); the list's credits after a late cancellation (01) and a cancelled case under Past (04). |
| Concurrency | One test (05): a case cancelled while a booking on it runs, deterministic as `ConsultationCaseConcurrencyTest`, failing without either case lock (spec decision 10). |
| Domain | `Appointment`'s cancellation rules: just before, at and after the deadline, a deadline of 0 hours, both days of the summer-time changes, a running appointment (01). `openCredits` with forfeited credits, and the Past key without an appointment (01). `CaseForCancellation.cancelAppointment`: each outcome and each code, in order (01). `RefundEligibility`: each reason alone, together and in order, the soonest appointment, at the deadline, a refunded credit, a running appointment, no appointments (02). `CaseForCancellation.cancel`: each code in order, the appointments it cancels with their credits, a running one left as it is (03). `CaseForBooking`: `CASE_CANCELLED` before the credits (03). `CustomerConsultationCase`: `canCancel`, `canBook`, completed at an appointment's end, `refundIfCancelledNow` present exactly with `canCancel` (03). `CustomerConsultationCaseSummary`: a cancelled case is Past, the Past order with both keys and the fallback (04). |
| Frontend | Vitest for logic only ([D-29](../../decisions.md#architecture)): `cancellationFailure` (01, 03), `bookingFailure`'s new kind (03), the refund texts (03). The dialogs and pages are checked by hand with Playwright. |

## Commands

No new commands.
