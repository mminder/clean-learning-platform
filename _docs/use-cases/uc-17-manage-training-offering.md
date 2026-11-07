# UC-17 Manage training offering

| | |
|---|---|
| Actor | admin |
| Scope | PoC |
| Related | [UC-02](uc-02-browse-shop.md), [UC-04](uc-04-purchase-training-run.md), [UC-12](uc-12-view-my-trainings.md), [UC-13](uc-13-manage-availability.md) |

This use case also covers "Admin views all trainings" from the initial spec (D-31).

## Goal

The admin creates trainings and their runs, staffs the runs with experts, and keeps an overview of all runs and participants.

## Preconditions

- The user has `ADMIN`.

## Main flow: training

1. The admin opens **Trainings**. The system lists all training offerings in every status. Each shows its runs: dates, seats used / total, and the expert or "unassigned".
2. The admin creates a new training or opens an existing one.
3. The admin edits:
   - name, description, image (upload, replace, remove);
   - price (CHF), cancellation deadline (hours), default seats;
   - day template: an ordered list of days, each with ≥ 1 time slot (local start–end, no overlaps).
4. The admin saves. A new training starts as `DRAFT`.
5. The admin changes the status with **Publish**, **Unpublish**, **Archive** or **Delete** (same rules as [UC-16](uc-16-manage-consultation-offering.md)).

## Main flow: runs

1. In the training detail, the admin clicks **Add run**.
2. The admin enters one date per template day (strictly increasing, not necessarily consecutive), the seats (default: the training's default seats), optionally an expert, and optionally a Teams link.
3. The system creates the run and copies the day template onto the chosen dates as the run's own schedule.
4. On an existing run, the admin can:
   - change the seats (≥ number of active enrollments);
   - assign, re-assign or remove the expert;
   - set the Teams link;
   - change the dates, only while the run has no enrollments;
   - delete the run, only while it has no enrollments;
   - view the participants (name, email, enrollment status, refund eligibility).

## Alternative and error flows

- **The expert's busy time overlaps the run** (booked appointments or another assigned run): 409, listing the conflicting appointments and runs. The assignment isn't saved.
- **Changing dates or deleting a run with enrollments:** 409.
- **Seats below active enrollments:** 409.
- **Publishing without ≥ 1 day with ≥ 1 time slot:** 409.

## Business rules

- Template edits affect only runs created afterwards (D-08).
- Experts can be assigned to runs without having availability. Availability is only for consultations (D-09).
- The shop shows only runs of `PUBLISHED` trainings whose first time slot is in the future ([UC-04](uc-04-purchase-training-run.md)).
- Images follow D-21.

## Visibility and authorization

- Only admins.

## Out of scope

- Cancelling or rescheduling a run that has enrollments (future, needs notifications), multiple experts per run, different time slots per run.
