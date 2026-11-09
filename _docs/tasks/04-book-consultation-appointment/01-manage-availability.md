# 01 Manage availability

| | |
|---|---|
| Increment | [04 Book a consultation appointment](../../specs/04-book-consultation-appointment.md) |
| Type | AFK |
| Blocked by | – |
| Status | Done |

## Goal

An expert opens **Availability**, selects dates in a month calendar, sets their daily time ranges and saves. Only they see and change their own availability. This task builds the `availability` feature from the schema to the page: the first data an expert owns, local dates and times in the API, the expert's lock, and the test clock.

## What to build

Read first: [UC-13](../../use-cases/uc-13-manage-availability.md), [D-09](../../decisions.md#consultation), [D-27](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), the spec's decisions 8, 9, 10, 13 and 19, and the plan's [packages and names](README.md#packages-and-names), [schema](README.md#schema), [API](README.md#api), [availability](README.md#availability), [availability page](README.md#availability-page), [dates](README.md#dates) and [tests](README.md#tests) sections. Task 03 of [increment 02](../02-manage-consultation-offerings/03-create-and-edit-consultation-offering.md) is the pattern for a form with a list of rows and its 400s.

Work in this order, test first:

1. **Test clock:** `TestClock` and its test configuration; `@IntegrationTest` imports it and resets it after each test ([tests](README.md#tests)). Check that the purchase tests of increment 03 still pass on the system clock.
2. **A failing REST test:** with the clock at Mon 9 Nov 2026, 15:00 (Zurich), Expert 2 saves 17 and 18 Nov with 09:00–12:00 and 13:00–17:00; `PUT /api/expert/availability` answers 200 with them, and `GET` answers the same.
3. **Schema:** `V7__create_availability.sql`, as the plan's schema section says, with the `updated_at` triggers.
4. **Domain**, test first: `BusinessTimeZone` in `domain.shared` (and `ClockConfig` uses it); `TimeRange` in `domain.availability.value`; `Availability` with the overlap and touch rule and `requireNoDateBefore`.
5. **Ports and adapters:** `LockExpertPort` in `application.user.port`, implemented by `UserAdapter`; `LoadAvailabilityPort` and `ReplaceAvailabilityPort`, implemented by `AvailabilityAdapter` with the two entities. The replacement deletes with bulk JPQL, then persists (the plan says why).
6. **Use cases:** `GetAvailabilityUseCase` and `SaveAvailabilityUseCase` (lock, decide, write, answer), with today in Zurich from the `Clock`.
7. **API:**
   - `AvailabilityController` with `GET` and `PUT`, the request and the responses as the plan shows them, `ProblemResponses` per route;
   - `@ValidTimeRanges` and its validator, with the plan's paths and messages;
   - role matrix rows for both routes; drop the `GET /api/expert/probe` row;
   - regenerate `openapi.json`, then `npm run generate:api`.
8. **Test data:** `delete-test-data.sql` deletes every availability date and range before the test offerings.
9. **Frontend:**
   - shadcn `calendar`;
   - `lib/date.ts`: `formatLocalDate`, `todayInZurich`, `toCalendarDay`, `fromCalendarDay`, test first;
   - `features/availability/queries.ts`: `useAvailability`, `useSaveAvailability`;
   - `AvailabilityPage` with `AvailabilityForm`, as the plan's availability page section says, at `/expert/availability` under `RequireRole role="EXPERT"`;
   - **Availability** in the header for users with `EXPERT`, between **My purchases** and **Consultations**.
10. **Docs:** [frontend.md](../../architecture/frontend.md) gets the new `lib/date.ts` functions, the calendar's time zone, and the header's order; [backend.md](../../architecture/backend.md#testing) gets the test clock and drops `TimeRange` from its shared examples; [UC-13](../../use-cases/uc-13-manage-availability.md) already has the planning's rules.

## Acceptance criteria

- [x] On the dev server, Expert 2 opens **Availability** from the header, selects two future dates, adds 09:00–12:00 and 13:00–17:00, saves and sees "Saved"; after a reload the page shows the same (Playwright).
- [x] Days before today can't be selected in the calendar; today can.
- [x] A range 12:00–09:00 shows "must be after the start" next to its end; the ranges 09:00–12:00 and 11:00–14:00, and 09:00–12:00 and 12:00–15:00, show "must not overlap or touch another time range" next to the later one's start; nothing is saved (REST and Playwright).
- [x] A request with a date before today answers 400 with the plan's `detail`, and nothing is saved.
- [x] Saving keeps the dates before today and replaces those from today on (REST test with the clock moved forward a day).
- [x] An empty list of ranges, and an empty list of dates, are saved and answered as empty.
- [x] Expert 1 and Expert 2 each see only their own availability.
- [x] `TimeRange`, `Availability` and the new `lib/date.ts` functions have their tests; `lib/date.ts`'s in summer and winter time and around midnight UTC (Vitest).
- [x] `openapi.json` and `schema.d.ts` have both routes; the role matrix has both rows and no expert probe; every check passes.

## Tests

- Domain: `TimeRange` (start before end), `Availability` (overlap, touch, order, `requireNoDateBefore` with today allowed)
- REST: the happy path of both routes; the 400s above; replacing from today on; empty lists; a date named twice counts once
- Authorization: `RoleMatrixTest` rows; Expert 2 doesn't see Expert 1's availability
- Frontend: `lib/date.ts`

## Out of scope

- The slot rule: [02](02-bookable-slots.md). Booking: [03](03-book-appointment.md).
- A concurrency test for two saves at once. The save takes the expert's lock through `LockExpertPort`, whose lock [05](05-booking-races.md) proves for booking; two saves of one expert only meet in two tabs, and backend.md's list of locked races doesn't name them.
- Different ranges per date or weekday, recurring patterns, calendar sync: UC-13's out of scope.

## Notes

- **The selected dates as text** below the calendar ("Selected: 17 Nov 2026, 18 Nov 2026", or "No dates selected."), with `formatLocalDate`: the calendar shows one month, so dates in other months were invisible. The plan's [availability page](README.md#availability-page) says so now.
- **`Field`** (label, control, the backend's message) moved from `ConsultationOfferingForm` to `components/Field.tsx`, so both forms use it instead of a copy. [frontend.md](../../architecture/frontend.md) names it.
- **Local times** come out of Jackson as `09:00:00`; the form cuts them to `HH:mm`, as the plan expected. An empty time input sends `""`, which the API reads as `null`: "must not be null" next to the field (REST test).
- **Writing the availability** replaces the rows with bulk deletes, unlike backend.md's rule that child rows are written as a difference. [backend.md](../../architecture/backend.md#persistence) now names it as the exception, with the plan's reason.
- **`OpenApiContractTest`** also checks the `PUT`'s answers (200, 400, 415).
- **The shadcn CLI** offered to overwrite `components/ui/button.tsx` when adding `calendar`; it was kept as it is.
- **The bundle** is 564 kB (main: 512 kB); Vite's 500 kB warning was there before.
- **For later, not changed:** the calendar's weeks start on Sunday (react-day-picker's default for the browser's `en-US`), and a native time input shows 12-hour times in an English (US) browser; the values are 24-hour `HH:mm` either way.
- The spec's status is **In progress**, as the first task started.
