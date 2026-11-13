# 04 Cancelled cases on My purchases

| | |
|---|---|
| Increment | [05 Cancel a consultation appointment or case](../../specs/05-cancel-consultation-appointment-or-case.md) |
| Type | AFK |
| Blocked by | [03](03-cancel-case.md) |
| Status | Todo |

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

- [ ] A cancelled case is `PAST`, also one that had an upcoming or a running appointment, or open credits (domain).
- [ ] Past is sorted newest first: a completed case by its last appointment's start, one without an appointment by its purchase time, a cancelled case by its cancellation time, mixed in one order (domain).
- [ ] The REST test's cancelled case comes under Past with `cancelledAt`; Customer 2's list shows none of Customer 1's cases.
- [ ] On the dev server, a cancelled case's card under **Past** says "Cancelled", without the next appointment and the credits (Playwright).
- [ ] `openapi.json` and `schema.d.ts` have the new field; every check passes.

## Tests

- Domain: `CustomerConsultationCaseSummary` (the group of a cancelled case, the Past order with all three keys)
- REST: the list with a cancelled case
- Frontend: none beyond the checks; the group and the order are the API's

## Out of scope

- Cancelled enrollments under Past: with the trainings (UC-09).

## Notes
