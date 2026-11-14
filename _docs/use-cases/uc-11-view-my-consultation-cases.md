# UC-11 View my consultation cases

| | |
|---|---|
| Actor | expert |
| Scope | PoC |
| Related | [UC-10](uc-10-join-call.md), [UC-15](uc-15-take-consultation-notes.md) |

## Goal

The expert sees every case they're assigned to and what's coming up.

## Preconditions

- The user has `EXPERT`.

## Main flow

1. The expert opens **My cases**. It's the landing page for experts.
2. The system shows the cases assigned to the expert as cards, grouped:

| Group | Contains | Order |
|---|---|---|
| Upcoming | cases with an upcoming appointment, one that hasn't ended ([glossary](../glossary.md#consultation)) | next appointment, ascending |
| Needs booking | `ACTIVE` cases with open credits and no upcoming appointment | purchase date, ascending, as on "My purchases": nothing stores the first booking date (decided 2026-11-14) |
| Past (collapsed) | `COMPLETED` or `CANCELLED` cases | newest first, as on "My purchases" ([UC-05](uc-05-view-my-purchases.md)) |

3. Each card shows the customer's name, the offering, the next booked appointment (date and time), the open (bookable) credits, and the total credits; a cancelled case's card says "Cancelled" in place of the next appointment and the credits (decided 2026-11-14).
4. The expert opens a case. The detail shows:
   - the customer's name and email;
   - the offering and, of the purchase terms, the session count, the session duration, the cancellation deadline and the purchase time, but not the price paid (decided 2026-11-14);
   - the open and total credits; a cancelled case says instead "Cancelled on <date, time>", without the refund outcome (decided 2026-11-14);
   - all appointments: upcoming until they end (with the Join link, [UC-10](uc-10-join-call.md)), consumed, and, in a list of their own, cancelled (refunded or forfeited), also the ones cancelled with their case (decided 2026-11-14 by the product owner);
   - all notes, private and public ([UC-15](uc-15-take-consultation-notes.md)).

## Alternative and error flows

- **No cases yet:** an empty state.
- **A case assigned to another expert:** 404.

## Business rules

- A case belongs to an expert only after the customer's first booking (D-14). Cases without an assigned expert appear in no expert's list.

## Visibility and authorization

- Only cases where the expert is the assigned expert.
- The customer's email is visible to the assigned expert (D-18).
- The price paid and a cancelled case's refund outcome aren't part of the expert's API (D-18).

## Out of scope

- The expert booking, cancelling or rescheduling appointments; messaging customers.
