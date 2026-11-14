# 01 My cases

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

An expert lands on **My cases** after login and sees the cases they're assigned to, grouped into Upcoming, Needs booking and Past, each with the customer, the offering, the next appointment and the credits. This task builds the first route that is scoped to the assigned expert.

## What to build

Read first: [UC-11](../../use-cases/uc-11-view-my-consultation-cases.md) steps 1 to 3, [UC-01](../../use-cases/uc-01-login.md)'s landing pages, [D-17](../../decisions.md#views-and-calls), [D-39](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), the spec's decisions 1, 2, 3 and 18, its requirement [My cases](../../specs/06-expert-side-of-consultations.md#requirement-my-cases), and the plan's [packages and names](README.md#packages-and-names), [API](README.md#api), [the case as its expert sees it](README.md#the-case-as-its-expert-sees-it), [header and landing page](README.md#header-and-landing-page), [My cases](README.md#my-cases) and [tests](README.md#tests) sections. The customer's list is the pattern for every layer: `ListCustomerConsultationCasesUseCase`, `CustomerConsultationCaseSummary`, `CustomerConsultationCaseAdapter.loadAll`, `MyPurchasesPage` and `ConsultationCaseCard` ([task 04 of increment 04](../04-book-consultation-appointment/04-my-purchases-groups.md)).

Work in this order, test first:

1. **A failing REST test:** with the clock at Mon 9 Nov 2026, 15:00 (Zurich), Expert 2 is available on 17 Nov, and Customer 1 buys Career Coaching and books Expert 2 on 17 Nov, 10:00–11:00. `GET /api/expert/consultation-cases` as Expert 2 answers that one case: "Customer 1", "Career Coaching", 2 of 3 credits open, the appointment as `nextAppointment`, `UPCOMING`. As Expert 1 it answers an empty list.
2. **Domain:** `ExpertAppointment` and `ExpertConsultationCaseSummary` in `domain.consultation.view`, on top of `CaseInList`. No new rule: the groups and the order are `CaseInList`'s.
3. **Adapter:** `ExpertConsultationCaseAdapter` implements `LoadExpertConsultationCasesPort`; `ExpertConsultationCaseSummaryRow`; two queries in `ConsultationCaseRepository`, the cases of an expert with the customer's name and the offering's name, and every appointment of those cases, booked and cancelled.
4. **Use case and API:** `ListExpertConsultationCasesUseCase`, which sorts with `CaseInList.orderAt(now)`; `ExpertConsultationCaseController` with `@PreAuthorize("hasRole('EXPERT')")` and the list route; `ExpertConsultationCaseSummaryResponse` and `ExpertAppointmentResponse`; a role matrix row. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:**
   - `expert-queries.ts` with `useExpertConsultationCases`;
   - `CaseGroups`, taken out of `MyPurchasesPage`, which then uses it;
   - `MyCasesPage` with `ExpertConsultationCaseCard` and the empty text, at `/expert/cases` under `RequireRole role="EXPERT"`;
   - **My cases** in the header, before **Availability**;
   - `landingPath`: a user with `EXPERT` lands on `/expert/cases`, test first in `landing.test.ts`.
6. **Docs:** [frontend.md](../../architecture/frontend.md#navigation-and-auth) gets the header's order and the landing pages as they are now. Set the spec to **In progress**.
7. **Check by hand** on the dev server with Playwright: log in as Expert 2, Expert 1, Admin 1 and Customer 1 and see where each lands; the empty text; then a case in each group, the cards, and the link to `/expert/cases/:id` (its page comes with 02). **My purchases** looks as before.

## Acceptance criteria

- [ ] Expert 1 and Expert 2 land on **My cases** after login; Admin 1 still lands on **Consultations** and Customer 1 on **My purchases**; a login started from **Buy** still returns there (Vitest and Playwright).
- [ ] The header shows **My cases** before **Availability** for a user with `EXPERT`, and not for one without.
- [ ] Expert 2's list has Customer 1's case and not Customer 2's case with Expert 1; a case without an expert is in neither expert's list (REST).
- [ ] A case with an upcoming appointment is `UPCOMING`, also while the appointment runs; an active case with open credits and nothing upcoming is `NEEDS_BOOKING`; a completed and a cancelled case are `PAST`; the answer is in the page's order (REST).
- [ ] A card shows the customer's name, the offering's name, the next appointment in Zurich time and "2 of 3 credits open"; a cancelled case's card says "Cancelled" instead (Playwright).
- [ ] An expert without cases reads "No cases yet. A case appears here once a customer books you."
- [ ] A case stays on its expert's list after an admin removed the expert from the offering's eligible experts (REST, on a test offering, not the seeded one).
- [ ] Expert 1, who bought and booked themself, has the case on **My cases**, with "Expert 1" as the customer, and on **My purchases** (REST).
- [ ] `/expert/cases` shows "Page not found" to a user without `EXPERT`; the route answers 401 without a login and 403 without the role (role matrix).
- [ ] `openapi.json` and `schema.d.ts` have the route; every check passes.

## Tests

- Domain: `ExpertConsultationCaseSummary` (the next appointment and the group at a time, a running appointment)
- REST: the route's happy path, the groups and their order, only assigned cases, the expert removed from the eligible ones, the expert who booked themself
- Authorization: the role matrix row; each expert's list has only their own cases
- Frontend: Vitest for `landingPath`

## Out of scope

- The case detail's page and route: [02](02-expert-case-detail.md). Until then a card's link opens "Page not found".
- **Profile** in the header: [05](05-expert-profile.md).
- **My trainings** (UC-12) and the admin's **Cases** (UC-18): later increments.

## Notes
