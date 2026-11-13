# 04 Cancelled cases on My purchases

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | [03](03-cancel-case.md) |
| Status | Done |

## Goal

**My purchases** shows a cancelled case under **Past**, with "Cancelled" on its card, and sorts Past newest first, a cancelled case by its cancellation time.

## What to build

Read first: the landing page of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-landing-page-my-purchases), [D-17](../../decisions.md#views-and-calls), the spec's decisions 6 and 8, its requirement [Cancelled cases on My purchases](../../specs/05-cancel-consultation-appointment-or-case.md#requirement-cancelled-cases-on-my-purchases), and the plan's [API](README.md#api), [credits and completion](README.md#credits-and-completion), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it) and [My purchases](README.md#my-purchases) sections. Increment 04's [task 04](../04-book-consultation-appointment/04-my-purchases-groups.md) built the groups; follow its notes.

Work in this order, test first:

1. **A failing REST test:** Customer 1 has a case with an upcoming appointment, which they cancel, and an active case; `GET /api/customer/consultation-cases` answers the cancelled one as `PAST` with `cancelledAt`, after the active one.
2. **Domain:** `CustomerConsultationCaseSummary` gains `Optional<Instant> cancelledAt`; `group()` is `PAST` for a cancelled case; `MY_PURCHASES_ORDER`'s Past key is `cancelledAt` for a cancelled case, and otherwise the last appointment's start or, without one, the purchase time ([01](01-cancel-appointment.md)).
3. **Adapter and API:** the list's query reads the case's `cancelled_at`; `CustomerConsultationCaseSummaryResponse` gains `cancelledAt`. Regenerate `openapi.json`, then `npm run generate:api`.
4. **Frontend:** `ConsultationCaseCard` says "Cancelled" in place of the next appointment and the credits.

## Acceptance criteria

- [x] A cancelled case is `PAST`, also one that had an upcoming or a running appointment, or open credits (domain).
- [x] Past is sorted newest first: a completed case by its last appointment's start, one without an appointment by its purchase time, a cancelled case by its cancellation time, mixed in one order (domain).
- [x] The REST test's cancelled case comes under Past with `cancelledAt`; Customer 2's list shows none of Customer 1's cases.
- [x] On the dev server, a cancelled case's card under **Past** says "Cancelled", without the next appointment and the credits (Playwright).
- [x] `openapi.json` and `schema.d.ts` have the new field; every check passes.

## Tests

- Domain: `CustomerConsultationCaseSummary` (the group of a cancelled case, the Past order with all three keys)
- REST: the list with a cancelled case
- Frontend: none beyond the checks; the group and the order are the API's

## Out of scope

- Cancelled enrollments under Past: with the trainings (UC-09).

## Notes

- **The domain:** `CustomerConsultationCaseSummary` has `Optional<Instant> cancelledAt` (before `asOf`). `group()` answers `PAST` first for a cancelled case, whatever appointments or credits it has; `pastOrderKey()` is `cancelledAt`, otherwise the newest past appointment's start, otherwise the purchase time (01). The domain tests give a cancelled case upcoming, running and no appointments, and sort all three keys in one list, with a cancelled case bought long before and one whose appointment lies in the future. A cancelled case with a running appointment sorts under Past by its cancellation time (added in the review).
- **Persistence:** no migration (V10 has `cancelled_at`). `findCustomerCases` selects `c.cancelledAt` into `CustomerConsultationCaseSummaryRow` (a nullable field); still one query scoped to the customer, with the appointments' and forfeited credits' queries unchanged. Task 03 cancelled a cancelled case's appointments that hadn't started, so its `nextAppointment` is `null`, except while an appointment that was running at the cancellation still runs: that one stays `BOOKED` and is its `nextAppointment`, `CONSUMED`. Its `nextAppointment`, `openCredits` and `totalCredits` are sent, but the card doesn't show them, and the Past order doesn't read the next appointment.
- **The API:** `CustomerConsultationCaseSummaryResponse.cancelledAt` is `@Nullable`; `openapi.json` and `schema.d.ts` regenerated (one field).
- **The card:** `ConsultationCaseCard` shows "Cancelled" (medium weight, as the credits line) in place of the next appointment and the credits, and keeps the expert and "Bought <date>". The cancellation time isn't on the card: the case detail has it, and the order uses it.
- **Tests:** `CustomerConsultationCaseSummaryTest` (a cancelled case is Past with an upcoming, a running or no appointment, and with open credits; the Past order with a completed case, a case without an appointment and cancelled cases; a cancelled case after the active groups). REST: `ConsultationCaseCancellationTest.aCancelledCaseIsPastOnMyPurchasesAfterTheActiveOne` (Customer 1's cancelled case with an upcoming appointment comes `PAST` with `cancelledAt`, after the active one; Customer 2's list has neither); `CustomerConsultationCaseControllerTest`'s strict list expectations have `"cancelledAt": null`. No frontend test, as the task says.
- **Playwright** (by hand, dev server on the real clock, Fri 13 Nov 2026): Customer 1 bought Career Coaching twice through the API (two rounds, four cases), opened one case of each round, **Cancel case** and confirmed in the dialog ("Cancelled on …" appeared). **My purchases** shows the two active cases under Needs booking with "3 of 3 credits open", and **Past (2)**, opened, with two cards that say the expert line, "Cancelled" and "Bought 13 Nov 2026", without the credits or a next appointment. A cancelled case that had an upcoming appointment is covered by the REST test, not by hand.
