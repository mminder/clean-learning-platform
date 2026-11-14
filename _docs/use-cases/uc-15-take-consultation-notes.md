# UC-15 Take consultation notes

| | |
|---|---|
| Actor | expert |
| Scope | PoC |
| Related | [UC-05](uc-05-view-my-purchases.md), [UC-11](uc-11-view-my-consultation-cases.md), [UC-18](uc-18-view-all-consultation-cases.md) |

## Goal

The expert keeps notes on a case, either for themself or shared with the customer.

## Preconditions

- The user is the case's assigned expert.

## Main flow

1. On the case detail ([UC-11](uc-11-view-my-consultation-cases.md)), the expert clicks **Add note**.
2. The expert enters a title and text and chooses the visibility: **Private** (the default) or **Public**.
3. The expert saves. The system stores the note with the time it was written.
4. The note appears in the list, newest first, with its visibility badge.

## Alternative flows

- **Edit:** the expert changes the title, text or visibility. The note then shows "edited <date, time>", the time of its last save, and keeps its place in the list. Every save counts as an edit, also one that changes only the visibility (decided 2026-11-14).
- **Delete:** after confirmation, the note is removed permanently.
- **Missing title or text, or only spaces:** 400.
- **The note was deleted meanwhile, in another tab:** 404, and the notes reload. Two saves of one note don't lock: the last one wins (decided 2026-11-14).

## Business rules

- Notes belong to the case, not to a single appointment.
- Plain text with line breaks; no Markdown or rich text.
- Notes can be written on cases in any status (active, completed or cancelled).

## Visibility and authorization

| | Private note | Public note |
|---|---|---|
| Assigned expert | read and write | read and write |
| Customer (case owner) | – | read |
| Admin | – | – |
| Other experts | – | – |

- Admin case views never contain notes (D-14).

## Out of scope

- Customer replies, attachments, notes per appointment, rich text.
