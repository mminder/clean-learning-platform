# 07 Join a call

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [02](02-expert-case-detail.md), [05](05-expert-profile.md) |
| Status | Done |

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

- [x] The window includes 09:00 and excludes 11:00 for the appointment 10:00–11:00 (domain and REST).
- [x] Outside the window the customer's case response contains no link: its body doesn't contain the URL at 08:59 and at 11:00 (REST).
- [x] Inside the window, also while the session runs, the customer's appointment has **Join** with Expert 2's link (REST and Playwright).
- [x] Before the window the customer reads "The link will be available from 09:00 (Zurich time).", with or without a link in the expert's profile; inside it without a link, "The meeting link has not been provided yet." (REST for the fields, Playwright for the texts).
- [x] The expert's case has the expert's own link at any time, and the page shows **Join** on each upcoming appointment and on no past or cancelled one; without a link it says "Add your Teams link in your profile." with a link to **Profile** (REST and Playwright).
- [x] After Expert 2 changed their link, each of their upcoming appointments has the new one, on both sides (REST).
- [x] With the link set, the bodies of the shop's offering detail and of the eligible experts don't contain it (REST; 05 has the test, it passes unchanged).
- [x] Expert 1, who booked themself, gets the link on the customer's route only inside the window (REST).
- [x] A case detail opened before the window shows **Join** when the window opens, without a reload by the customer (Playwright); `msUntilLinkWindowReload` answers the wait, nothing for a time more than a day away or without a waiting appointment, and 30 seconds for a time that has passed while the window is still closed (Vitest).
- [x] **Join** opens in a new tab, with `rel="noreferrer"`.
- [x] `openapi.json` and `schema.d.ts` have the new fields; every check passes.

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

- **Backend:** `LinkWindow` (`domain.shared`) has `OPENS_BEFORE_START`, `opensAt` and `isOpenAt`. `CustomerConsultationCase` holds `Optional<TeamsLink> expertTeamsLink` next to the expert, and `CustomerUpcomingAppointment.of` takes it and keeps it only while the window is open; the record refuses a link with a closed window. `ExpertConsultationCase` holds `Optional<TeamsLink> teamsLink`. `findCustomerCase` selects `p.teamsLink` from the left join it already had; `findExpertCase` gains a left join on the expert's own profile, so a missing profile changes nothing about which case is found. The cancellations' answers carry the window, too: they load the case through the same port.
- **REST tests** are a class of their own, `ConsultationTeamsLinkTest`: the customer's case at 08:59, 09:00, 10:30 and 11:00 (the body contains the link only at 09:00 and 10:30, and a past appointment has exactly `id`, `start`, `end` and `status`); **My purchases** inside the window has no link; the expert's case before, during and after the window; no link; a changed link on two upcoming appointments, both sides; Expert 1 who booked themself. The shop's detail and the eligible experts are 05's `ExpertProfileControllerTest`, unchanged. `ExpertConsultationCaseControllerTest`'s strict expectation now has `"teamsLink": null`. Guarded by hand: with the domain's refusal and the `linkWindowOpen ?` in `CustomerUpcomingAppointment.of` both removed, two REST tests (08:59 and Expert 1's) and two domain tests fail; with the window opening after `opensAt` instead of at it, four REST tests and `LinkWindowTest` fail.
- **Frontend:** **Join** is `JoinButton.tsx`, one component for both pages, so `target="_blank"` and `rel="noreferrer"` can't drift apart; the plan's frontend tree names it now. The customer's texts are a `CallLink` on `ConsultationCasePage`. The reload is an effect on the page that waits `msUntilLinkWindowReload` and then refetches; it depends on the query's `dataUpdatedAt` as well as on the appointments, because a refetch that answers the same data keeps its reference (structural sharing) and would otherwise not start the 30 seconds' retry. The appointments' rows wrap (`flex-wrap`) on both pages, since the customer's text is long on a phone.
- **"from 09:00"** shows only the time, as the plan says (`formatTime`): next to the appointment's date it reads well, but an appointment starting before 01:00 has a window that opens on the day before, and the text doesn't say so.
- **Check by hand** (Playwright from the npx cache, the Vite dev server and the `poc` backend): as in 02 to 06, the dev database's Customer 1 case has Expert 1, not Expert 2, so the check used Expert 1. Without a link, both upcoming appointments said "Add your Teams link in your profile.", "your profile" linking to `/expert/profile`, with no **Join**; Expert 1 saved `https://example.com/meet/expert-1` on **Profile** ("Saved"), and then each upcoming appointment had **Join** (`target="_blank"`, `rel="noreferrer"`), the past one none, and **Join** opened the link in a new tab with an empty `document.referrer` (the host answered locally through Playwright's routing). As Customer 1 on the real clock, the appointment on 25 Nov, 10:00 said "The link will be available from 09:00 (Zurich time)." and its JSON had `teamsLink: null`; the dev database's appointment on 15 Nov, 08:30, which started a minute after the check, was inside its window and had **Join** with the link without any interception. Answering the case with every window open showed **Join**, which opened the link in a new tab; with open windows and no link, "The meeting link has not been provided yet." With `linkAvailableFrom` six seconds ahead, the page showed "The link will be available from …", then **Join** half a second after that time, after a second fetch of the case and without a navigation. Afterwards Expert 1's profile was set back without the link, `updated_at` included; the check added no other rows. The dev database has no cancelled appointment, so "no **Join** on a cancelled one" rests on the page, which puts **Join** only into the upcoming list, and on the REST tests.
