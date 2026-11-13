# 02 Refund eligibility

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | – |
| Status | Done |

## Goal

UC-08's refund rule exists as one domain class, with its first reason, and unit tests for every case: the rule that the case's dialog previews and its cancellation stores ([03](03-cancel-case.md)), and that the enrollment's cancellation (UC-09) will mirror.

## What to build

Read first: the business rules of [UC-08](../../use-cases/uc-08-cancel-consultation-case.md#business-rules), [D-11](../../decisions.md#consultation) and [D-12](../../decisions.md#consultation), the spec's decisions 5 and 13, its scenarios [Not refund-eligible](../../specs/05-cancel-consultation-appointment-or-case.md#scenario-not-refund-eligible), [Only the first reason](../../specs/05-cancel-consultation-appointment-or-case.md#scenario-only-the-first-reason) and [A refunded credit doesn't count against the refund](../../specs/05-cancel-consultation-appointment-or-case.md#scenario-a-refunded-credit-doesnt-count-against-the-refund), and the plan's [refund eligibility](README.md#refund-eligibility) section. `SlotFinder` and its tests (increment 04, task 02) are the pattern for a pure domain rule.

Pure domain, test first, one condition at a time:

1. **`NonRefundableReason`** in `domain.consultation.value`: `SESSION_TOOK_PLACE`, `CREDIT_FORFEITED`, `DEADLINE_PASSED`, in UC-08's order.
2. **`RefundEligibility`** in `domain.consultation`: `at(List<Appointment> bookedAppointments, int forfeitedCredits, CancellationDeadline deadline, Instant asOf)`, the reason (empty when refund-eligible) and, with `DEADLINE_PASSED` only, the soonest such appointment and its free-cancellation time; `isRefundEligible()`. The constructor refuses an appointment without `DEADLINE_PASSED`, and `DEADLINE_PASSED` without one.

It uses only what exists today: `Appointment.statusAt` and `start`, and `CancellationDeadline.freeUntil`. It runs in parallel with [01](01-cancel-appointment.md), which adds cancellation rules to `Appointment` but changes none of these; don't touch `Appointment`.

## Acceptance criteria

- [x] No appointments, and no forfeited credit: refund-eligible.
- [x] Each reason alone: a consumed appointment, also one that's running; a forfeited credit; a booked appointment that hasn't started and whose free-cancellation time isn't after `asOf`.
- [x] Reasons together give the first in UC-08's order.
- [x] `DEADLINE_PASSED` names the soonest appointment whose deadline passed, not the soonest appointment, and its free-cancellation time.
- [x] At the deadline exactly, the appointment counts as passed; a second before, it doesn't.
- [x] A refunded credit doesn't count: no booked appointment and no forfeited credit is refund-eligible, whatever was cancelled in time.
- [x] Every check passes.

## Tests

- Domain: `RefundEligibility` as listed above, with fixed instants in Zurich time, including an appointment on a day of a summer-time change.

## Out of scope

- Using the rule: the case detail's `refundIfCancelledNow` and the case's cancellation, [03](03-cancel-case.md).
- The texts of the reasons: [03](03-cancel-case.md), in the frontend.

## Notes

- Followed the plan without deviation. The record is `RefundEligibility(Optional<NonRefundableReason> reason, Optional<Appointment> deadlinePassedAppointment, Optional<Instant> freeCancellationUntil)`; the constructor requires the two optionals present exactly when the reason is `DEADLINE_PASSED`.
- The caller passes only booked appointments; the rule doesn't filter cancelled ones. "Late" is `!freeUntil(start).isAfter(asOf)`; an appointment starting at `asOf` is consumed, so it never reaches the deadline check.
- `RefundEligibilityTest` covers: no appointments, eligible with free appointments, consumed, running, starting now, forfeited credit (one and several), each reason alone, the order of the reasons together, the soonest late appointment (not the soonest appointment, any list order), exactly at the deadline and a second before, no deadline (0 hours), the summer-time change, a refunded credit not counting, and the constructor's check in both directions.
