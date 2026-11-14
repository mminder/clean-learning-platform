# 02 The expert's case detail

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [01](01-my-cases.md) |
| Status | Todo |

## Goal

An expert opens one of their cases and sees the customer's name and email, the terms without the price, the credits, and every appointment: upcoming, past and cancelled. Another expert's case is "Case not found". This task sets the pattern for one resource shaped per audience: review it with extra care.

## What to build

Read first: [UC-11](../../use-cases/uc-11-view-my-consultation-cases.md) step 4, [D-12](../../decisions.md#consultation), [D-18](../../decisions.md#views-and-calls), [D-39](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), the spec's decisions 2, 4 and 14, its requirements [The expert's case detail](../../specs/06-expert-side-of-consultations.md#requirement-the-experts-case-detail) and [Only experts, and only the assigned expert](../../specs/06-expert-side-of-consultations.md#requirement-only-experts-and-only-the-assigned-expert), and the plan's [API](README.md#api), [the case as its expert sees it](README.md#the-case-as-its-expert-sees-it), [ownership](README.md#ownership), [the expert's case detail](README.md#the-experts-case-detail) and [tests](README.md#tests) sections. The customer's detail is the pattern: `GetCustomerConsultationCaseUseCase`, `CustomerConsultationCase`, `CustomerConsultationCaseAdapter.load` and `ConsultationCasePage`.

Work in this order, test first:

1. **A failing REST test:** Customer 1's case of 3 sessions with Expert 2 has a consumed appointment, one cancelled late and an upcoming one on 17 Nov 2026 (book three, move the clock past the first, cancel the second inside its deadline). `GET /api/expert/consultation-cases/{id}` as Expert 2 answers 200 with Customer 1's name and email, the terms, 0 of 3 credits open, and each appointment in its list, the cancelled one with `creditRefunded` false.
2. **Domain:** `ExpertConsultationCase` in `domain.consultation.view`: the three lists at `asOf`, on top of `CaseCredits`. It holds the session duration and the cancellation deadline, not `PurchaseTerms`.
3. **Adapter:** `ExpertConsultationCaseAdapter` implements `LoadExpertConsultationCasePort`; `ExpertConsultationCaseRow`; one query in `ConsultationCaseRepository`, scoped to the assigned expert, joined with the offering and the customer's user row.
4. **Use case and API:** `GetExpertConsultationCaseUseCase`; the route with `ExpertConsultationCaseResponse` and `ExpertCancelledAppointmentResponse`, without `notes` and `teamsLink`, which come with 03 and 07; a role matrix row. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** `useExpertConsultationCase`; `ExpertConsultationCasePage` at `/expert/cases/:id`, as the plan's table says, without notes and **Join**.
6. **Check by hand** on the dev server with Playwright: a case with an upcoming appointment, one cancelled in time and one cancelled late (a 72-hour deadline makes a booking late from the start, as spec 05's demo does); a cancelled case; then Expert 1 on the URL of Expert 2's case.

## Acceptance criteria

- [ ] The detail shows the customer's name and email, the offering, "3 sessions", the session duration, the cancellation deadline, the purchase time and the credits (REST and Playwright).
- [ ] The response has no price paid and no refund outcome: the body of an active and of a cancelled case contains neither `pricePaid` nor `refundEligible` (REST).
- [ ] An upcoming appointment is listed soonest first, and a running one as `CONSUMED` among the upcoming ones, with "Now"; a past one is "Consumed"; a cancelled one says "Cancelled, credit refunded" or "Cancelled, credit forfeited", newest first (REST, domain and Playwright).
- [ ] A cancelled case says "Cancelled on <date, time> (Zurich time)" in place of the credits, without a refund outcome, and lists the appointments cancelled with it among the cancelled ones (REST and Playwright).
- [ ] Expert 1 on Customer 1's case, which has Expert 2, gets 404, like an unknown id; the page says "Case not found" (REST and Playwright).
- [ ] The customer's detail of the same case still shows no cancelled appointment (the existing tests pass unchanged).
- [ ] `/expert/cases/:id` shows "Page not found" to a user without `EXPERT`; the role matrix has the route.
- [ ] `openapi.json` and `schema.d.ts` have the route; every check passes.

## Tests

- Domain: `ExpertConsultationCase` (the three lists at a time, at an appointment's start and end, the cancelled ones' order)
- REST: the route's happy path; a case with a consumed, a refunded and a forfeited appointment; a cancelled case; no price and no refund outcome in the body; unknown and malformed ids
- Authorization: the role matrix row; ownership with both seeded experts
- Frontend: none beyond the checks; the lists and the statuses are the API's

## Out of scope

- Notes: [03](03-notes.md). **Join**: [07](07-join-call.md).
- The expert booking, cancelling or rescheduling an appointment: UC-11's out of scope.
- The admin's case detail: UC-18.

## Notes
