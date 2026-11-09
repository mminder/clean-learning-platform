# 02 Bookable slots

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | [01](01-manage-availability.md) |
| Status | Todo |

## Goal

The domain knows every bookable slot of an expert: the start times on the 30-minute grid where a whole session fits inside the expert's availability, overlaps none of their busy time, and starts at least 24 hours from now, also on the days of the summer-time changes. This task is the slot rule alone, in the domain, with extensive unit tests; [03](03-book-appointment.md) uses it to show and to book.

## What to build

Read first: the bookable slot in [UC-06](../../use-cases/uc-06-book-consultation-appointment.md#business-rules) and the [glossary](../../glossary.md#consultation), [D-10](../../decisions.md#consultation), the spec's decisions 11 and 14, its [bookable slots](../../specs/04-book-consultation-appointment.md#requirement-bookable-slots) scenarios, and the plan's [bookable slots](README.md#bookable-slots) section.

Work test first, one rule at a time:

1. **`BusyTime`** in `domain.consultation.value`: start before end.
2. **`SlotFinder`** in `domain.consultation`, with the constants `GRID` and `MINIMUM_NOTICE` (D-10: in one domain class), and `startTimes(SessionDuration, Instant now)`:
   - the grid and the fit inside one range, for 30, 60 and 90 minutes;
   - a range that doesn't start or end on the grid (09:15–11:45);
   - two ranges on one date, and a session that would span the break between them;
   - several dates, in order;
   - the 24 hours' notice, exactly at the limit and one minute past it;
   - busy time: overlapping, touching at either end (offered), longer than a session, on another date;
   - the days of the summer-time changes: Sun 28 Mar 2027 (no start time from 02:00 to 02:59; 01:30 is 01:30–03:30, 60 real minutes) and Sun 31 Oct 2027 (02:00 and 02:30 aren't offered; a session around the change lasts its real duration);
   - no dates, no ranges, or everything busy: no start times.
3. **`isBookable(Instant start, SessionDuration, Instant now)`**: a start time of `startTimes`, and nothing else, also a start between the grid's times or a second off.

The spec's scenarios use Mon 9 Nov 2026, 15:00 (Zurich) as now; the tests write the instants with their Zurich time, so they read like the scenarios.

## Acceptance criteria

- [ ] Every scenario of the spec's [bookable slots](../../specs/04-book-consultation-appointment.md#requirement-bookable-slots) requirement holds as a domain test, from "Grid and fit" to "No free times".
- [ ] Both days of the summer-time changes are tested as the plan says.
- [ ] `isBookable` agrees with `startTimes` for every start time, and is false for a start a minute off the grid.
- [ ] `SlotFinder` and `BusyTime` have no Spring or JPA imports, and `LayerRulesTest` passes; every check passes.

## Tests

- Domain: `BusyTime`, `SlotFinder` (the list above)

## Out of scope

- Loading the availability and the busy time, the routes and the dialog: [03](03-book-appointment.md).
- The free-cancellation time of a slot: `CaseForBooking` adds it in 03.
- Busy time from training runs: with the trainings (spec decision 14).

## Notes

