# 05 A case cancelled during a booking

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | [03](03-cancel-case.md) |
| Status | Done |

## Goal

A test proves that a case cancelled while a booking on it runs never keeps a future booked appointment, and fails when either request's case lock is gone.

## What to build

Read first: [D-27](../../decisions.md#architecture), [backend.md](../../architecture/backend.md#concurrency), the spec's [concurrent cancellations](../../specs/05-cancel-consultation-appointment-or-case.md#requirement-concurrent-cancellations) scenario and decision 10, the plan's [cancelling a case](README.md#cancelling-a-case) and [tests](README.md#tests) sections, and increment 04's [booking races](../04-book-consultation-appointment/05-booking-races.md#notes), whose helpers this test reuses.

A new test in `ConsultationCaseConcurrencyTest`, with the clock set and Expert 2's availability saved through the API. Customer 1 buys Career Coaching and books Expert 2 once, so the case has its expert and a booking on it doesn't write the case's row (only a first booking sets the expert). Both times are well before their deadline, so the case stays refund-eligible.

1. **Hold the expert:** the test holds Expert 2's `app_user` row (`for no key update`), the row a booking locks after the case.
2. **Start the booking** of another slot with Expert 2, and wait until it waits for a lock: with its case lock, it holds the case and waits for the expert; without it, it has read the case as active and waits for the expert.
3. **Start the case's cancellation** with `{"refundEligible": true}`, and wait until it waits for a lock or has answered: with its case lock, it waits for the booking's; without it, it reads the case's appointments without the new one and waits at its update of the case; and with the booking's lock gone, it isn't held up at all and answers.
4. **Let go of the expert**, and wait for both answers.
5. **Assert** the booking answered 201 and the cancellation 200, the case is `CANCELLED`, and none of its appointments is still `BOOKED` with a start in the future: the new one was cancelled with the case, its credit refunded.
6. **Check it fails without each lock**: once with the cancellation's lock taken out of `LockCaseForCancellationPort` (the booking's stays), and once with the booking's taken out of `LockCaseForBookingPort` (the cancellation's stays). Each is a separate query or method, so one can lose its `@Lock` without the other; if both share `lockCustomerCaseEntity`, add a plain variant for the check only, and remove it again. Three runs each; note the results under **Notes**.

## Acceptance criteria

- [x] The test passes five runs out of five and in `./mvnw verify`.
- [x] It fails without the cancellation's case lock, and without the booking's (three runs each, noted).
- [x] No request waits beyond the test's timeout, with or without the locks: the test ends with a clear assertion, not a hang.
- [x] [backend.md](../../architecture/backend.md#concurrency) says how this test makes its race certain, in a sentence next to the booking's tests.
- [x] Every check passes; `git diff` shows nothing left under `src/main` from the checks.

## Tests

- Concurrency: the one above

## Out of scope

- Two cancellations of one appointment or one case at once: under the case's lock the second answers 409, and without it both write the same (spec decision 10).
- Any change to the cancellation or the booking. If the test shows one is wrong, note it and fix it in this task only if the fix is in the lock order the plan describes; otherwise tell the orchestrator.

## Notes

- **`aCaseCancelledDuringABookingKeepsNoFutureBookedAppointment`** in `ConsultationCaseConcurrencyTest`, with its helpers (the row lock from the test's own connection, the 10-second `await`, the shut-down that waits for the requests). A new helper `cancel` answers the status as `book` does (both through `answer`). The clock is Mon 9 Nov 2026, 15:00 (Zurich); Expert 2 saves 17 Nov, 09:00–12:00; Customer 1 buys Career Coaching (3 sessions) and books Expert 2 on 17 Nov, 10:00, so the case has its expert.
- **How the race is certain:** the test holds Expert 2's `app_user` row (`for no key update`), starts the booking of 17 Nov, 11:00, and waits until `pg_stat_activity` shows one statement on `app_user` or `consultation_case` waiting for a lock (the booking's lock of the expert, with or without its case lock before). It starts the cancellation with `{"refundEligible": true}` and waits until it has answered or two statements wait for a lock: with its case lock, its lock of the case, held by the booking; without it, its `update consultation_case`, which the answer's query flushes, after it read the appointments without the new one; with the booking's case lock gone, it answers at once. Only then does the test commit, and it waits for both answers, each bounded by the timeout. It asserts `["201", "200"]`, the case `CANCELLED`, and both appointments `CANCELLED` with their credits refunded, in start order.
- **The plain variant:** none was added. `ConsultationCaseRepository.findCustomerCaseEntity` is the same query without `@Lock`, already used by the slots' `LoadCaseForBookingPort.load`; for each check the adapter's method called it in place of `lockCustomerCaseEntity`, and was restored afterwards.
- **Checked:** with the code as it is, the test passed five runs out of five (about 1.0–1.3 s). With `lockForCancellation` on `findCustomerCaseEntity` (the booking's lock kept), it failed three runs out of three (about 0.9 s each); with `lock` (the booking's) on it and the cancellation's kept, three out of three (about 0.9 s each). Every failure answered `["201", "200"]` and then failed at the appointments: `["2026-11-17T09:00:00Z CANCELLED true", "2026-11-17T10:00:00Z BOOKED -"]`, a cancelled case with a future booked appointment. No run hung. With the code restored, `git diff` showed nothing under `src/main`. The cancellation and the booking needed no change.
- **Docs:** backend.md's concurrency section has a sentence on how this test makes its race certain, next to the booking's tests.
