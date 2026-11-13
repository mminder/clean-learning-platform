# 05 A case cancelled during a booking

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | [03](03-cancel-case.md) |
| Status | Todo |

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

- [ ] The test passes five runs out of five and in `./mvnw verify`.
- [ ] It fails without the cancellation's case lock, and without the booking's (three runs each, noted).
- [ ] No request waits beyond the test's timeout, with or without the locks: the test ends with a clear assertion, not a hang.
- [ ] [backend.md](../../architecture/backend.md#concurrency) says how this test makes its race certain, in a sentence next to the booking's tests.
- [ ] Every check passes; `git diff` shows nothing left under `src/main` from the checks.

## Tests

- Concurrency: the one above

## Out of scope

- Two cancellations of one appointment or one case at once: under the case's lock the second answers 409, and without it both write the same (spec decision 10).
- Any change to the cancellation or the booking. If the test shows one is wrong, note it and fix it in this task only if the fix is in the lock order the plan describes; otherwise tell the orchestrator.

## Notes
