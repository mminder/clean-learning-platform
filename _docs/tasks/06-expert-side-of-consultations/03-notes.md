# 03 Notes

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [02](02-expert-case-detail.md) |
| Status | Done |

## Goal

The assigned expert adds, edits and deletes notes on a case, each private or public, and sees them on the case detail, newest first. These are the first rows a user creates, edits and deletes in a list of their own.

## What to build

Read first: [UC-15](../../use-cases/uc-15-take-consultation-notes.md), [D-14](../../decisions.md#consultation), [D-36](../../decisions.md#architecture), the spec's decisions 5, 7 and 17, its requirement [Notes](../../specs/06-expert-side-of-consultations.md#requirement-notes), and the plan's [schema](README.md#schema), [API](README.md#api), [notes](README.md#notes), [ownership](README.md#ownership), [notes on the pages](README.md#notes-on-the-pages), [queries](README.md#queries) and [tests](README.md#tests) sections. Task 03 of [increment 02](../02-manage-consultation-offerings/03-create-and-edit-consultation-offering.md) is the pattern for a write with its 400s and its form.

Work in this order, test first:

1. **A failing REST test:** on Customer 1's case with Expert 2, with the clock at Mon 9 Nov 2026, 15:00 (Zurich), `POST /api/expert/consultation-cases/{id}/notes` as Expert 2 with the title "First session", a text of two lines and `PRIVATE` answers 201 with the note, `writtenAt` 9 Nov, 15:00, and no `editedAt`. The case's detail then lists it.
2. **Domain:** `NoteId`, `NoteTitle`, `NoteText` and `NoteVisibility` in `domain.consultation.value`; `Note` with `write` and `edit`. `ExpertConsultationCase` gains the notes.
3. **Schema and adapter:** `V11__create_note.sql` with its trigger; `NoteEntity`, `NoteRepository` and `NoteAdapter`, which implements `CheckAssignedExpertPort`, `LoadNotePort`, `SaveNotePort` and `DeleteNotePort`, each scoped as the plan says; `ExpertConsultationCaseAdapter` loads the case's notes, newest first. `delete-test-data.sql` deletes every note before the cases.
4. **Use cases and API:** `AddNoteUseCase`, `EditNoteUseCase` and `DeleteNoteUseCase`; the three routes with `NoteRequest` and `ExpertNoteResponse`; `notes` in `ExpertConsultationCaseResponse`; role matrix rows. Regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:** `useAddNote`, `useEditNote` and `useDeleteNote`; `NoteCard` and `NoteDialog`; the notes on `ExpertConsultationCasePage` with **Add note**, **Edit** and **Delete**.
6. **Check by hand** on the dev server with Playwright: add a private note with two lines, edit it and make it public, delete it after the confirmation; an empty title and an empty text; then open a note in two tabs, delete it in one and save it in the other.

## Acceptance criteria

- [x] A new note is first in the list, with the badge "Private", the time it was written and its line break; `POST` answers 201 (REST and Playwright).
- [x] An edit at 10 Nov 2026, 09:00 answers 200 with that `editedAt`; the note says "edited 10 Nov 2026, 09:00" and keeps its place in the list, also when only its visibility changed (REST and Playwright).
- [x] A delete answers 204 after the confirmation "Delete the note "First session"?" with "This can't be undone."; a second delete answers 404 (REST and Playwright).
- [x] A title or a text that is missing, empty or only spaces answers 400 with a message next to each field, and nothing is saved; a missing visibility answers 400 (REST and Playwright).
- [x] Notes can be added, edited and deleted on an active, a completed and a cancelled case (REST).
- [x] Expert 1 adding, editing or deleting a note on Customer 1's case, which has Expert 2, gets 404, and nothing changes; so does a note of another case of Expert 2 under this case's path (REST).
- [x] Saving a note that was deleted in another tab says "This note no longer exists.", and the notes reload (Playwright).
- [x] The note's times come from the `Clock`, not from the database (the REST tests set the clock).
- [x] `openapi.json` and `schema.d.ts` have the three routes and `notes`; the role matrix has the three rows; every check passes.

## Tests

- Domain: `NoteTitle` and `NoteText` (blank, no-break spaces), `Note` (`write`; `edit` with each change and with none), `ExpertConsultationCase` with notes
- REST: the three routes' happy paths; the list's order after an edit; the 400s; a case in each status; unknown and malformed ids; 415
- Authorization: the role matrix rows; ownership with both seeded experts, and a note under another case's path
- Frontend: none beyond the checks

## Out of scope

- The customer reading public notes: [04](04-public-notes-for-the-customer.md).
- A lock or a version for two tabs: the last save wins (spec decision 7).
- Customer replies, attachments, notes per appointment and rich text: UC-15's out of scope.

## Notes

- The check by hand ran a Playwright script (Playwright from the npx cache, not a project dependency) against the Vite dev server and the backend with the `poc` profile. The dev database already had Customer 1's case with Expert 1, so the check used that case as Expert 1, and the only rows it added were notes, deleted afterwards. It added a private note with two lines (201, first, "Private", the line break, the time), sent an empty and an only-spaces title and text (400, "must not be blank" next to both fields, nothing saved), edited the older of two notes and made it public, then private again (200, "edited …", its place kept), deleted it after the confirmation and its **Cancel** (204), and saved in one tab a note deleted in another (404, "This note no longer exists." with only **Close**, the notes reloaded). The dev server runs on the real clock, so the page showed today's times; the times of 9 and 10 Nov 2026 are checked by the REST tests with the `TestClock`.
- `note.title` and `note.text` are declared `collate "und-x-icu"`, which the plan's schema didn't say: without it Postgres's `\S` doesn't treat the no-break spaces as blank, so the check would accept a title that `NoteTitle` refuses. `NameCheckConstraintsTest` covers both columns and fails without the collation. The plan's schema now says so.
- A delete's 404 (a note deleted in another tab) shows "This note no longer exists." above the notes, not in a dialog: `ConfirmDialog` closes when it's confirmed. The notes reload, as after a save's 404. The plan's "Notes on the pages" now says so.
- `ConsultationCaseMapper.toNote` maps a `NoteEntity`, so 04's customer adapter can use it, too.
- The ownership test fails when any of the three scoped queries (`findExpertNote`, `deleteExpertNote`, `isAssignedExpert`) drops its case or its expert condition: each was loosened in turn to check.
- Known gap, not handled (spec decision 7, no lock): a note deleted by another request between `EditNoteUseCase`'s load and its update, in the same few milliseconds, makes Hibernate's update hit no row and answers 500 instead of 404.
