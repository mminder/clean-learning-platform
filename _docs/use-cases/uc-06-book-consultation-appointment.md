# UC-06 Book consultation appointment

| | |
|---|---|
| Actor | customer |
| Scope | PoC |
| Related | [UC-05](uc-05-view-my-purchases.md), [UC-07](uc-07-cancel-consultation-appointment.md), [UC-13](uc-13-manage-availability.md) |

## Goal

The customer turns an open credit into an appointment with an expert.

## Preconditions

- The user owns the case.
- The case is `ACTIVE` and has open credits > 0.

## Main flow

1. The customer clicks **Book appointment** on the case detail.
2. **If the case has no assigned expert yet**, the system shows the offering's eligible experts (name, photo, bio), and the customer selects one.
3. The system shows the dates on which the expert has bookable slots, and the customer selects a date.
4. The system shows that date's bookable start times as "start–end", and the customer selects one.
5. The system shows a confirmation with the expert, date, time and duration, and "Free cancellation until <start − deadline>".
6. The customer confirms.
7. The system locks the case and then the expert, re-checks all rules, and creates an appointment with status `BOOKED`. On the first booking, it also sets the selected expert as the case's assigned expert.
8. The case detail shows the new appointment and the reduced open credits.

## Alternative and error flows

- **The slot was taken in the meantime:** 409, "This time is no longer available." The customer goes back to step 3.
- **No open credits:** **Book appointment** is disabled. If the request arrives anyway: 409.
- **The expert has no bookable slots:** "No free times available at the moment."
- **The case was cancelled:** 409.

## Business rules

- **Bookable slot** (D-10): a start time on a 30-minute grid such that:
  - `[start, start + session duration]` lies fully inside one of the expert's availability ranges on that date;
  - it doesn't overlap the expert's busy time (any `BOOKED` appointment of any case, or a time slot of an assigned run);
  - `start ≥ now + 24 h`.
- The session duration comes from the case's purchase terms.
- There's no buffer between sessions.
- A customer can book several future appointments, up to the open credits.
- The assigned expert can't be changed. All appointments of a case are with the same expert.
- A user with `EXPERT` and `CUSTOMER` may select themself (D-06).
- The expert picker shows the offering's current eligible experts. Removing eligibility later doesn't affect cases where that expert is already assigned.

## Visibility and authorization

- Only the case owner can book.
- The customer only sees free start times, never other customers' appointments.

## Out of scope

- Changing the expert, rescheduling (cancel and rebook instead), recurring appointments, an expert approval step.
