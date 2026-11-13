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
- The case is `ACTIVE` and not `COMPLETED`.

## Main flow

1. The customer clicks **Cancel case** on the case detail.
2. The system evaluates refund eligibility and shows the matching dialog:
   - **eligible:** "Cancel this case? The purchase price of CHF x would be refunded." (MVP, once money moves: "will be refunded")
   - **not eligible:** "Cancel this case? The purchase price would **not** be refunded because <reason>. Cancel anyway?"

   Either dialog lists the future appointments that will be cancelled, each as "<date, start–end> with <expert>". The buttons are **Keep case** and **Cancel case** (decided 2026-11-13).
3. The customer confirms. The request names the outcome the dialog stated: whether the case is refund-eligible (decided 2026-11-13).
4. The system locks the case, checks the rules again, and:
   - sets every future `BOOKED` appointment to `CANCELLED`, each with `creditRefunded` as UC-07 would decide at that moment: `true` before its deadline, `false` after (decided 2026-11-13);
   - sets the case to `CANCELLED`, storing `cancelledAt` and `refundEligible`.
5. The case detail says "Cancelled on <date, time>" and whether the purchase price would be refunded, in place of the credits. The case moves to Past on the landing page, with "Cancelled" on its card. No more bookings are possible.

## Alternative and error flows

- **The customer closes the dialog or clicks Keep case:** nothing changes.
- **Refund eligibility changed while the dialog was open**, for example because an appointment's deadline passed: 409 `REFUND_ELIGIBILITY_CHANGED`; nothing is cancelled, and the dialog shows the new outcome to confirm again (decided 2026-11-13).
- **The case was already cancelled**, for example in another tab: 409 `CASE_CANCELLED`, "This case was cancelled."
- **The case is completed:** 409 `CASE_COMPLETED`, "This case is completed."

## Business rules

The case is refund-eligible if and only if:

```
no appointment is CONSUMED
∧ no credit is forfeited (no late-cancelled appointment)
∧ every future BOOKED appointment is still before its cancellation deadline
```

- A case with no appointments at all is refund-eligible, and so is one whose appointments were all cancelled in time.
- The dialog gives the first reason that applies, in this order (decided 2026-11-13):
  1. "a session has already taken place."
  2. "the credit of a session cancelled late was forfeited."
  3. "the free cancellation deadline of your appointment on <date, start–end> passed on <date, time>.", naming the soonest such appointment.
- The PoC says "would be refunded" and "would **not** be refunded", in the dialog and on the cancelled case.
- Credit adjustments don't affect refund eligibility.
- `COMPLETED` and `CANCELLED` cases can't be cancelled. A case is `COMPLETED` once it has no open credits and no booked appointment that hasn't ended, so while its last session runs it can still be cancelled, without a refund.

## Visibility and authorization

- Only the case owner can cancel the case.
- The customer doesn't see the appointments cancelled with the case ([UC-05](uc-05-view-my-purchases.md), decided 2026-11-13).
- Admins see the cancellation and `refundEligible` ([UC-18](uc-18-view-all-consultation-cases.md)).

## Out of scope

- Moving money (MVP), partial refunds, admin-initiated case cancellation.
