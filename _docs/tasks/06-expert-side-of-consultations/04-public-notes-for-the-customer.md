# 04 Public notes on the customer's case

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [03](03-notes.md) |
| Status | Todo |

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

- [ ] The customer's case shows the public note under "Notes from your expert", with its title, text, time and "edited …" once it was edited; the section is hidden without a public note (Playwright).
- [ ] The customer's response contains no private note, not even its id, and no visibility field (REST).
- [ ] A note made public appears for the customer, and one made private again disappears (REST).
- [ ] The answers of both cancellations, which are the customer's case too, carry the public notes the same way: they load the case through the same port (REST).
- [ ] Expert 1, who booked themself and wrote a private and a public note, sees only the public one on the customer's route, and both on the expert's (REST).
- [ ] Customer 2 still gets 404 for Customer 1's case.
- [ ] `openapi.json` and `schema.d.ts` have `notes` and `CustomerNoteResponse`; every check passes.

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
