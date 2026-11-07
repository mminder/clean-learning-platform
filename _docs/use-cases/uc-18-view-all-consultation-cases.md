# UC-18 View all consultation cases, adjust credits

| | |
|---|---|
| Actor | admin |
| Scope | PoC |
| Related | [UC-08](uc-08-cancel-consultation-case.md), [UC-11](uc-11-view-my-consultation-cases.md), [UC-15](uc-15-take-consultation-notes.md) |

## Goal

The admin has an overview of all consultation cases and can grant or remove credits for support reasons.

## Preconditions

- The user has `ADMIN`.

## Main flow

1. The admin opens **Cases**. It's the landing page for admins without `EXPERT`.
2. The system lists all cases, grouped like [UC-11](uc-11-view-my-consultation-cases.md). Each shows the customer's name, the expert's name (or "not assigned"), the offering, the next appointment, open / total credits, and the status.
3. The admin opens a case. The detail shows:
   - customer (name, email), expert, offering, purchase terms;
   - all appointments with their status;
   - the cancellation info (`cancelledAt`, `refundEligible`), if cancelled;
   - the credit adjustment history (delta, reason, admin, time).
4. The admin clicks **Adjust credits**, enters a delta (e.g. `+1` or `-1`) and a reason, and confirms.
5. The system locks the case, validates, and stores the adjustment. Total and open credits update immediately.

## Alternative and error flows

- **Delta 0 or missing reason:** 400.
- **The adjustment would make open credits negative:** 409.
- **The case is cancelled:** 409.

## Business rules

- `total credits = session count from purchase terms + Σ adjustments` (D-13).
- A positive adjustment on a `COMPLETED` case makes it `ACTIVE` again (derived).
- Adjustments are never edited or deleted. A mistake is corrected with a counter-adjustment.

## Visibility and authorization

- Admins see all cases but **never notes** (D-14). The admin case API has no note fields.

## Out of scope

- Changing a case's expert, admin booking or cancelling appointments, refunding forfeited credits individually (use an adjustment instead), search and filters.
