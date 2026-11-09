# UC-13 Manage availability

| | |
|---|---|
| Actor | expert |
| Scope | PoC |
| Related | [UC-06](uc-06-book-consultation-appointment.md), [UC-17](uc-17-manage-training-offering.md) |

## Goal

The expert defines when customers can book consultation appointments with them.

## Preconditions

- The user has `EXPERT`.

## Main flow

1. The expert opens **Availability**.
2. The system shows a month calendar with the currently selected dates, plus the expert's daily time ranges (e.g. 09:00–12:00, 13:00–17:00).
3. The expert toggles dates on or off and edits the daily time ranges.
4. The expert saves.
5. The system stores the selected dates from today on and the time ranges. The changes apply to new bookings immediately.

## Alternative and error flows

- **Invalid time ranges** (start ≥ end, or ranges that overlap or touch): 400 with a message for each field.
- **A date before today:** 400. The calendar doesn't let the expert select one.

## Business rules

- **One** set of daily time ranges applies to **all** selected dates (D-09).
- Time ranges are local Zurich times.
- Past dates, the ones before today (Zurich), can't be changed; today can. The save replaces the expert's dates from today on and never touches earlier ones, and the page shows dates from today on (decided 2026-11-09).
- Time ranges may start and end at any minute; bookable start times still follow the 30-minute grid ([UC-06](uc-06-book-consultation-appointment.md)). Ranges that touch, such as 09:00–12:00 and 12:00–15:00, are refused like overlapping ones: a session must fit inside one range, and one range 09:00–15:00 says the same. The set of ranges may be empty; then nothing is bookable (decided 2026-11-09).
- Removing a date or narrowing the time ranges **never** cancels existing appointments. It only affects new bookings.
- Availability is only for consultations. Assignments to training runs ignore it ([UC-17](uc-17-manage-training-offering.md)), but those time slots count as busy time for bookings.

## Visibility and authorization

- An expert only sees and edits their own availability.
- Customers never see availability directly, only the bookable slots derived from it ([UC-06](uc-06-book-consultation-appointment.md)).

## Out of scope

- Different time ranges per date or weekday, recurring weekly patterns, calendar sync (Outlook/Google).
