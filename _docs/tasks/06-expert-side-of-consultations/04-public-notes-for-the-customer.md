# 04 Public notes on the customer's case

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [03](03-notes.md) |
| Status | Done |

## Goal

A customer reads their expert's public notes on the case detail, under "Notes from your expert". A private note never reaches the customer's API.

## What to build

Read first: the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), [UC-15](../../use-cases/uc-15-take-consultation-notes.md#visibility-and-authorization), [D-14](../../decisions.md#consultation), [security.md](../../architecture/security.md#shaped-responses), the spec's decision 6, its scenarios [Public notes reach the customer](../../specs/06-expert-side-of-consultations.md#scenario-public-notes-reach-the-customer) and [An expert who is also the customer](../../specs/06-expert-side-of-consultations.md#scenario-an-expert-who-is-also-the-customer), and the plan's [API](README.md#api), [notes](README.md#notes) and [notes on the pages](README.md#notes-on-the-pages) sections.

Work in this order, test first:

1. **A failing REST test:** Expert 2 wrote a private and a public note on Customer 1's case. `GET /api/customer/consultation-cases/{id}` as Customer 1 answers the public one in `notes`, with its title, text, `writtenAt` and `editedAt`, and the body contains neither the private note's id nor its title.
2. **Domain:** `CustomerConsultationCase` gains the notes and refuses a private one.
3. **Adapter and API:** `CustomerConsultationCaseAdapter` loads the case's notes with `visibility = PUBLIC` in the query, newest first; `CustomerNoteResponse`, without a visibility; `notes` in `CustomerConsultationCaseResponse`. Regenerate `openapi.json`, then `npm run generate:api`.
4. **Frontend:** the section "Notes from your expert" on `ConsultationCasePage`, below the appointments, with `NoteCard`; hidden while there's none.
5. **Check by hand** on the dev server with Playwright: as Expert 2, write a private and a public note; as Customer 1, see the public one only; make it private as Expert 2, and it's gone for Customer 1 after a reload.

## Acceptance criteria

- [x] The customer's case shows the public note under "Notes from your expert", with its title, text, time and "edited …" once it was edited; the section is hidden without a public note (Playwright).
- [x] The customer's response contains no private note, not even its id, and no visibility field (REST).
- [x] A note made public appears for the customer, and one made private again disappears (REST).
- [x] The answers of both cancellations, which are the customer's case too, carry the public notes the same way: they load the case through the same port (REST).
- [x] Expert 1, who booked themself and wrote a private and a public note, sees only the public one on the customer's route, and both on the expert's (REST).
- [x] Customer 2 still gets 404 for Customer 1's case.
- [x] `openapi.json` and `schema.d.ts` have `notes` and `CustomerNoteResponse`; every check passes.

## Tests

- Domain: `CustomerConsultationCase` (refuses a private note)
- REST: the customer's case with both kinds of notes, a change of visibility, an expert who is the case's customer
- Authorization: the security test that the customer's case has no private note
- Frontend: none beyond the checks

## Out of scope

- Notes on **My purchases**' cards.
- Customer replies: UC-15's out of scope.
- The admin's case, which never has notes: UC-18.

## Notes

- The REST tests are in `ConsultationCaseNotesTest`, next to the expert's, which already sets up Customer 1's case with Expert 2: the customer's case with both kinds of notes (also the security test: the body has neither the private note's id, title nor text, no `visibility`, and the public note has exactly `id`, `title`, `text`, `writtenAt` and `editedAt`; Customer 2 gets 404), a note made public and private again, both cancellations' answers, and Expert 1 who booked themself. With the `visibility = PUBLIC` condition removed from `NoteRepository.findPublicNotes`, all four fail: three on the private note in the body, and the cancellations with the domain's refusal, which turns a private note that reaches `CustomerConsultationCase` into a 500, never into a response.
- `CustomerNoteResponse` is a field of its own at the end of `CustomerConsultationCaseResponse`; the strict expectation of the purchase's answer in `CustomerConsultationCaseControllerTest` now has `"notes": []`.
- `NoteCard` needed no change: its `NoteContent` is the four fields both responses share.
- The check by hand ran a Playwright script (Playwright from the npx cache, not a project dependency) against the Vite dev server and the backend with the `poc` profile. As in task 03, the dev database had Customer 1's case with Expert 1, not Expert 2, so the check used that case as Expert 1; the only rows it added were two notes, deleted afterwards. Before any note, the customer's case had no "Notes from your expert"; Expert 1 added the private "First session" and the public "Homework" and edited "Homework"; after a reload, Customer 1 saw only "Homework", below the appointments, with "(Zurich time)", its line break, its time and "edited …", without buttons or a badge, and nothing of "First session" on the page; Expert 1 made "Homework" private, and after a reload Customer 1's section was gone. The dev server runs on the real clock, so the times shown were today's.
