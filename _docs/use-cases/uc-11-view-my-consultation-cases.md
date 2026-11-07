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
| Upcoming | cases with a future `BOOKED` appointment | next appointment, ascending |
| Needs booking | `ACTIVE` cases with open credits and no future booked appointment | first booking date, ascending |
| Past (collapsed) | `COMPLETED` or `CANCELLED` cases | last appointment, descending |

3. Each card shows the customer's name, the offering, the next booked appointment (date and time), the open (bookable) credits, and the total credits.
4. The expert opens a case. The detail shows:
   - the customer's name and email;
   - the offering and the purchase terms;
   - all appointments: upcoming (with the Join link, [UC-10](uc-10-join-call.md)), consumed, and cancelled (refunded or forfeited);
   - all notes, private and public ([UC-15](uc-15-take-consultation-notes.md)).

## Alternative and error flows

- **No cases yet:** an empty state.
- **A case assigned to another expert:** 404.

## Business rules

- A case belongs to an expert only after the customer's first booking (D-14). Cases without an assigned expert appear in no expert's list.

## Visibility and authorization

- Only cases where the expert is the assigned expert.
- The customer's email is visible to the assigned expert (D-18).

## Out of scope

- The expert booking, cancelling or rescheduling appointments; messaging customers.
