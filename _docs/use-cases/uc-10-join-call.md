# UC-10 Join call

| | |
|---|---|
| Actor | customer, expert |
| Scope | PoC |
| Related | [UC-05](uc-05-view-my-purchases.md), [UC-11](uc-11-view-my-consultation-cases.md), [UC-12](uc-12-view-my-trainings.md), [UC-14](uc-14-manage-expert-profile.md), [UC-17](uc-17-manage-training-offering.md) |

## Goal

Participants find the right Teams link at the right time.

## Preconditions

- The user is the case owner, the assigned expert, an enrolled customer, or the run's expert.

## Main flow

1. The user opens the case detail (each appointment) or the enrollment or run detail (each training day).
2. The system shows a **Join** button that opens the Teams link in a new tab.

## Alternative and error flows

- **Customer before the link window:** instead of the button, "The link will be available from 13:00 (Zurich time).", whether a link is configured or not. The button appears when the window opens, without a reload (decided 2026-11-14).
- **Customer after the call ended:** no link.
- **No link configured** (the expert profile has no Teams link, or the run has none): the customer reads "The meeting link has not been provided yet." inside the link window. The expert reads "Add your Teams link in your profile." on their appointments, with a link to **Profile** ([UC-14](uc-14-manage-expert-profile.md), decided 2026-11-14).

## Business rules

| Call | Link source | Window for customers |
|---|---|---|
| Consultation appointment | the assigned expert's profile Teams link | from appointment start − 1 h until appointment end |
| Training day | the run's Teams link (the same for all days) | from the day's first slot start − 1 h until its last slot end |

- Experts always see their links.
- A consultation appointment stays among the upcoming ones, with **Join**, until it ends, though it's consumed from its start ([UC-05](uc-05-view-my-purchases.md), decided 2026-11-09).
- The API only returns the link to customers inside the window.

## Visibility and authorization

- Only participants of the call (customer, expert) get the link. Admins see run links in [UC-17](uc-17-manage-training-offering.md).

## Out of scope

- Creating Teams meetings automatically (MS Graph), an embedded video call (e.g. Jitsi), attendance tracking.
