# Use cases

## Actors

| Actor | Description |
|---|---|
| Visitor | Anyone who isn't logged in. Can browse the shop. |
| Customer | A private person who buys, books and takes part in consultations and trainings. |
| Expert | A platform employee who coaches customers and leads training runs. |
| Admin | A back-office employee who manages offerings, cases and (MVP) users. |

A user can hold several roles. The header shows a section for each of them, and the landing page follows the priority Expert → Admin → Customer ([UC-01](uc-01-login.md)).

## Overview

| ID | Use case | Actor | PoC | MVP |
|---|---|---|---|---|
| [UC-01](uc-01-login.md) | Log in / log out | all | user picker | OIDC + JIT provisioning |
| [UC-02](uc-02-browse-shop.md) | Browse shop and offering detail | visitor, customer | ✓ | |
| [UC-03](uc-03-purchase-consultation-package.md) | Purchase consultation package | customer | ✓ without payment | + payment |
| [UC-04](uc-04-purchase-training-run.md) | Purchase training run | customer | ✓ without payment | + payment |
| [UC-05](uc-05-view-my-purchases.md) | View my purchases | customer | ✓ | |
| [UC-06](uc-06-book-consultation-appointment.md) | Book consultation appointment | customer | ✓ | |
| [UC-07](uc-07-cancel-consultation-appointment.md) | Cancel consultation appointment | customer | ✓ | |
| [UC-08](uc-08-cancel-consultation-case.md) | Cancel consultation case | customer | ✓ flag only | + refund |
| [UC-09](uc-09-cancel-training-enrollment.md) | Cancel training enrollment | customer | ✓ flag only | + refund |
| [UC-10](uc-10-join-call.md) | Join call | customer, expert | ✓ | |
| [UC-11](uc-11-view-my-consultation-cases.md) | View my consultation cases | expert | ✓ | |
| [UC-12](uc-12-view-my-trainings.md) | View my trainings | expert | ✓ | |
| [UC-13](uc-13-manage-availability.md) | Manage availability | expert | ✓ | |
| [UC-14](uc-14-manage-expert-profile.md) | Manage expert profile | expert | ✓ | |
| [UC-15](uc-15-take-consultation-notes.md) | Take consultation notes | expert | ✓ | |
| [UC-16](uc-16-manage-consultation-offering.md) | Manage consultation offering | admin | ✓ | |
| [UC-17](uc-17-manage-training-offering.md) | Manage training offering | admin | ✓ | |
| [UC-18](uc-18-view-all-consultation-cases.md) | View all consultation cases, adjust credits | admin | ✓ | |
| [UC-19](uc-19-manage-users.md) | Manage users | admin | – | ✓ |

## Cross-cutting rules

- **Time.** All times are in `Europe/Zurich` and displayed with a "(Zurich time)" hint. See the [glossary](../glossary.md#time).
- **Confirmation.** Every purchase, booking and cancellation, and every status change of an offering ([UC-16](uc-16-manage-consultation-offering.md), [UC-17](uc-17-manage-training-offering.md)), needs a confirmation dialog that states the consequence: the price, whether the credit is refunded, whether the purchase is refund-eligible, or what the shop shows afterwards.
- **The server decides.** Deadlines, seats, credits and slots are evaluated by the backend at the moment of the request. The UI only previews them. A request names what the customer confirmed (the price, the expert, whether a credit or the price would be refunded); if the server finds something else, it answers 409, changes nothing, and the dialog shows what's current to confirm again (decided 2026-11-09 for purchases and bookings, 2026-11-13 for cancellations).
- **Lists.** "My …" lists are grouped into Upcoming (by next date), Needs booking, and Past (collapsed, newest first).
- **Money.** CHF including VAT. No money moves in the PoC; a refund is recorded as a `refundEligible` flag.
- **Visibility.** Users only see what belongs to them. A resource of someone else returns 404 ([security](../architecture/security.md)).

## Future (not planned)

- An Order entity and payment provider integration (MVP)
- Email notifications
- Changing a case's expert (e.g. when an expert is sick)
- An expert or admin cancelling appointments
- Rescheduling or cancelling a whole run
- A waitlist for sold-out runs
- An embedded video call (e.g. Jitsi) instead of Teams links
- GDPR features (data export, account deletion)
- Per-user time zone, multiple UI languages
- An availability check before purchase
- Attendance or no-show tracking
- A guard for unsaved form edits, e.g. when the admin publishes, unpublishes or archives an offering with a changed form ([UC-16](uc-16-manage-consultation-offering.md))
- Keeping the buy dialog open with its message when its offering leaves the shop while the customer is in another tab, and returning the focus after **Close** when the offering's card has left the shop ([UC-03](uc-03-purchase-consultation-package.md); today the shop reloads on the tab's return and the dialog closes with the card or the detail page)
- A person's appointments as a customer and as an expert don't block each other: an expert who is also a customer can be booked as an expert at a time they booked as a customer, and the other way round ([UC-06](uc-06-book-consultation-appointment.md), [UC-13](uc-13-manage-availability.md))
- A time range of the availability that ends at midnight, so that 23:30–24:00 can be offered; today a range can't end at midnight ([UC-13](uc-13-manage-availability.md))
- 24-hour time inputs on the availability page whatever the browser's language; today they follow the browser, for example 12-hour in English (US) ([UC-13](uc-13-manage-availability.md))
- A hint without "first" on a case whose appointments were all cancelled and that has open credits left; today it says "Book your first session" again, as on a new case ([UC-05](uc-05-view-my-purchases.md))

## Template

Every use-case file has the same sections: metadata (actor, scope, related), Goal, Preconditions, Main flow, Alternative and error flows, Business rules, Visibility and authorization, Out of scope.
