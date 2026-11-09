# 04 Book a consultation appointment

| | |
|---|---|
| Status | Ready |
| Use cases | [UC-13](../use-cases/uc-13-manage-availability.md), all steps; [UC-06](../use-cases/uc-06-book-consultation-appointment.md), all steps, with busy time from appointments only (decision 14); [UC-05](../use-cases/uc-05-view-my-purchases.md), the assigned expert, the appointments and **Book appointment** on the case detail, and the Upcoming and Past groups on **My purchases** (decision 6); [UC-03](../use-cases/uc-03-purchase-consultation-package.md), step 7's hint "Book your first session" |
| Tasks | [tasks/04-book-consultation-appointment](../tasks/04-book-consultation-appointment/README.md) |

## Why

After increment 03, a customer can buy a package and gets a case with session credits, but there is nothing to spend them on: the case detail shows "Not assigned yet" and "3 of 3 open", and that never changes. This increment closes that loop. An expert says when they're available, and a customer turns a credit into an appointment.

Booking is what most remaining use cases build on: cancelling an appointment or a case (UC-07, UC-08, whose refund rule looks at every appointment's deadline), the call link (UC-10), the expert's cases (UC-11), notes (UC-15) and the admin's case view (UC-18) all need an assigned expert or an appointment. It also brings what those use cases will reuse:

- the platform's richest rule so far, the bookable slot ([D-10](../decisions.md#consultation)), as a domain class with extensive unit tests, including the days of the summer-time changes;
- local dates and times in the API for the first time (availability), next to instants (appointments), and both shown in `Europe/Zurich`;
- the first routes under `/api/expert`, and an expert as the owner of their data (availability);
- the two locked races that [backend.md](../architecture/backend.md#testing) still owes: an expert's slot (two customers) and a case's credits (two tabs), with the lock order case → expert ([D-27](../decisions.md#architecture));
- derived states that depend on the time: a consumed appointment and a completed case ([D-11](../decisions.md#consultation), [D-12](../decisions.md#consultation)).

UC-13 comes in the same increment: without it, availability would have to come from seed data, whose dates go stale in a long-lived development database, and booking would have no real input. Cancelling (UC-07, UC-08) gets an increment of its own, because both cancellations share the deadline rule. Trainings (UC-17) come later, and their runs then add busy time to the slot rule.

## What changes

- An expert opens **Availability**, selects dates in a month calendar, sets their daily time ranges and saves. Bookings follow at once; removing a date never cancels an appointment.
- The case detail has **Book appointment**, and the hint "Book your first session" until the first booking.
- On the first booking, the customer picks their expert from the offering's eligible experts. That expert stays the case's expert.
- The customer sees only the dates and times that really fit: inside the expert's availability, free of the expert's other appointments, and at least 24 hours ahead.
- A confirmation states the expert, the date, the time, the duration and the free-cancellation deadline.
- If someone else took the time in the meantime, the booking is refused, and the customer picks again.
- The case detail shows the expert, the upcoming and the past appointments, and the open credits that are left.
- **My purchases** groups the cases into Upcoming, Needs booking and Past.

## User stories

1. As an expert, I want to select the dates I'm available and my daily hours, so that customers can book me only then.
2. As an expert, I want to change my availability without cancelling what's booked, so that I can plan ahead safely.
3. As a customer, I want to pick my expert on the first booking from the offering's experts, so that I work with the person who suits me.
4. As a customer, I want to see only times that really work, so that I don't pick a time that then fails.
5. As a customer, I want to confirm the expert, date, time and free-cancellation deadline before I book, so that I don't book by mistake.
6. As a customer, I want to book several sessions ahead, up to my open credits, so that I can plan a series.
7. As a customer, I want to be told when someone else took my time, so that I can pick another.
8. As a customer, I want **My purchases** and the case detail to show what's next and what I have left, so that I keep track.
9. As an expert who is also a customer, I want to book myself, so that I can try the flow ([D-06](../decisions.md#catalog)).
10. As a developer, I want one finished example of a two-row lock and of local dates and times in the API, so that later use cases follow one pattern.

## Requirements

In the scenarios, Career Coaching has 60-minute sessions and a 24-hour cancellation deadline, and both experts are eligible, as seeded. "Now" is Mon 9 Nov 2026, 15:00 (Zurich time), unless a scenario says otherwise; tests fix it with the `Clock`.

### Requirement: Manage availability

UC-13 steps 1–5, with its rules. Dates are Zurich calendar dates, and time ranges are Zurich wall-clock times (decision 11).

#### Scenario: Set availability

- **GIVEN** Expert 2 is logged in
- **WHEN** they open **Availability**, select 17 and 18 Nov 2026, set the time ranges 09:00–12:00 and 13:00–17:00, and save
- **THEN** the page says "Saved", and after a reload it shows the same dates and ranges
- **AND** `PUT /api/expert/availability` answered 200

#### Scenario: Invalid time ranges

- **WHEN** the expert saves a range 12:00–09:00, or the ranges 09:00–12:00 and 11:00–14:00, or 09:00–12:00 and 12:00–15:00 (decision 9)
- **THEN** the response is 400, the page shows a message next to each invalid range, and nothing is saved

#### Scenario: Past dates can't be changed

- **WHEN** the expert opens **Availability**
- **THEN** the days before today (Zurich) can't be toggled, and today can (decision 9)
- **AND** a request with a date before today answers 400

#### Scenario: Removing availability keeps appointments

- **GIVEN** Customer 1 booked Expert 2 on 17 Nov 2026, 10:00–11:00
- **WHEN** Expert 2 removes 17 Nov, or narrows the ranges to 13:00–17:00, and saves
- **THEN** the appointment is still booked and shown on Customer 1's case
- **AND** no new booking on 17 Nov is offered

#### Scenario: Only their own availability

- **GIVEN** Expert 1 and Expert 2 each saved different dates
- **WHEN** each opens **Availability**
- **THEN** each sees only their own dates and ranges

### Requirement: Choose the expert on the first booking

UC-06 step 2 and its rules on eligible and assigned experts.

#### Scenario: The expert picker

- **GIVEN** Customer 1 has a case of Career Coaching without an assigned expert
- **WHEN** they click **Book appointment**
- **THEN** the dialog lists Expert 1 and Expert 2, sorted by display name, each with the name, the bio and the placeholder photo the shop shows (decision 3)

#### Scenario: An expert books themself

- **WHEN** Expert 1, who is also a customer, books on their own case and picks Expert 1
- **THEN** the booking succeeds, and Expert 1 is the case's expert ([D-06](../decisions.md#catalog))

#### Scenario: No eligible experts

- **GIVEN** a case of an offering that has no eligible experts
- **WHEN** the customer clicks **Book appointment**
- **THEN** the dialog says "No experts are available for this offering yet. Please try again later." (decision 2)

#### Scenario: The expert is no longer eligible

- **GIVEN** the customer picked Expert 2 on a case without an assigned expert and is at the confirmation
- **WHEN** an admin removes Expert 2 from the offering's eligible experts, and the customer then confirms
- **THEN** the response is 409 with code `EXPERT_NOT_ELIGIBLE`, the dialog shows the message and the current experts, and nothing is booked

#### Scenario: The assigned expert stays

- **GIVEN** Customer 1's case has Expert 2 as its expert
- **WHEN** an admin removes Expert 2 from the offering's eligible experts, and Customer 1 then clicks **Book appointment**
- **THEN** the dialog skips the expert step and offers Expert 2's times, and the booking succeeds

#### Scenario: Two first bookings with different experts

- **GIVEN** a case without an assigned expert is open in two tabs
- **WHEN** the customer books Expert 1 in one tab and then Expert 2 in the other
- **THEN** the second answers 409 with code `EXPERT_CHANGED`, the dialog reloads the case and continues with Expert 1, and all of the case's appointments are with Expert 1 (decision 12)

### Requirement: Bookable slots

UC-06 steps 3–4 and the bookable slot of [D-10](../decisions.md#consultation): a start time on the 30-minute grid, the whole session inside one availability range, no overlap with the expert's busy time, and at least 24 hours from now. Busy time is the expert's booked appointments, of every case (decision 14).

#### Scenario: Grid and fit

- **GIVEN** Expert 2 is available on 17 Nov 2026, 09:00–12:00
- **WHEN** the customer picks 17 Nov
- **THEN** the dialog offers 09:00–10:00, 09:30–10:30, 10:00–11:00, 10:30–11:30 and 11:00–12:00, each as start–end, with "(Zurich time)"

#### Scenario: A session doesn't span two ranges

- **GIVEN** Expert 2 is available on 17 Nov 2026, 09:00–12:00 and 13:00–17:00
- **THEN** 11:30–12:30 and 12:30–13:30 aren't offered, and 13:00–14:00 is

#### Scenario: 24 hours' notice

- **GIVEN** now is Mon 16 Nov 2026, 10:15, and Expert 2 is available on 16 and 17 Nov, 09:00–12:00
- **THEN** 16 Nov isn't offered, and 17 Nov offers 10:30–11:30 and 11:00–12:00 only

#### Scenario: The expert's other appointments

- **GIVEN** Customer 2 booked Expert 2 on 17 Nov 2026, 10:00–11:00
- **WHEN** Customer 1 picks Expert 2 and 17 Nov
- **THEN** 09:30–10:30, 10:00–11:00 and 10:30–11:30 aren't offered, and 09:00–10:00 and 11:00–12:00 are (no buffer between sessions)
- **AND** the response holds only the free start and end times, nothing about Customer 2's appointment

#### Scenario: The summer-time change

- **GIVEN** Expert 2 is available on Sun 28 Mar 2027, 01:00–05:00, when the clocks jump from 02:00 to 03:00
- **THEN** no start time between 02:00 and 03:00 is offered, and 01:30 is offered as 01:30–03:30, a session of 60 real minutes (decision 11)
- **AND** with the same range on Sun 31 Oct 2027, when 02:00–03:00 happens twice, the start times 02:00 and 02:30 aren't offered

#### Scenario: No free times

- **GIVEN** Expert 2 has no future availability, or every slot is taken
- **WHEN** the customer picks Expert 2
- **THEN** the dialog says "No free times available at the moment."

### Requirement: Book an appointment

UC-06 steps 1 and 5–8 and its error flows. The booking dialog has steps, like the buy dialog: expert (first booking only), date, time, confirmation (decision 4).

#### Scenario: The first booking

- **GIVEN** Customer 1 has a Career Coaching case with 3 open credits and no expert
- **WHEN** they click **Book appointment**, pick Expert 2, 17 Nov 2026 and 10:00–11:00, and confirm
- **THEN** the dialog closes, and the case detail shows Expert 2 as the expert, 2 of 3 credits open, and the appointment 17 Nov 2026, 10:00–11:00 under Upcoming
- **AND** `POST /api/customer/consultation-cases/{id}/appointments` answered 201

#### Scenario: The confirmation states the terms

- **WHEN** the customer picks Expert 2, 17 Nov 2026 and 10:00–11:00, and continues
- **THEN** the dialog asks "Book Expert 2 on 17 Nov 2026, 10:00–11:00 (60 min, Zurich time)?" and says "Free cancellation until 16 Nov 2026, 10:00." (decision 7)

#### Scenario: The customer changes their mind

- **WHEN** the customer closes the dialog, at any step
- **THEN** nothing is booked, and the case has no expert if it had none

#### Scenario: A later booking

- **GIVEN** Customer 1's case has Expert 2 and one appointment
- **WHEN** they click **Book appointment**
- **THEN** the dialog starts with the date, for Expert 2

#### Scenario: Up to the open credits

- **GIVEN** Customer 1's case has 3 credits
- **WHEN** they book three future appointments
- **THEN** the case shows 0 of 3 credits open, and **Book appointment** is disabled
- **AND** a booking request for that case answers 409 with code `NO_OPEN_CREDITS`

#### Scenario: The time was taken

- **GIVEN** Customer 1 and Customer 2 are both at the confirmation for Expert 2 on 17 Nov 2026, 10:00–11:00
- **WHEN** Customer 2 confirms, and then Customer 1 confirms
- **THEN** Customer 1's response is 409 with code `SLOT_NOT_AVAILABLE`, the dialog says "This time is no longer available." and goes back to the date step with the reloaded times, and nothing is booked for Customer 1

#### Scenario: A case of an archived offering

- **GIVEN** an admin archived the offering of Customer 1's case
- **WHEN** Customer 1 books on the case
- **THEN** the booking succeeds: existing purchases keep working ([D-07](../decisions.md#catalog))

#### Scenario: Invalid request

- **WHEN** the booking request has no start time or no expert
- **THEN** the response is 400 with a `ProblemDetail` that names each invalid field
- **AND** a start time that isn't a bookable slot answers 409 with code `SLOT_NOT_AVAILABLE`

### Requirement: Concurrent bookings

[D-27](../decisions.md#architecture): the booking locks the case, then the expert, and re-checks every rule under the locks (UC-06 step 7).

#### Scenario: Two customers, one slot

- **WHEN** Customer 1 and Customer 2 book Expert 2 on 17 Nov 2026, 10:00–11:00 at the same moment
- **THEN** exactly one succeeds, the other gets 409 `SLOT_NOT_AVAILABLE`, and Expert 2 has one appointment at that time

#### Scenario: Two tabs, one credit

- **GIVEN** a case with one open credit
- **WHEN** the customer books two different times on it at the same moment
- **THEN** exactly one succeeds, the other gets 409 `NO_OPEN_CREDITS`, and the case never has more booked appointments than credits

### Requirement: The case detail and My purchases

UC-05's case detail and landing page, as far as cases have data now: no cancellations, no notes, no call link.

#### Scenario: Book your first session

- **GIVEN** a case without appointments
- **WHEN** the customer opens it, also right after buying it
- **THEN** the case detail shows "Book your first session" next to **Book appointment**

#### Scenario: The case detail

- **GIVEN** Customer 1's case has Expert 2, one past and one upcoming appointment
- **WHEN** they open it
- **THEN** it shows Expert 2's name, bio and placeholder photo, the credits, the upcoming appointment with its date and start–end, and the past appointment as consumed, each in Zurich time
- **AND** the past appointment still uses its credit: 1 of 3 credits open

#### Scenario: A running appointment

- **GIVEN** Customer 1's case has 3 credits and one appointment, on 17 Nov 2026, 10:00–11:00, and now is 17 Nov 2026, 10:30
- **WHEN** they open the case, and **My purchases**
- **THEN** the case detail shows the appointment under the upcoming ones, marked "Now", with 2 of 3 credits open, and **My purchases** shows the case under **Upcoming** with "Now: 17 Nov 2026, 10:00–11:00" (decision 21)
- **AND** from 11:00 the appointment is a past one, consumed, and the case is under **Needs booking**

#### Scenario: The groups on My purchases

- **GIVEN** Customer 1 has a case with an upcoming appointment, a case with open credits and nothing upcoming, and a case whose three appointments are all past
- **WHEN** they open **My purchases**
- **THEN** the first is under **Upcoming**, the second under **Needs booking**, and the third under **Past**, which is collapsed (decision 6)
- **AND** Upcoming is sorted by the next appointment, Needs booking by purchase time, and Past by the last appointment, newest first

#### Scenario: The case card

- **WHEN** a case has an expert and an upcoming appointment
- **THEN** its card shows the expert's name, the next appointment's date and time, and the open and total credits

### Requirement: Only the owner books, and only the expert manages their availability

[Security](../architecture/security.md#ownership-in-use-cases): ownership is a scoped query, and a case of another customer is a 404, like an unknown one. The availability routes have no id: they're always the current expert's.

#### Scenario: Another customer's case

- **WHEN** Customer 2 asks for the bookable slots of Customer 1's case, or books on it
- **THEN** the response is 404, and nothing is booked

#### Scenario: Routes by role

- **WHEN** a user without `EXPERT` calls `GET` or `PUT /api/expert/availability`, or opens `/expert/availability`
- **THEN** the route answers 403 (401 without a login), and the page shows "Page not found"
- **AND** the booking routes under `/api/customer` answer 403 without `CUSTOMER`

#### Scenario: The header

- **WHEN** Expert 1 or Expert 2 logs in
- **THEN** the header shows **Availability**
- **AND** the landing pages stay as they are until the expert's **My cases** exists (UC-11)

## Decisions

Agreed with the product owner while planning (2026-11-09): the scope, UC-13 and UC-06 together, with UC-07 and UC-08 in the next increment; then decisions 1 to 15, each as recommended. The ones marked *planning* were taken while writing the [plan](../tasks/04-book-consultation-appointment/README.md), following the docs; object to them before the first task starts.

1. **Experts don't see their bookings yet.** Their cases and appointments are UC-11, which isn't in this increment; until then, an expert sees a booking only as a time that's gone. UC-11 comes with the expert's side (UC-11, UC-14, UC-10, UC-15) in a later increment.
2. **An offering without eligible experts** (left to this increment by [spec 03](03-buy-consultation-package.md#decisions), decision 7): **Book appointment** stays enabled, and the dialog says "No experts are available for this offering yet. Please try again later." The customer bought in good faith, and the dialog explains itself. [UC-06](../use-cases/uc-06-book-consultation-appointment.md) now says so.
3. **Expert photos:** `expert_profile` has no photo until UC-14, so the picker and the case detail show the same `ImagePlaceholder` as the shop's offering detail.
4. **The booking flow is a dialog with steps**, opened from the case detail like the buy dialog: expert (first booking only), date, time, confirmation. The date step is a month calendar where only dates with bookable slots can be picked, opening on the month of the first one; the time step lists that date's slots.
5. **One request loads all slots:** the expert's bookable slots for the case, from now on, with no horizon. Availability is a list of selected dates, so the result stays small at PoC scale, and the calendar needs no request per month.
6. **The Past group arrives now.** Without cancellations, a case is `COMPLETED` once its credits are booked and every appointment has started ([glossary](../glossary.md#purchases)). It's neither Upcoming nor Needs booking, so without Past it would vanish from **My purchases**. Cancelled cases join Past with UC-08. This replaces spec 03's expectation (its decision 1) that Past comes with UC-08.
7. **The confirmation states the free-cancellation deadline**, as UC-06 step 5 says, although cancelling comes with the next increment. It's a term of the purchase, true from the moment of booking, and no button promises anything.
8. **No seed availability.** Seed dates relative to the migration date are fixed when the database is first migrated, so they go stale in a database that's kept for weeks; and tests have to set the seeded experts' availability anyway, because they're the only experts. Tests set it through the API, the test cleanup deletes all availability and appointments as it deletes all cases, and the demo starts with the experts setting theirs. [backend.md](../architecture/backend.md#persistence)'s seed content now says so.
9. **Availability input rules** beyond UC-13's: a date before today (Zurich) is past and can't be changed; today can. The save replaces the expert's dates from today on and never touches earlier ones, and the page shows dates from today on. Time ranges may start and end at any minute (the grid does the rest) and may be empty (then nothing is bookable). Ranges that touch, such as 09:00–12:00 and 12:00–15:00, are refused like overlapping ones: a session must fit inside one range, so 11:30–12:30 would silently be missing, and one range 09:00–15:00 says the same. [UC-13](../use-cases/uc-13-manage-availability.md) now says so.
10. **Availability in the header** for every user with `EXPERT`, between **My purchases** and **Consultations**. The landing pages don't change: no seeded user is only an expert, and an expert's landing page will be **My cases** (UC-11).
11. **The days of the summer-time changes.** Ranges and the grid are wall-clock times, but a session lasts its real duration. A start time is offered only if it exists exactly once on that date: in spring, 02:00–02:59 doesn't exist; in autumn, 02:00–02:59 happens twice and isn't offered. A session must end, as an instant, at or before the end of its range, so 01:30 on 28 Mar 2027 is the session 01:30–03:30, which lasts 60 minutes. Nobody is expected to coach at 2 a.m.; the rule only has to be correct and simple. [D-10](../decisions.md#consultation) and UC-06 now say so.
12. **The booking request names the expert**, the one the customer saw: the one they picked, or the case's. If the case has no expert, the server checks that the named one is eligible (409 `EXPERT_NOT_ELIGIBLE`) and assigns them; if it has one and it's another, the answer is 409 `EXPERT_CHANGED`. As with the confirmed price in spec 03 (decision 3), the server never books something the customer didn't see. UC-06 now says so.
13. **Saving availability locks the expert's row**, the same lock a booking takes after the case. Two saves at once, from two tabs, would otherwise interleave their deletes and inserts and fail with a 500, and a booking always sees either the old or the new availability. A booking just before a removal is still valid: removing availability never cancels anything. [D-27](../decisions.md#architecture) now says so.
14. **Busy time is the expert's booked appointments.** Runs don't exist yet; the trainings increment adds the time slots of the expert's runs to the same port, and assigning an expert to a run checks the appointments ([D-09](../decisions.md#consultation)).
15. **Only what exists is stored**, as in spec 03 (decision 12): the case gets its assigned expert, and the appointment its start, end and status `BOOKED`. The case's status and the appointment's `CANCELLED` and `creditRefunded` come with UC-07 and UC-08. The status column exists from the start so that every query of appointments already filters by `BOOKED`, and UC-07 only adds a value. Open credits are the session count minus the booked appointments, until forfeited credits (UC-07) and credit adjustments (UC-18) exist.
16. **The picker's experts come from the case** (*planning*), `GET /api/customer/consultation-cases/{id}/eligible-experts`, not from the shop: an archived offering's case can still book, and the shop doesn't show archived offerings.
17. **The booking answers 201 with the new appointment, without a `Location`** (*planning*): an appointment has no route of its own; it's part of its case, which the page reloads.
18. **The server computes what the dialog and the pages show** (*planning*), as [frontend.md](../architecture/frontend.md#dates-times-and-money) asks: each slot carries its free-cancellation time, the case detail carries `canBook`, and the list carries each case's group, in the order of **My purchases**. The frontend never computes a deadline, a credit or a group.
19. **A settable `Clock` in integration tests** (*planning*): consumed appointments, completed cases and the 24 hours' notice need a time the test chooses. `@IntegrationTest` provides a `TestClock` and resets it after each test; until a test sets it, it's the system clock.
20. **Executed as in [D-33](../decisions.md#scope-and-docs)** (*planning*): tasks 01, 02 and 03 one after the other, each building on the one before; then 04 and 05 in parallel, each in its own git worktree.
21. **An appointment is upcoming until it ends** (decided 2026-11-09, while drafting [spec 06](06-expert-side-of-consultations.md), question 2). It's consumed from its start, for the credits and, from increment 05, for cancelling. But the case detail lists it among the upcoming appointments, marked "Now" while it runs, and **My purchases** keeps its case under **Upcoming** until it ends. During a session the customer finds it where they expect the next one, and UC-10's **Join** (increment 06) stays within reach until the end. It's in this increment so that the groups aren't built twice. The [glossary](../glossary.md#consultation), [D-17](../decisions.md#views-and-calls), UC-05, UC-10 and UC-11 now say so.

## Out of scope

- Cancelling an appointment, and the forfeited credit: UC-07, next increment.
- Cancelling a case, and cancelled cases under Past: UC-08, next increment.
- The **Join** button and the Teams link: UC-10, with the expert profile's link (UC-14).
- Expert photos and editing the expert profile: UC-14 (decision 3).
- The expert's cases and appointments, and the expert's landing page: UC-11 (decision 1). Notes: UC-15.
- Busy time from training runs: with the trainings (UC-17) (decision 14).
- The admin's case list and credit adjustments: UC-18.
- Rescheduling, changing the expert, recurring appointments, an approval step by the expert: UC-06's out of scope.
- Time ranges per date or weekday, recurring weekly patterns, calendar sync: UC-13's out of scope.
- Email notifications and an availability check before purchase: [future list](../use-cases/README.md#future-not-planned).

## Demo

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=poc
```

```bash
cd frontend && npm run dev
```

"Day 2" and "day 3" are the dates two and three days from today.

1. Open `http://localhost:5173`, log in as Expert 2: the header shows **Availability**. Open it, select day 2 and day 3, set 09:00–12:00 and 13:00–17:00, and save. Try a range 12:00–09:00: a message next to it, nothing saved. Days before today can't be toggled.
2. Log out, log in as Expert 1, and make day 2 available, 09:00–12:00.
3. Log out, log in as Customer 1, buy 3 sessions of Career Coaching: the case detail says "Book your first session".
4. Click **Book appointment**: Expert 1 and Expert 2, with bio and placeholder photo. Pick Expert 2: only day 2 and day 3 can be picked. Pick day 2: 09:00–10:00 to 11:00–12:00, and 13:00–14:00 to 16:00–17:00. Pick 10:00–11:00 and continue: the confirmation names Expert 2, the date, 10:00–11:00 (60 min) and the free-cancellation time one day earlier. Confirm: Expert 2, 2 of 3 credits open, the appointment under Upcoming.
5. Click **Book appointment** again: no expert step. Day 2 no longer offers 09:30–10:30, 10:00–11:00 or 10:30–11:30. Close the dialog. **My purchases** shows the case under **Upcoming**, with Expert 2 and the appointment.
6. In a private window, log in as Customer 2, buy Career Coaching, book Expert 2 on day 2: 10:00–11:00 isn't offered. Go to the confirmation of 11:00–12:00 and leave it open. As Customer 1, book 11:00–12:00 with Expert 2. As Customer 2, confirm: "This time is no longer available.", and the times reload without 11:00–12:00.
7. As Customer 2, paste the URL of Customer 1's case: "Case not found".
8. In the private window, log out, log in as Expert 2, remove day 2 from the availability and save. As Customer 1, reload the case: both appointments are still there.
9. In the private window, log out, log in as Expert 1, buy Career Coaching and book yourself, Expert 1, on day 2.
10. In the private window, log out, log in as Admin 1, open Career Coaching and remove Expert 2 from its eligible experts. Save. As Customer 1, click **Book appointment**: it still offers Expert 2's day 3. Put Expert 2 back.

Completed cases, consumed appointments and the Past group need time to pass, so tests with a fixed `Clock` cover them instead of the demo.

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

11. Open `http://localhost:8080`, log in as Customer 2, open the case from step 6, which has no expert yet, and book Expert 1 on day 2.

## Risks

| Risk | Mitigation |
|---|---|
| The slot rule gets the time zone wrong: wrong times around midnight UTC or on the days of the summer-time changes. | One domain class computes slots from wall-clock ranges, with unit tests in summer and winter time and on both change days (decision 11). The frontend groups and formats slots only through `lib/date.ts`, with Vitest tests around midnight UTC. |
| An expert is booked twice at the same time from two cases. | The booking locks the expert's row after the case's ([D-27](../decisions.md#architecture)) and re-checks the slot under the lock. A concurrency test makes the race certain, as `ConsultationOfferingConcurrencyTest` does, and fails without the expert lock. |
| A case gets more appointments than credits from two tabs. | The booking locks the case first. A second concurrency test, which fails without the case lock. |
| Rules that depend on time passing (consumed, completed, Past) can't be reached through the API: a booking is at least 24 hours ahead. | Tests set the time with a `Clock` they control: book, then move the clock past the appointment (decision 19). |
| Tests now change the seeded experts' availability and add appointments to them. | No seed availability (decision 8). The cleanup script deletes all appointments and availability before the cases. |
| An admin removes an expert from the offering while a customer's first booking picks them, and the booking doesn't lock the offering. | The booking checks eligibility when it runs. One that passes just before the removal is valid: removing eligibility never affects an assigned expert (UC-06). No offering lock, so bookings never wait for an admin's edit. |
| Local dates and times (availability) and instants (appointments, slots) get mixed up between the backend and the frontend. | The API keeps them apart by type: availability as `LocalDate` and `LocalTime` without offset, slots and appointments as instants with offset ([backend.md](../architecture/backend.md#rest-conventions)). |
| The month calendar is a new kind of UI component for both pages. | shadcn's `calendar` supports picking several dates (availability) and disabling dates (booking). One component, added with the CLI. |
| The increment is larger than 03: two use cases, two roles and two races. | The [plan](../tasks/04-book-consultation-appointment/README.md) cuts it into five tasks: availability, the slot rule, booking with the case detail, My purchases, and the races. The slot rule is a domain task of its own, and booking, the largest, sets the pattern and gets the most careful review. |
