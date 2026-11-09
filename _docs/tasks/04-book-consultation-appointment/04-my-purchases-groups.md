# 04 My purchases with Upcoming and Past

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [03](03-book-appointment.md) |
| Status | Done |

## Goal

**My purchases** groups the customer's cases into **Upcoming**, **Needs booking** and **Past** (collapsed), each in its order, and each card shows the expert and the next appointment. A completed case moves to Past instead of vanishing.

## What to build

Read first: the landing page of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-landing-page-my-purchases), [D-17](../../decisions.md#views-and-calls), the case status in the [glossary](../../glossary.md#purchases), the spec's decisions 6, 18 and 21, its [case detail and My purchases](../../specs/04-book-consultation-appointment.md#requirement-the-case-detail-and-my-purchases) scenarios, and the plan's [API](README.md#api), [the case as its customer sees it](README.md#the-case-as-its-customer-sees-it) and [My purchases](README.md#my-purchases) sections.

Work in this order, test first:

1. **A failing REST test:** with the clock set, Customer 1 has three cases: one with an upcoming appointment, one with open credits and nothing upcoming, and one whose three appointments are all past (booked, then the clock moved). `GET /api/customer/consultation-cases` answers them as `UPCOMING`, `NEEDS_BOOKING` and `PAST`, in that order, with the expert's name and the next appointment.
2. **Domain:** `PurchaseGroup`; `CustomerConsultationCaseSummary` with the expert's name, the appointments at `asOf`, `openCredits()`, `group()`, `nextAppointment()`, and `MY_PURCHASES_ORDER`. It shares the appointments' rules with `CustomerConsultationCase` instead of copying them (plan).
3. **Port and adapter:** `LoadCustomerConsultationCasesPort.loadAll` takes `asOf`; the adapter loads the expert's display name with the cases, and the appointments of all the customer's cases with one more query, scoped to the customer.
4. **Use case and API:** `ListCustomerConsultationCasesUseCase` passes `asOf` from the `Clock` and sorts with `MY_PURCHASES_ORDER`; `CustomerConsultationCaseSummaryResponse` gains `expertDisplayName`, `nextAppointment` and `group`; regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** `MyPurchasesPage` with the three groups in the API's order, Past in a `<details>`, empty groups hidden, the empty state as before; `ConsultationCaseCard` with the expert's name or "Not assigned yet" and "Next: …" with an upcoming appointment, or "Now: …" while it runs.

## Acceptance criteria

- [x] The REST test's three cases come in the three groups, in the order Upcoming, Needs booking, Past.
- [x] Within a group: Upcoming by the next appointment, soonest first; Needs booking by purchase time; Past by the last appointment, newest first (domain tests).
- [x] A case whose credits are all booked but one appointment is still ahead is Upcoming; while that one runs, it's still Upcoming, and once it has ended, it's Past (spec decision 21).
- [x] A case with past appointments and an open credit is Needs booking.
- [x] Customer 2's list shows none of Customer 1's cases or appointments.
- [x] On the dev server, after Customer 1 books, **My purchases** shows the case under **Upcoming** with Expert 2 and "Next: …" in Zurich time, and a case without appointments under **Needs booking**; **Past** is collapsed (Playwright; Past with a REST test, as it needs the clock).
- [x] `openapi.json` and `schema.d.ts` have the new fields; every check passes.

## Tests

- Domain: `CustomerConsultationCaseSummary` (each group, the boundaries in time at an appointment's start and end, the order, credits)
- REST: the list's groups, order and fields; ownership with both seeded customers
- Frontend: none beyond the checks; the grouping is the API's

## Out of scope

- Cancelled cases under Past: UC-08.
- Enrollments in the groups: with the trainings (UC-04).

## Notes

- **The summary shares the rules by calling `Appointment`**, as the case detail does: `openCredits()` is `Appointment.openCredits`, `nextAppointment()` the first of `Appointment.upcoming`, `group()` is built on those two, and Past's key is the first of `Appointment.past`. Nothing was added to `Appointment`; the subtraction, the status and the upcoming test exist once. This also fixes "3 of 3 credits open" on **My purchases** after a booking (task 03's note); its TODO is gone.
- **The tests:** the list's REST tests are in `ConsultationCaseBookingTest`, which already has the helpers to save availability and book through the API: the three groups and their order, a running appointment moving to Needs booking at its end, and ownership with both customers. `CustomerConsultationCaseControllerTest`'s list tests now expect the new fields. A lenient JSON comparison ignores an array's order, so the order is asserted on the ids.
- **"(Zurich time)"** sits next to the **Upcoming** heading, since only an Upcoming card shows a time (plan updated). The implementation first had a line "Times are in Zurich time." under it; the review changed it to the hint frontend.md asks for, next to the heading, as on the case detail. The frontend splits the API's list by `group` and keeps its order.
- **A Past case always has an appointment** (its credits are all booked and none is upcoming, and a session count is at least 1), which the Past order's `getFirst()` relies on. UC-08 will have to give a cancelled case another key.

