# 03 Cancel a case

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | [01](01-cancel-appointment.md), [02](02-refund-eligibility.md) |
| Status | Todo |

## Goal

A customer cancels a whole case from its detail. The dialog says whether the price would be refunded, and why not, and lists the appointments it cancels. A cancelled case shows when it was cancelled and its refund outcome, and can't be booked or cancelled again.

## What to build

Read first: [UC-08](../../use-cases/uc-08-cancel-consultation-case.md), the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), the error flow "The case was cancelled" of [UC-06](../../use-cases/uc-06-book-consultation-appointment.md#alternative-and-error-flows), [D-12](../../decisions.md#consultation), [D-27](../../decisions.md#architecture), the spec's decisions 4 to 7, 11 to 13, its requirement [Cancel a case](../../specs/05-cancel-consultation-appointment-or-case.md#requirement-cancel-a-case), and the plan's [schema](README.md#schema), [API](README.md#api), [credits and completion](README.md#credits-and-completion), [cancelling a case](README.md#cancelling-a-case), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it), [the cancellation dialogs](README.md#the-cancellation-dialogs), [case detail](README.md#case-detail) and [texts](README.md#texts) sections. Task 01's appointment cancellation is the pattern for every layer; follow its notes.

Work in this order, test first:

1. **A failing REST test:** Customer 1 buys 3 sessions for CHF 450.00 and books Expert 2 on 17 Nov, 10:00–11:00. At 15 Nov, 12:00 the case has `canCancel` and `refundIfCancelledNow` refund-eligible; `POST …/cancel` with `{"refundEligible": true}` answers 200 with the case: `cancellation` at 15 Nov, 12:00, refund-eligible, `canBook` and `canCancel` false, no appointment listed. Expert 2's 17 Nov, 10:00 is offered again.
2. **Domain:** `CaseStatus`, `CancellationOutcome`, `CaseCancellation`; completion through one rule on `Appointment`; `CaseForCancellation` gains the status, `refundEligibility(now)` and `cancel` with its codes, using [02](02-refund-eligibility.md)'s `RefundEligibility`; `CaseForBooking` gains the status and `requireActive()`; `CustomerConsultationCase` gains `cancellation`, `canCancel()`, `refundIfCancelledNow()`, and `canBook()` checks the status.
3. **Schema and adapter:** `V10__cancel_consultation_case.sql`; `ConsultationCaseEntity` maps the status (the purchase writes `ACTIVE`), `cancelledAt` and `refundEligible`; the adapter maps them into the cases and implements `SaveCaseCancellationPort`.
4. **Use case and API:** `CancelConsultationCaseUseCase`; the route with `CancelConsultationCaseRequest`; the case detail's new fields; `CASE_CANCELLED` in the booking and in `ListBookableSlotsUseCase`, declared on both routes; a role matrix row. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** `useCancelConsultationCase`; `cancellation-failure.ts` gains the case's codes, `booking-failure.ts` the kind `caseCancelled`; `refund-texts.ts`; `CancelCaseDialog`, rendered once by the page; **Cancel case**; the cancelled case's detail.
6. **Check by hand** on the dev server with Playwright: the dialog refund-eligible with an appointment listed, and not refund-eligible with a late-cancelled credit (the spec's demo steps 5 to 7); **Keep case**; the cancelled case's detail; and booking in a second tab left open on the case.

## Acceptance criteria

- [ ] A case without appointments: the dialog says the CHF 450.00 would be refunded; after the cancellation the detail says "Cancelled on 15 Nov 2026, 12:00" and the refund, in place of the credits, without **Book appointment**, **Cancel case** or "Book your first session" (REST and Playwright).
- [ ] The future appointments are cancelled with the case, each with its credit refunded before its deadline and forfeited after; a running appointment isn't cancelled (domain and REST).
- [ ] Not refund-eligible, each with its reason as in the spec, and only the first of several: a consumed appointment, a credit cancelled late, an appointment past its deadline at 16 Nov, 11:00; the cancellation stores `refundEligible = false` (domain and REST).
- [ ] 409 `REFUND_ELIGIBILITY_CHANGED` for `{"refundEligible": true}` at 16 Nov, 10:01; 409 `CASE_CANCELLED` for a second cancellation; 409 `CASE_COMPLETED` once the last appointment of a fully booked case has ended; each with the plan's `detail`, and nothing changes (REST).
- [ ] A cancelled case's bookable slots and booking answer 409 `CASE_CANCELLED`, and the booking dialog shows the message with only **Close** (REST and Playwright).
- [ ] A cancelled case's detail lists its consumed appointments, but not the ones cancelled with it, and has no **Cancel** (REST).
- [ ] Customer 2 cancelling Customer 1's case answers 404, and nothing changes; the role matrix has the route.
- [ ] The dialog follows the plan's tables: it reloads its preview when it opens, lists the appointments it cancels with their expert, says **Keep case** and **Cancel case**, and stays open on a 409 with what's current (Playwright).
- [ ] `openapi.json` and `schema.d.ts` have the new route and fields; every check passes.

## Tests

- Domain: `CaseForCancellation.cancel` (each code in order, the appointments it cancels and their credits, a running one), `CaseForBooking` (`CASE_CANCELLED` first), `CustomerConsultationCase` (`canCancel`, `canBook`, completion at an appointment's end, `refundIfCancelledNow` exactly with `canCancel`)
- REST: the route's happy path, not refund-eligible, the three 409s, the cancelled case's detail, `CASE_CANCELLED` on the slots and the booking
- Authorization: the role matrix row; ownership with both seeded customers
- Frontend: Vitest for `cancellationFailure`'s new codes, `bookingFailure`'s new kind and the refund texts

## Out of scope

- Cancelled cases on **My purchases**: [04](04-cancelled-cases-on-my-purchases.md). Until then a cancelled case may show in the wrong group there.
- The race of a case cancelled during a booking: [05](05-cancellation-race.md).
- The admin's view of the cancellation: UC-18.

## Notes
