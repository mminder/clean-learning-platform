# UC-05 View my purchases

| | |
|---|---|
| Actor | customer |
| Scope | PoC |
| Related | [UC-06](uc-06-book-consultation-appointment.md), [UC-07](uc-07-cancel-consultation-appointment.md), [UC-08](uc-08-cancel-consultation-case.md), [UC-09](uc-09-cancel-training-enrollment.md), [UC-10](uc-10-join-call.md) |

## Goal

The customer sees everything they bought, what's next, and what they can do.

## Preconditions

- The user has `CUSTOMER`.

## Main flow: landing page "My purchases"

1. The system shows the customer's cases and enrollments as cards in three groups:

| Group | Contains | Order |
|---|---|---|
| Upcoming | items with a next date (a future `BOOKED` appointment, or a future run day) | next date, ascending |
| Needs booking | `ACTIVE` cases with open credits and no future booked appointment | purchase date, ascending |
| Past (collapsed) | `COMPLETED` or `CANCELLED` cases and enrollments | last date, descending |

2. A **case card** shows the offering name and image, the assigned expert (if any), the next appointment, and "open / total credits".
3. An **enrollment card** shows the training name and image, the run dates, and the next training day.

## Main flow: case detail

The case detail shows:
- the offering name and the purchase terms (session duration, cancellation deadline);
- the assigned expert (name, photo, bio), or "Not assigned yet";
- open and total credits;
- upcoming appointments, each with **Cancel** ([UC-07](uc-07-cancel-consultation-appointment.md)) and the call link ([UC-10](uc-10-join-call.md));
- past appointments: consumed, or cancelled with "credit refunded" / "credit forfeited";
- the expert's public notes, newest first ([UC-15](uc-15-take-consultation-notes.md));
- the actions **Book appointment** ([UC-06](uc-06-book-consultation-appointment.md)), disabled without open credits, and **Cancel case** ([UC-08](uc-08-cancel-consultation-case.md));
- the hint "Book your first session" while the case has no appointment ([UC-03](uc-03-purchase-consultation-package.md)).

## Main flow: enrollment detail

The enrollment detail shows:
- the training name and the purchase terms (cancellation deadline);
- the run schedule (days with time slots);
- the expert, or "to be announced";
- the call link per training day ([UC-10](uc-10-join-call.md));
- the status, and **Cancel enrollment** ([UC-09](uc-09-cancel-training-enrollment.md)).

## Alternative and error flows

- **No purchases:** an empty state with a link to the shop.
- **A case or enrollment of another customer:** 404.

## Business rules

- `COMPLETED` and `CONSUMED` are derived from the current time and never stored (see the [glossary](../glossary.md)).
- Cancelled cases and enrollments stay visible under Past.

## Visibility and authorization

- Only the customer's own cases and enrollments.
- Only `PUBLIC` notes. Private notes are never part of the customer API.

## Out of scope

- Invoices and receipts.
