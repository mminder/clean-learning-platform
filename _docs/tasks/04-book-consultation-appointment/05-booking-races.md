# 05 Booking races

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [03](03-book-appointment.md) |
| Status | Todo |

## Goal

Two tests prove the two locks of a booking: two customers can't book one expert's slot at once, and two bookings can't take a case's last credit at once. Each fails when its lock is gone.

## What to build

Read first: [D-27](../../decisions.md#architecture), [backend.md](../../architecture/backend.md#concurrency), the spec's [concurrent bookings](../../specs/04-book-consultation-appointment.md#requirement-concurrent-bookings) scenarios, and the plan's [booking](README.md#booking) and [tests](README.md#tests) sections. `ConsultationOfferingConcurrencyTest` is the pattern, and task 04 of [increment 03](../03-buy-consultation-package/04-purchase-guard.md#notes) explains its second lock.

`ConsultationCaseConcurrencyTest`, with the clock set and the experts' availability saved through the API:

1. **Two customers, one slot.** Customer 1 and Customer 2 each buy Career Coaching, so both cases have no expert yet, and both book Expert 2 at the same start at once. The test holds Expert 2's `app_user` row (`for no key update`), the row the expert's lock takes, until both requests wait. Without the expert's lock they would wait nowhere, so the test also holds Expert 2's `expert_profile` row (`for update`): each first booking sets its case's expert, and that foreign key check waits for it. The test lets go of the `app_user` row first, then waits until each request either waits for the `expert_profile` row or for the other request, and only then lets go of the `expert_profile` row, as the purchase's test does with the offering's rows. Exactly one answers 201, the other 409 `SLOT_NOT_AVAILABLE`, and Expert 2 has one appointment at that start.
2. **Two tabs, one credit.** The test makes a published offering with a 1-session package and both experts eligible (a test script, like `more-published-offerings.sql`), and Customer 1 buys it: a case with one credit and no expert. Two bookings of that case at once, one naming Expert 1 and the other Expert 2, so that each locks a different expert and only the case's lock can order them. The test holds the case's row (`for no key update`) until both wait: with the lock they wait at it; without it, each waits at its update of the case's expert. Exactly one answers 201, the other 409 `NO_OPEN_CREDITS`, and the case has one appointment.
3. **Check each test fails without its lock**: once with `LockExpertPort`'s `find` made plain, once with `LockCaseForBookingPort`'s query without `@Lock`, three runs each, and note the results under **Notes**.

## Acceptance criteria

- [ ] Both tests pass five runs out of five and in `./mvnw verify`.
- [ ] The first fails without the expert's lock, the second without the case's lock (three runs each, noted).
- [ ] No request waits beyond the tests' timeout, with or without the locks: each test ends with a clear assertion, not a hang.
- [ ] [backend.md](../../architecture/backend.md#concurrency) says how the tests make the races certain, if it needs more than the purchase's sentence.
- [ ] Every check passes.

## Tests

- Concurrency: the two above

## Out of scope

- Two saves of availability at once: [01](01-manage-availability.md#out-of-scope) says why.
- Any change to the booking itself. If a test shows the booking is wrong, note it and fix it in this task only if the fix is in the lock order the plan describes; otherwise tell the orchestrator.

## Notes

