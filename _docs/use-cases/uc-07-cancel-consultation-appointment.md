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
2. The system checks whether `now < start − cancellation deadline` and shows a dialog that names the appointment, "Cancel the appointment with <expert> on <date, start–end> (Zurich time)?", with the consequence (decided 2026-11-13):
   - **in time:** "The session credit will be returned to your case."
   - **late:** "The free cancellation deadline passed on <date time>. If you cancel, the session credit is **not** refunded. Cancel anyway?"

   The buttons are **Keep appointment** and **Cancel appointment**.
3. The customer confirms. The request names the outcome the dialog stated: whether the credit is returned (decided 2026-11-13).
4. The system locks the case, checks the rules again, and sets the appointment to `CANCELLED` with `creditRefunded = true` (in time) or `false` (late).
5. The case detail shows the updated credits, without the appointment, and the slot is free for other customers again.

## Alternative and error flows

- **The customer closes the dialog or clicks Keep appointment:** nothing changes.
- **The deadline passed while the dialog was open:** 409 `DEADLINE_PASSED`; nothing is cancelled, and the dialog shows the late warning to confirm again (decided 2026-11-13).
- **The appointment has started in the meantime:** 409 `APPOINTMENT_STARTED`, "This appointment can no longer be cancelled."
- **The appointment was already cancelled**, for example in another tab or with its case: 409 `APPOINTMENT_CANCELLED`, "This appointment was already cancelled." The case reloads.

## Business rules

- The deadline comes from the case's purchase terms, in hours (D-11). Cancelling at the deadline itself is late.
- The server decides "in time" or "late" with its clock at the moment of the request, and cancels only with the outcome the customer confirmed.
- A late cancellation forfeits the credit: `open credits = total − BOOKED − forfeited`.
- Consumed appointments can't be cancelled.
- A case of an archived offering can still cancel: existing purchases keep working (D-07).

## Visibility and authorization

- Only the case owner can cancel. The appointment is addressed under its case, so another customer's case, or an appointment of another case, answers 404.
- The customer doesn't see cancelled appointments: the open credits show the outcome (decided 2026-11-13).

## Out of scope

- The expert or an admin cancelling appointments, rescheduling, cancellation reasons.
