# UC-09 Cancel training enrollment

| | |
|---|---|
| Actor | customer |
| Scope | PoC: refund flag only. MVP: actual refund. |
| Related | [UC-04](uc-04-purchase-training-run.md), [UC-05](uc-05-view-my-purchases.md) |

## Goal

The customer cancels a training enrollment before the run starts. The seat is freed for others.

## Preconditions

- The user owns the enrollment.
- The enrollment is `ENROLLED`, and the run's first time slot hasn't started yet.

## Main flow

1. The customer clicks **Cancel enrollment** on the enrollment detail.
2. The system checks whether `now < first time slot start − cancellation deadline`:
   - **in time:** "Cancel your enrollment? The purchase price of CHF x will be refunded." (PoC: "would be refunded")
   - **late:** "The free cancellation deadline passed on <date time>. The purchase price will **not** be refunded. Cancel anyway?"
3. The customer confirms.
4. The system locks the run and sets the enrollment to `CANCELLED`, storing `cancelledAt` and `refundEligible`.
5. The seat is free and can be bought again ([UC-04](uc-04-purchase-training-run.md)).

## Alternative and error flows

- **The customer cancels the dialog:** nothing changes.
- **The run has started in the meantime:** 409, "This training has already started and can no longer be cancelled."

## Business rules

```
            first slot start − deadline              first slot start         last slot end
 ───────────────────┼────────────────────────────────────────┼───────────────────────┼──►
  cancel, refund-   │  cancel, not refund-eligible           │  no cancel            │ COMPLETED
  eligible          │  (warning), seat freed                 │                       │ (derived)
```

- The deadline comes from the enrollment's purchase terms, in hours (D-11).
- The server decides "in time" or "late" with its clock at the moment of the request.

## Visibility and authorization

- Only the enrollment owner can cancel.

## Out of scope

- Moving money (MVP), cancelling or rescheduling a whole run, transferring a seat to another person.
