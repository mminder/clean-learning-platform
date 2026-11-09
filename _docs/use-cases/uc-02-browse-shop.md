# UC-02 Browse shop and offering detail

| | |
|---|---|
| Actor | visitor, customer (any user) |
| Scope | PoC |
| Related | [UC-03](uc-03-purchase-consultation-package.md), [UC-04](uc-04-purchase-training-run.md) |

## Goal

Let anyone see what the platform offers and start a purchase.

## Preconditions

None. No login is required.

## Main flow

1. The visitor opens the shop. It's the start page for visitors.
2. The system lists all published offerings as cards, sorted by name. Each card shows:
   - image, name and description, shortened to three lines;
   - a type badge: **Consultation** or **Training**;
   - the price: "from CHF x" (the cheapest package) for a consultation, the price for a training;
   - a **Buy** button.
3. The visitor opens an offering's detail page. It shows the offering's image above its name, or a placeholder without one.
4. For a **consultation**, the detail page shows the description, session duration, cancellation deadline, the packages (sessions and price), and the eligible experts (name, photo, bio).
5. For a **training**, the detail page shows the description, price, cancellation deadline, the day template (days with time slots), and the buyable runs. Each run shows its dates, "N seats left" or "Sold out", and the expert's name or "to be announced".
6. **Buy** on the card or the detail page starts [UC-03](uc-03-purchase-consultation-package.md) or [UC-04](uc-04-purchase-training-run.md).

## Alternative and error flows

- **Training without upcoming runs:** the detail page shows "No upcoming dates", and **Buy** is disabled.
- **Offering not published, or unknown:** the detail page returns 404. So does a published consultation offering without packages.

## Business rules

- Only `PUBLISHED` offerings appear. A consultation offering also needs at least one package; without one it's hidden from the list and its detail page returns 404 ([UC-16](uc-16-manage-consultation-offering.md#business-rules)). One without eligible experts still appears.
- Runs are listed when their first time slot is in the future. Sold-out runs are shown but can't be selected.
- **Buy** is shown to visitors and to users with `CUSTOMER`. It's hidden for logged-in users without `CUSTOMER`.

## Visibility and authorization

- `/api/shop/**` and `GET /api/images/{id}` are public.
- The shop never shows participants, enrollment details or draft and archived offerings.

## Out of scope

- Shopping cart, search, filters, categories, ratings.
