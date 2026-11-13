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
2. **If the case has no assigned expert yet**, the system shows the offering's eligible experts (name, photo, bio), and the customer selects one. Until expert photos exist ([UC-14](uc-14-manage-expert-profile.md)), a placeholder stands in for the photo, as on the offering detail.
3. The system shows the dates on which the expert has bookable slots, and the customer selects a date.
4. The system shows that date's bookable start times as "start–end", and the customer selects one.
5. The system shows a confirmation with the expert, date, time and duration, and "Free cancellation until <start − deadline>", for example: "Book Expert 2 on 17 Nov 2026, 10:00–11:00 (60 min, Zurich time)?" and "Free cancellation until 16 Nov 2026, 10:00." If start − deadline isn't after now, which a deadline longer than the minimum notice allows, it says "This time is within the cancellation deadline, so it can't be cancelled for free." instead (decided 2026-11-10).
6. The customer confirms.
7. The system locks the case and then the expert, re-checks all rules, and creates an appointment with status `BOOKED`. On the first booking, it also sets the selected expert as the case's assigned expert. The request names the expert the customer saw: the selected one, or the case's (decided 2026-11-09).
8. The case detail shows the new appointment and the reduced open credits.

## Alternative and error flows

- **The slot was taken in the meantime**, by another booking with the expert, or by one of the customer's own on another case: 409, "This time is no longer available." The customer goes back to step 3, with the times reloaded.
- **The offering has no eligible experts** (case without an assigned expert): "No experts are available for this offering yet. Please try again later." **Book appointment** stays enabled (decided 2026-11-09).
- **The selected expert is no longer eligible** (first booking): 409, "This expert is no longer available for this offering. Please choose another." The customer goes back to step 2, with the experts reloaded.
- **The case got another expert in the meantime** (a first booking in another tab): 409, "This case already has an expert. Please choose a time with them." The customer goes back to step 3, with the case's expert.
- **No open credits:** **Book appointment** is disabled. If the request arrives anyway: 409.
- **The expert has no bookable slots:** "No free times available at the moment."
- **The case was cancelled**, for example in another tab ([UC-08](uc-08-cancel-consultation-case.md)): the times and the booking answer 409 `CASE_CANCELLED`, "This case was cancelled."

## Business rules

- **Bookable slot** (D-10): a start time on a 30-minute grid such that:
  - `[start, start + session duration]` lies fully inside one of the expert's availability ranges on that date;
  - it doesn't overlap the expert's busy time (any `BOOKED` appointment of any case, or a time slot of an assigned run);
  - it doesn't overlap the customer's busy time (any `BOOKED` appointment of any of their cases, with any expert);
  - `start ≥ now + 24 h`.
- Availability ranges and the grid are Zurich wall-clock times, but a session lasts its real duration. On the days of the summer-time changes, a start time is offered only if it exists exactly once on that date, and the session must end, as an instant, at or before the end of its range (D-10, decided 2026-11-09).
- The session duration comes from the case's purchase terms.
- There's no buffer between sessions.
- A customer's appointments never overlap, across all their cases and experts, so a time that overlaps one of them isn't offered, and a booking of it is refused as a taken slot. Touching is fine. The booking doesn't lock the customer, so two bookings by the same customer at the same instant, on two cases with two different experts, could still overlap; that's accepted (decided 2026-11-09).
- A customer can book several future appointments, up to the open credits.
- The assigned expert can't be changed. All appointments of a case are with the same expert.
- A user with `EXPERT` and `CUSTOMER` may select themself (D-06).
- The expert picker shows the offering's current eligible experts, by display name. Removing eligibility later doesn't affect cases where that expert is already assigned.
- A case of an archived offering can still book: existing purchases keep working (D-07).

## Visibility and authorization

- Only the case owner can book.
- The customer only sees free start times, never other customers' appointments.

## Out of scope

- Changing the expert, rescheduling (cancel and rebook instead), recurring appointments, an expert approval step.
