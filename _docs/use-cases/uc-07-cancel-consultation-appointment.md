# UC-07 Cancel consultation appointment

| | |
|---|---|
| Actor | customer |
| Scope | PoC |
| Related | [UC-05](uc-05-view-my-purchases.md), [UC-06](uc-06-book-consultation-appointment.md), [UC-08](uc-08-cancel-consultation-case.md) |

## Goal

The customer cancels a booked appointment. The credit returns to the case only if the customer cancels in time.

## Preconditions

- The user owns the case.
- The appointment is `BOOKED` and its start is in the future.

## Main flow

1. The customer clicks **Cancel** on an upcoming appointment.
2. The system checks whether `now < start − cancellation deadline`:
   - **in time:** "Cancel this appointment? The session credit will be returned to your case."
   - **late:** "The free cancellation deadline passed on <date time>. If you cancel, the session credit is **not** refunded. Cancel anyway?"
3. The customer confirms.
4. The system locks the case and sets the appointment to `CANCELLED` with `creditRefunded = true` (in time) or `false` (late).
5. The case detail shows the updated credits, and the slot is free for other customers again.

## Alternative and error flows

- **The customer cancels the dialog:** nothing changes.
- **The appointment has started in the meantime:** 409, "This appointment can no longer be cancelled."

## Business rules

- The deadline comes from the case's purchase terms, in hours (D-11).
- The server decides "in time" or "late" with its clock at the moment of the request.
- A late cancellation forfeits the credit: `open credits = total − BOOKED − forfeited`.
- Consumed appointments can't be cancelled.

## Visibility and authorization

- Only the case owner can cancel.

## Out of scope

- The expert or an admin cancelling appointments, rescheduling, cancellation reasons.
