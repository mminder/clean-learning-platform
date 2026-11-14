# 07 Join a call

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [02](02-expert-case-detail.md), [05](05-expert-profile.md) |
| Status | Todo |

## Goal

Each upcoming appointment has **Join**, which opens the assigned expert's Teams link. The expert always has it; the customer from one hour before the start until the end, and never gets the link outside that window. This is the first response whose content depends on the time.

## What to build

Read first: [UC-10](../../use-cases/uc-10-join-call.md), [D-19](../../decisions.md#views-and-calls), [security.md](../../architecture/security.md#shaped-responses), the spec's decisions 2, 10, 11 and 19, its requirement [Join a call](../../specs/06-expert-side-of-consultations.md#requirement-join-a-call), and the plan's [API](README.md#api), [the link window](README.md#the-link-window), [join](README.md#join) and [tests](README.md#tests) sections. `CustomerUpcomingAppointment`'s cancellation preview is the pattern for a value the server derives at `asOf` ([task 01 of increment 05](../05-cancel-consultation-appointment-or-case/01-cancel-appointment.md)).

Work in this order, test first:

1. **A failing REST test:** Expert 2's profile has the link `https://example.com/meet/expert-2`, and Customer 1's case has the appointment on Tue 17 Nov 2026, 10:00–11:00. With the clock at 08:59, the customer's case has `linkAvailableFrom` 09:00, `linkWindowOpen` false, and its body doesn't contain the link; at 09:00 and at 10:30 it has `teamsLink`; at 11:00 the appointment is past, and the body doesn't contain the link.
2. **Domain:** `LinkWindow` in `domain.shared`, test first: just before, at and after both ends. `CustomerConsultationCase` gains the assigned expert's Teams link; `CustomerUpcomingAppointment` derives the three link values and refuses a link with a closed window. `ExpertConsultationCase` gains the expert's own link.
3. **Adapters:** the customer's case query reads the assigned expert's `teams_link`; the expert's case query reads the expert's own.
4. **API:** `linkAvailableFrom`, `linkWindowOpen` and `teamsLink` in `CustomerUpcomingAppointmentResponse`; `teamsLink` in `ExpertConsultationCaseResponse`. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** **Join** and its texts on both case details, as the plan's table says; `msUntilLinkWindowReload` in `link-window-reload.ts`, test first, and the reload on `ConsultationCasePage`.
6. **Check by hand** on the dev server with Playwright: as Expert 2, **Join** on an upcoming appointment opens the link in a new tab; without a link, the text and its link to **Profile**. As Customer 1, "The link will be available from … (Zurich time)." The customer's **Join** needs an appointment within the hour, which the 24 hours' notice doesn't allow on the real clock: show it by answering the case's request with an open window (Playwright's route interception), and the reload by answering with a `linkAvailableFrom` a few seconds ahead.

## Acceptance criteria

- [ ] The window includes 09:00 and excludes 11:00 for the appointment 10:00–11:00 (domain and REST).
- [ ] Outside the window the customer's case response contains no link: its body doesn't contain the URL at 08:59 and at 11:00 (REST).
- [ ] Inside the window, also while the session runs, the customer's appointment has **Join** with Expert 2's link (REST and Playwright).
- [ ] Before the window the customer reads "The link will be available from 09:00 (Zurich time).", with or without a link in the expert's profile; inside it without a link, "The meeting link has not been provided yet." (REST for the fields, Playwright for the texts).
- [ ] The expert's case has the expert's own link at any time, and the page shows **Join** on each upcoming appointment and on no past or cancelled one; without a link it says "Add your Teams link in your profile." with a link to **Profile** (REST and Playwright).
- [ ] After Expert 2 changed their link, each of their upcoming appointments has the new one, on both sides (REST).
- [ ] With the link set, the bodies of the shop's offering detail and of the eligible experts don't contain it (REST; 05 has the test, it passes unchanged).
- [ ] Expert 1, who booked themself, gets the link on the customer's route only inside the window (REST).
- [ ] A case detail opened before the window shows **Join** when the window opens, without a reload by the customer (Playwright); `msUntilLinkWindowReload` answers the wait, nothing for a time more than a day away or without a waiting appointment, and 30 seconds for a time that has passed while the window is still closed (Vitest).
- [ ] **Join** opens in a new tab, with `rel="noreferrer"`.
- [ ] `openapi.json` and `schema.d.ts` have the new fields; every check passes.

## Tests

- Domain: `LinkWindow` (both ends), `CustomerUpcomingAppointment` (the link only in the window, a running appointment, an expert without a link, a link with a closed window refused), `ExpertConsultationCase` with and without a link
- REST: the customer's case at 08:59, 09:00, 10:30 and 11:00; an expert without a link; a changed link; the expert's case
- Authorization: the security test that the customer's Teams link is absent outside the link window ([security.md](../../architecture/security.md#testing))
- Frontend: Vitest for `msUntilLinkWindowReload`

## Out of scope

- **Join** and the run's link on training days: with the trainings (UC-12, UC-17). They reuse `LinkWindow`.
- A reload when the window closes: the appointment's end doesn't reload the page today either.
- Creating Teams meetings, an embedded call and attendance tracking: UC-10's out of scope.

## Notes
