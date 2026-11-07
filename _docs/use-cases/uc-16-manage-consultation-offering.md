# UC-16 Manage consultation offering

| | |
|---|---|
| Actor | admin |
| Scope | PoC |
| Related | [UC-02](uc-02-browse-shop.md), [UC-03](uc-03-purchase-consultation-package.md), [UC-06](uc-06-book-consultation-appointment.md) |

## Goal

The admin creates consultation offerings, maintains them, and controls whether they're in the shop.

## Preconditions

- The user has `ADMIN`.

## Main flow

1. The admin opens **Consultations**. The system lists all consultation offerings in every status, with name, status, number of packages and number of eligible experts.
2. The admin creates a new offering or opens an existing one.
3. The admin edits:
   - name, description, image (upload, replace, remove);
   - session duration (minutes);
   - cancellation deadline (hours);
   - packages: add, edit or remove packages (session count, price in CHF);
   - eligible experts: select from users with `EXPERT`.
4. The admin saves. A new offering starts as `DRAFT`.
5. The admin changes the status with **Publish**, **Unpublish**, **Archive** or **Delete**.

## Alternative and error flows

- **Publishing an incomplete offering:** 409, listing what's missing.
- **Unpublish or delete when purchases exist:** 409, "Offering has purchases. Archive it instead."
- **Invalid values** (duration ≤ 0, deadline < 0, session count ≤ 0, price < 0): 400.

## Business rules

```
          publish              archive
 DRAFT ─────────────► PUBLISHED ───────────► ARCHIVED (final, read-only)
   ▲  │                   │
   │  └─ delete ──► gone  │  (delete and unpublish only without purchases)
   └──── unpublish ───────┘
```

- **Publish** requires ≥ 1 package and ≥ 1 eligible expert.
- `ARCHIVED`: not in the shop and not buyable. Existing cases keep working, including booking their remaining credits.
- Edits never change existing cases, because purchase terms are copied at purchase (D-08). Name, description and image are always shown live.
- Removing an eligible expert only affects future expert selections, not cases where that expert is already assigned.
- Images follow D-21.

## Visibility and authorization

- Only admins. Changes are visible in the shop immediately when the offering is `PUBLISHED`.

## Out of scope

- Offering categories, scheduled publishing, price history, discounts.
