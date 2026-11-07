# UC-12 View my trainings

| | |
|---|---|
| Actor | expert |
| Scope | PoC |
| Related | [UC-10](uc-10-join-call.md), [UC-17](uc-17-manage-training-offering.md) |

## Goal

The expert sees the runs they lead, when they take place, and who attends.

## Preconditions

- The user has `EXPERT`.

## Main flow

1. The expert opens **My trainings**.
2. The system lists every training in which the expert is assigned to at least one run. Each card shows the training name and image and the start of the expert's next run.
3. The expert opens a training. The detail shows **only the expert's own runs**, past and future. Each run shows:
   - its days and time slots;
   - seats used / total;
   - the Teams link ([UC-10](uc-10-join-call.md));
   - the participants (name, email), active enrollments only.

## Alternative and error flows

- **No assigned runs:** an empty state.
- **A training with no run assigned to the expert:** 404.

## Business rules

- Trainings are sorted by the start of the expert's next run, ascending. Trainings without a future own run come last.
- Cancelled enrollments don't appear in the participant list.

## Visibility and authorization

- An expert never sees runs, participants or Teams links of runs assigned to other experts (D-18).

## Out of scope

- Editing runs (admin only, [UC-17](uc-17-manage-training-offering.md)), messaging participants, attendance lists.
