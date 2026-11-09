# UC-03 Purchase consultation package

| | |
|---|---|
| Actor | customer |
| Scope | PoC without payment. MVP adds payment. |
| Related | [UC-02](uc-02-browse-shop.md), [UC-05](uc-05-view-my-purchases.md), [UC-06](uc-06-book-consultation-appointment.md) |

## Goal

The customer buys a package and receives a case with session credits.

## Preconditions

- The consultation offering is `PUBLISHED`.
- The user has `CUSTOMER`. A visitor is sent to [UC-01](uc-01-login.md) first and then returns.

## Main flow

1. The customer clicks **Buy** on the offering card or the detail page.
2. The system shows a dialog listing the offering's packages (sessions and price).
3. The customer selects a package.
4. The system asks: "Do you really want to buy *Career Coaching, 3 sessions* for CHF 450?"
5. The customer confirms.
6. The system creates a case:
   - status `ACTIVE`, with no assigned expert yet and no appointments;
   - purchase terms copied from the offering: price paid, session count, session duration, cancellation deadline;
   - the purchase time.
7. The system opens the case detail ([UC-05](uc-05-view-my-purchases.md)) with the hint "Book your first session".

## Alternative and error flows

- **The customer cancels the dialog:** nothing is created.
- **The offering was unpublished or archived in the meantime:** 409, "This offering is no longer available."
- **The package changed in the meantime** (its price or session count differs from what the customer confirmed, or it was removed): 409, "This package has changed or is no longer available. Please choose again." The dialog shows the current packages, and nothing is created (decided 2026-11-09).

## Business rules

- Purchase terms never change after the purchase (D-08).
- The server buys only at the price and session count the customer confirmed (decided 2026-11-09).
- A customer may buy the same offering several times; each purchase is a separate case.
- The purchase doesn't book an appointment and doesn't assign an expert.

## Visibility and authorization

- The case belongs to the purchasing customer. Only that customer, the assigned expert (after the first booking) and admins can see it.

## Out of scope

- Payment (MVP), vouchers, invoices.
