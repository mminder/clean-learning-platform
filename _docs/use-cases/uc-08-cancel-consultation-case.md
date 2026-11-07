# UC-08 Cancel consultation case

| | |
|---|---|
| Actor | customer |
| Scope | PoC: refund flag only. MVP: actual refund. |
| Related | [UC-05](uc-05-view-my-purchases.md), [UC-07](uc-07-cancel-consultation-appointment.md) |

## Goal

The customer cancels a whole case, for example after a wrong purchase. The purchase is refund-eligible only if nothing was used.

## Preconditions

- The user owns the case.
- The case is `ACTIVE`.

## Main flow

1. The customer clicks **Cancel case** on the case detail.
2. The system evaluates refund eligibility and shows the matching dialog:
   - **eligible:** "Cancel this case? The purchase price of CHF x will be refunded." (PoC: "would be refunded")
   - **not eligible:** "Cancel this case? The purchase price will **not** be refunded because <reason>. Cancel anyway?"
   Either dialog lists the future appointments that will be cancelled.
3. The customer confirms.
4. The system locks the case and:
   - sets every future `BOOKED` appointment to `CANCELLED`;
   - sets the case to `CANCELLED`, storing `cancelledAt` and `refundEligible`.
5. The case moves to Past on the landing page. No more bookings are possible.

## Alternative and error flows

- **The customer cancels the dialog:** nothing changes.

## Business rules

The case is refund-eligible if and only if:

```
no appointment is CONSUMED
∧ no credit is forfeited (no late-cancelled appointment)
∧ every future BOOKED appointment is still before its cancellation deadline
```

- A case with no appointments at all is refund-eligible.
- Credit adjustments don't affect refund eligibility.
- `COMPLETED` and `CANCELLED` cases can't be cancelled.

## Visibility and authorization

- Only the case owner can cancel the case.
- Admins see the cancellation and `refundEligible` ([UC-18](uc-18-view-all-consultation-cases.md)).

## Out of scope

- Moving money (MVP), partial refunds, admin-initiated case cancellation.
