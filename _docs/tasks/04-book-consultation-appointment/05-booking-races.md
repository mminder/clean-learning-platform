# 05 Booking races

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [03](03-book-appointment.md) |
| Status | Done |

## Goal

Two tests prove the two locks of a booking: two customers can't book one expert's slot at once, and two bookings can't take a case's last credit at once. Each fails when its lock is gone.

## What to build

Read first: [D-27](../../decisions.md#architecture), [backend.md](../../architecture/backend.md#concurrency), the spec's [concurrent bookings](../../specs/04-book-consultation-appointment.md#requirement-concurrent-bookings) scenarios, and the plan's [booking](README.md#booking) and [tests](README.md#tests) sections. `ConsultationOfferingConcurrencyTest` is the pattern, and task 04 of [increment 03](../03-buy-consultation-package/04-purchase-guard.md#notes) explains its second lock.

`ConsultationCaseConcurrencyTest`, with the clock set and the experts' availability saved through the API:

1. **Two customers, one slot.** Customer 1 and Customer 2 each buy Career Coaching, so both cases have no expert yet, and both book Expert 2 at the same start at once. The test holds Expert 2's `app_user` row (`for no key update`), the row the expert's lock takes, until both requests wait. Without the expert's lock they would wait nowhere, so the test also holds Expert 2's `expert_profile` row (`for update`): each first booking sets its case's expert, and that foreign key check waits for it. The test lets go of the `app_user` row first, then waits until each request either waits for the `expert_profile` row or for the other request, and only then lets go of the `expert_profile` row, as the purchase's test does with the offering's rows. Exactly one answers 201, the other 409 `SLOT_NOT_AVAILABLE`, and Expert 2 has one appointment at that start.
2. **Two tabs, one credit.** The test makes a published offering with a 1-session package and both experts eligible (a test script, like `more-published-offerings.sql`), and Customer 1 buys it: a case with one credit and no expert. Two bookings of that case at once, one naming Expert 1 and the other Expert 2, so that each locks a different expert and only the case's lock can order them. The test holds the case's row (`for no key update`) until both wait: with the lock they wait at it; without it, each waits at its update of the case's expert. Exactly one answers 201, the other 409 `NO_OPEN_CREDITS`, and the case has one appointment.
3. **Check each test fails without its lock**: once with `LockExpertPort`'s `find` made plain, once with `LockCaseForBookingPort`'s query without `@Lock`, three runs each, and note the results under **Notes**.

## Acceptance criteria

- [x] Both tests pass five runs out of five and in `./mvnw verify`.
- [x] The first fails without the expert's lock, the second without the case's lock (three runs each, noted).
- [x] No request waits beyond the tests' timeout, with or without the locks: each test ends with a clear assertion, not a hang.
- [x] [backend.md](../../architecture/backend.md#concurrency) says how the tests make the races certain, if it needs more than the purchase's sentence.
- [x] Every check passes.

## Tests

- Concurrency: the two above

## Out of scope

- Two saves of availability at once: [01](01-manage-availability.md#out-of-scope) says why.
- Any change to the booking itself. If a test shows the booking is wrong, note it and fix it in this task only if the fix is in the lock order the plan describes; otherwise tell the orchestrator.

## Notes

- **`ConsultationCaseConcurrencyTest`**, in `api.consultation`, copies the helpers of `ConsultationOfferingConcurrencyTest` (the row locks from the test's own connections, the 10-second `await`, the shut-down that waits for the requests). Both tests set the clock to Mon 9 Nov 2026, 15:00 (Zurich), save the experts' availability on 17 Nov, 09:00–12:00, and buy through the API. A booking answers its status, and for a 409 its code too, so a failure reads, for example, `["201", "201"]`.
- **Two customers, one slot:** the test holds Expert 2's `app_user` row (`for no key update`) and their `expert_profile` row (`for update`) on two connections, starts both bookings of 17 Nov, 10:00, and waits until `pg_stat_activity` shows two statements on `app_user` or `consultation_case` waiting for a lock (with the lock, the `app_user` lock; without it, the case's update, whose foreign key check of the expert waits). It lets go of `app_user`, waits until two requests wait for a lock that the `expert_profile` connection holds or that neither test connection holds (`pg_blocking_pids`: one waits at the foreign key, the other for it), and only then lets go of `expert_profile`. It asserts 201 and 409 `SLOT_NOT_AVAILABLE`, and one appointment of Expert 2 at 10:00.
- **Two tabs, one credit:** `one-session-offering.sql` adds Mock Interview, published, 60 minutes, one package of 1 session for CHF 150, Expert 1 and Expert 2 eligible. Customer 1 buys it, and books it twice at once: Expert 1 at 10:00 and Expert 2 at 11:00. The test holds the case's row (`for no key update`) until both wait for a lock, then lets go. It asserts 201 and 409 `NO_OPEN_CREDITS`, and one appointment of the case.
- **Checked:** with `UserAdapter.lock`'s `find` made plain, the first test failed three runs out of three with `["201", "201"]` (about 1 s each). With `@Lock` removed from `ConsultationCaseRepository.lockCustomerCaseEntity`, the second failed three runs out of three with `["201", "201"]` (about 1 s each). No run hung. With the code restored (`git diff` showed nothing under `src/main`), both passed five runs out of five (about 1.1 s and 0.1 s), and in `./mvnw verify`. The booking needed no change.
- **Docs:** backend.md's concurrency section has a sentence on how the booking's tests make their races certain.
