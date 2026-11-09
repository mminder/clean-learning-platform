# 03 Buy from the card, and visitors

| | |
|---|---|
| Increment | [03 Buy a consultation package](../../specs/03-buy-consultation-package.md) |
| Type | AFK |
| Blocked by | [01](01-buy-from-detail-page.md) |
| Status | Done |

## Goal

**Buy** is on every card in the shop, and opens the same dialog as on the detail page. A visitor sees **Buy** too: it takes them to the login and back to the page they were on.

## What to build

Read first: [UC-02](../../use-cases/uc-02-browse-shop.md) step 2 and its business rules, UC-01's [login from Buy](../../use-cases/uc-01-login.md#alternative-and-error-flows), [D-05](../../decisions.md#catalog), and the plan's [buy](README.md#buy) section.

This task is frontend only, in `features/shop/`.

1. **Visitors:** `BuyConsultationButton` shows **Buy** to a visitor; a click opens `/login?redirect=<path, search and hash>`, as `RequireRole` builds it. After the login, `pathAfterLogin` already returns there. The dialog doesn't reopen (spec decision 4).
2. **The card:** `OfferingCard`'s footer gets `BuyConsultationButton` next to the price. The title's link stretches over the whole card (`after:absolute after:inset-0`), so lift the button above it (for example `relative z-10`): a click on **Buy** opens the dialog, a click anywhere else still opens the detail page. The dialog loads the packages itself; the card has only the cheapest price.
3. Check every row of the plan's **Buy** table on the card and on the detail page.

## Acceptance criteria

- [x] On the dev server, a visitor clicks **Buy** on Career Coaching's card, logs in as Customer 1, and is back on the shop with the dialog closed; the same from the detail page returns to the detail page (Playwright).
- [x] Customer 1 buys from the card: the dialog lists both packages, and the case detail opens after confirming.
- [x] A click on the card outside **Buy** still opens the detail page, and keyboard focus reaches both the link and **Buy**.
- [x] Admin 1 sees no **Buy** on any card or detail page.
- [x] Every check passes.

## Tests

- None new: the logic is `pathAfterLogin` and the role check, both tested. The browser check covers the clicks.

## Out of scope

- Reopening the dialog after the login (spec decision 4).

## Notes

- **The dialog on a card vanishes when its offering leaves the shop.** The dialog is a child of the card. After a 409 `OFFERING_NOT_AVAILABLE` or a 404, the mutation reloads the shop list before it fails, the card leaves the list, and the dialog unmounts with it: the customer clicks **Buy** and the dialog just closes, without the "no longer available" message and its **Close** button (checked in the browser: Customer 2 at the confirmation, Admin 1 unpublishes, Customer 2 clicks **Buy**). Nothing is bought, so the outcome is safe, and the shop is right afterwards, but the message is lost. On the detail page the message shows, because that page stays. A fix needs the dialog's open state above the card (for example in `ShopPage`, with `BuyConsultationDialog` controlled), which changes task 01's component and the shop list: not done here. Decide whether to take it into a follow-up.
