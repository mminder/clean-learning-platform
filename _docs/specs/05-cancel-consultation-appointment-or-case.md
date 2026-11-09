# 05 Cancel a consultation appointment or case

| | |
|---|---|
| Status | Draft |
| Use cases | [UC-07](../use-cases/uc-07-cancel-consultation-appointment.md), all steps; [UC-08](../use-cases/uc-08-cancel-consultation-case.md), all steps, with the refund flag only; [UC-05](../use-cases/uc-05-view-my-purchases.md), **Cancel** on upcoming appointments, cancelled appointments with "credit refunded" or "credit forfeited", **Cancel case**, and cancelled cases under Past; [UC-06](../use-cases/uc-06-book-consultation-appointment.md), the error flow "The case was cancelled" |
| Tasks | Not planned yet: they're written once the open questions are answered |

## Why

After increment 04, a customer can buy a package and book appointments, but nothing can be undone: a booking they can't attend keeps its credit, and a purchase made by mistake stays a case for good. The booking confirmation already promises "Free cancellation until …" ([spec 04](04-book-consultation-appointment.md#decisions), decision 7), and nothing keeps that promise yet. This increment does.

Both cancellations share one rule, the cancellation deadline ([D-11](../decisions.md#consultation)), which is why they come together, as agreed while planning increment 04. They bring what later use cases reuse:

- the rule that decides "in time" or "late" with the server's clock, and the forfeited credit, which completes the open credits: `total − BOOKED − forfeited` ([glossary](../glossary.md#consultation));
- refund eligibility ([D-12](../decisions.md#consultation)), the platform's second rule with many cases after the bookable slot, as a domain class with extensive unit tests; cancelling an enrollment (UC-09, [D-16](../decisions.md#training)) mirrors it;
- the first stored status changes on data a customer owns: a case and an appointment become `CANCELLED`, and every view of a case so far learns to show them;
- a new race on the case's lock: a case cancelled while a booking on it runs.

The expert sees cancellations with the expert's side ([increment 06](06-expert-side-of-consultations.md)), the admin with UC-18.

## What changes

- Each upcoming appointment on the case detail shows until when it can be cancelled for free, and has **Cancel**.
- Cancelling in time returns the credit. Cancelling late warns first, with the deadline that passed, and forfeits the credit.
- A cancelled appointment frees the expert's time for other bookings. The case shows it as cancelled, with "credit refunded" or "credit forfeited".
- The case detail has **Cancel case**. Its confirmation says whether the purchase price would be refunded, and if not, why, and lists the appointments it cancels.
- A cancelled case can't be booked or cancelled again. It shows when it was cancelled and the refund outcome, and moves to Past on **My purchases**.
- No money moves: the refund is a flag on the case (PoC).

## User stories

1. As a customer, I want to cancel an appointment I can't attend, so that I get the credit back and can book another time.
2. As a customer, I want to see until when I can cancel for free, so that I decide in time.
3. As a customer, I want to be warned before a late cancellation forfeits my credit, so that I don't lose it by mistake.
4. As a customer, I want to cancel a package I bought by mistake, so that I'm refunded if I haven't used it.
5. As a customer, I want to know before I cancel a case whether I'd be refunded, and why not, so that I decide with the facts.
6. As a customer, I want to see my cancelled appointments and cases with their outcome, so that I keep track.
7. As another customer, I want a cancelled time to become bookable again, so that I can take it.
8. As a developer, I want refund eligibility as one tested domain rule, so that the enrollment's cancellation (UC-09) and the admin's case view (UC-18) follow one pattern.

## Requirements

In the scenarios, Career Coaching has 60-minute sessions and a 24-hour cancellation deadline, as seeded. Customer 1's case of 3 sessions for CHF 450.00 has Expert 2 as its expert and an appointment on Tue 17 Nov 2026, 10:00–11:00, which can be cancelled for free until Mon 16 Nov 2026, 10:00. "Now" is Sun 15 Nov 2026, 12:00, unless a scenario says otherwise; tests fix it with the `Clock`. All times are Zurich times.

### Requirement: Cancel an appointment

UC-07 with its rules. In time means `now < start − cancellation deadline`, with the deadline from the case's purchase terms, in real hours; the server decides at the moment of the request.

#### Scenario: Free cancellation on each appointment

- **WHEN** Customer 1 opens the case
- **THEN** the appointment on 17 Nov 2026, 10:00–11:00 says "Free cancellation until 16 Nov 2026, 10:00." and has **Cancel** (question 1)

#### Scenario: Cancel in time

- **WHEN** Customer 1 clicks **Cancel** on the appointment
- **THEN** the dialog asks "Cancel the appointment with Expert 2 on 17 Nov 2026, 10:00–11:00 (Zurich time)?" and says "The session credit will be returned to your case." (question 2)
- **WHEN** they confirm
- **THEN** the case shows 3 of 3 credits open, and the appointment as "Cancelled, credit refunded" (question 3)
- **AND** `POST /api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel` answered 200

#### Scenario: Cancel late

- **GIVEN** now is Mon 16 Nov 2026, 11:00
- **WHEN** Customer 1 clicks **Cancel** on the appointment
- **THEN** the dialog says "The free cancellation deadline passed on 16 Nov 2026, 10:00. If you cancel, the session credit is **not** refunded. Cancel anyway?"
- **WHEN** they confirm
- **THEN** the case shows 2 of 3 credits open, and the appointment as "Cancelled, credit forfeited"

#### Scenario: At the deadline

- **GIVEN** now is Mon 16 Nov 2026, 10:00 exactly
- **THEN** the cancellation is late: free cancellation ends at the deadline, as UC-07's `now < start − deadline` says

#### Scenario: The summer-time change

- **GIVEN** an appointment on Sun 28 Mar 2027, 10:00, the day the clocks jump from 02:00 to 03:00
- **THEN** it can be cancelled for free until Sat 27 Mar 2027, 09:00: 24 real hours earlier, as the booking confirmation said

#### Scenario: The deadline passes while the dialog is open

- **GIVEN** Customer 1 opened the dialog at 09:59 on 16 Nov 2026, and it says the credit will be returned
- **WHEN** they confirm at 10:01
- **THEN** the response is 409 with code `DEADLINE_PASSED`, nothing is cancelled, and the dialog shows the late warning to confirm again (question 4)

#### Scenario: The time is free again

- **GIVEN** Customer 1 cancelled the appointment on 17 Nov 2026, 10:00–11:00
- **WHEN** Customer 2 books Expert 2 on 17 Nov 2026
- **THEN** 10:00–11:00 is offered again

#### Scenario: The credit comes back to a full case

- **GIVEN** all 3 credits of Customer 1's case are booked, so **Book appointment** is disabled
- **WHEN** they cancel one appointment in time
- **THEN** 1 of 3 credits is open, and **Book appointment** is enabled

#### Scenario: A started appointment

- **GIVEN** now is Tue 17 Nov 2026, 10:00
- **THEN** the appointment is marked "Now" until 11:00 ([spec 04](04-book-consultation-appointment.md#decisions), decision 21) and has no **Cancel**
- **AND** a cancellation request answers 409 with code `APPOINTMENT_STARTED` and "This appointment can no longer be cancelled."

#### Scenario: Cancelled in another tab

- **GIVEN** the case is open in two tabs
- **WHEN** Customer 1 cancels the appointment in one, and then in the other
- **THEN** the second answers 409 with code `APPOINTMENT_CANCELLED` and "This appointment was already cancelled.", the dialog shows the message, and the case reloads

#### Scenario: The customer changes their mind

- **WHEN** Customer 1 closes the dialog, or clicks **Keep appointment**
- **THEN** nothing changes

#### Scenario: A case of an archived offering

- **GIVEN** an admin archived Career Coaching
- **THEN** Customer 1 can still cancel the appointment: existing purchases keep working ([D-07](../decisions.md#catalog))

### Requirement: Open credits count forfeited credits

The glossary's `open credits = total − BOOKED − forfeited`, where `BOOKED` includes the consumed appointments. A case is `COMPLETED` once it has no open credits and no future booked appointment, so a forfeited credit can complete a case.

#### Scenario: Credits after cancellations

- **GIVEN** a case of 3 sessions with one consumed appointment, one cancelled in time and one cancelled late
- **THEN** it shows 1 of 3 credits open, and **Book appointment** is enabled

#### Scenario: A forfeited credit completes the case

- **GIVEN** a case of 3 sessions with two consumed appointments and one cancelled late
- **THEN** it has no open credits, **Book appointment** is disabled, and **My purchases** shows the case under Past

### Requirement: Cancel a case

UC-08 with its rules, with the refund flag only. Refund-eligible means no consumed appointment, no forfeited credit, and every future booked appointment still before its deadline ([D-12](../decisions.md#consultation)).

#### Scenario: A case without appointments

- **GIVEN** Customer 1 bought 3 sessions for CHF 450.00 and booked nothing
- **WHEN** they click **Cancel case**
- **THEN** the dialog asks "Cancel this case?" and says "The purchase price of CHF 450.00 would be refunded." (question 5)
- **WHEN** they confirm
- **THEN** the case detail says "Cancelled on 15 Nov 2026, 12:00" and "The purchase price of CHF 450.00 would be refunded.", and has neither **Book appointment** nor **Cancel case** (question 6)
- **AND** `POST /api/customer/consultation-cases/{id}/cancel` answered 200

#### Scenario: Future appointments are cancelled with the case

- **GIVEN** the case has the appointment on 17 Nov 2026, 10:00–11:00
- **WHEN** Customer 1 clicks **Cancel case**
- **THEN** the dialog says the price would be refunded and lists "17 Nov 2026, 10:00–11:00 with Expert 2" as cancelled with the case
- **WHEN** they confirm
- **THEN** the appointment is cancelled, its credit refunded (question 7), and 10:00–11:00 on 17 Nov 2026 is free again for Expert 2's other customers

#### Scenario: Not refund-eligible

- **WHEN** Customer 1 clicks **Cancel case** on a case
  - with a consumed appointment,
  - or with an appointment cancelled late,
  - or with the appointment on 17 Nov 2026, 10:00–11:00 while now is 16 Nov 2026, 11:00,
- **THEN** the dialog says "The purchase price would **not** be refunded because" and, in that order:
  - "a session has already taken place.";
  - "the credit of a session cancelled late was forfeited.";
  - "the free cancellation deadline of your appointment on 17 Nov 2026, 10:00–11:00 passed on 16 Nov 2026, 10:00.",
- **AND** "Cancel anyway?" (question 5)
- **WHEN** they confirm
- **THEN** the case is cancelled and says "The purchase price would not be refunded."

#### Scenario: A refunded credit doesn't count against the refund

- **GIVEN** a case whose only appointment was cancelled in time
- **THEN** the case is refund-eligible

#### Scenario: The refund outcome changes while the dialog is open

- **GIVEN** Customer 1 opened **Cancel case** at 09:59 on 16 Nov 2026, and the dialog says the price would be refunded
- **WHEN** they confirm at 10:01
- **THEN** the response is 409 with code `REFUND_ELIGIBILITY_CHANGED`, nothing is cancelled, and the dialog shows the new outcome to confirm again (question 4)

#### Scenario: A completed or cancelled case

- **GIVEN** a case whose credits are all consumed, or a cancelled case
- **THEN** it has no **Cancel case**
- **AND** a cancellation request answers 409 with code `CASE_COMPLETED` ("This case is completed.") or `CASE_CANCELLED` ("This case was cancelled.")

#### Scenario: A cancelled case can't be booked

- **GIVEN** Customer 1 cancelled the case, and its detail is still open in another tab
- **WHEN** they book on it there
- **THEN** the bookable slots and the booking answer 409 with code `CASE_CANCELLED`, and the dialog shows the message

#### Scenario: The customer changes their mind

- **WHEN** Customer 1 closes the dialog, or clicks **Keep case**
- **THEN** nothing changes

### Requirement: Cancelled cases on My purchases

UC-05's Past group: completed and cancelled cases, collapsed.

#### Scenario: A cancelled case is Past

- **GIVEN** Customer 1 cancelled a case that had an upcoming appointment
- **WHEN** they open **My purchases**
- **THEN** the case is under Past, and its card says "Cancelled" in place of the credits and the next appointment (question 6)
- **AND** Past is sorted newest first, a cancelled case by its cancellation time (question 8)

### Requirement: The booking confirmation once free cancellation has passed

An offering's deadline may be longer than the 24 hours' notice a booking needs; the seeded 24 hours can't produce this (question 9).

#### Scenario: Late from the start

- **GIVEN** an offering with a 72-hour cancellation deadline, and its expert is available on Wed 11 Nov 2026, while now is Mon 9 Nov 2026, 15:00
- **WHEN** the customer goes to the confirmation of 11 Nov 2026, 10:00–11:00
- **THEN** it says "Free cancellation has already passed for this time: if you cancel, the credit isn't returned." instead of "Free cancellation until 8 Nov 2026, 10:00."

### Requirement: Concurrent cancellations

[D-27](../decisions.md#architecture): cancelling an appointment or a case locks the case, as a booking does, and checks every rule under the lock. A booking checks under its case lock that the case isn't cancelled.

#### Scenario: Cancel a case while booking on it

- **WHEN** Customer 1 cancels a case in one tab and books on it in another at the same moment
- **THEN** either the booking comes first and the cancellation cancels the new appointment with the case, or the cancellation comes first and the booking answers 409 `CASE_CANCELLED`
- **AND** a cancelled case never has a future booked appointment (question 10)

### Requirement: Only the owner cancels

[Security](../architecture/security.md#ownership-in-use-cases): the case is loaded with a query scoped to the customer, and the appointment under its case.

#### Scenario: Another customer's case or appointment

- **WHEN** Customer 2 cancels Customer 1's appointment or case
- **THEN** the response is 404, and nothing changes
- **AND** an appointment of another of Customer 1's cases, under this case's path, answers 404 too

#### Scenario: Routes by role

- **WHEN** a user without `CUSTOMER` calls either cancellation route
- **THEN** it answers 403 (401 without a login)

## Open questions

Each question has a recommended answer. The product owner decides; until then this spec stays a draft. The ones marked *technical* change no behavior a user sees, but the tasks rely on them.

Agreed with the product owner while planning increment 04 (2026-11-09): the scope, UC-07 and UC-08 together, in the increment after booking.

1. **The free-cancellation time on each upcoming appointment.** UC-05 doesn't show it: the customer reads it in the booking confirmation, and then only in the late warning, when it's too late. *Recommended:* each upcoming appointment on the case detail says "Free cancellation until 16 Nov 2026, 10:00." while that's ahead, and "Free cancellation ended on 16 Nov 2026, 10:00." after. The server computes it, as it does for the slots ([spec 04](04-book-consultation-appointment.md#decisions), decision 18). UC-05 changes. *Alternative:* only the dialogs say it.
2. **The dialogs' texts and buttons.** UC-07's in-time question doesn't name the appointment, and in a dialog about cancelling, the **Cancel** button of the shared `ConfirmDialog` would mean "don't cancel". *Recommended:* the question names the appointment, "Cancel the appointment with Expert 2 on 17 Nov 2026, 10:00–11:00 (Zurich time)?", followed by UC-07's consequence, in time or late. The buttons say what they do: **Keep appointment** and **Cancel appointment**, **Keep case** and **Cancel case**. UC-07 changes.
3. **Where cancelled appointments show.** UC-05 lists them among the past appointments, also when their start is still ahead. *Recommended:* yes, in one list renamed "Past and cancelled appointments", by start, newest first, each with "Consumed", "Cancelled, credit refunded" or "Cancelled, credit forfeited". *Alternative:* a third list, "Cancelled appointments".
4. **The server cancels only what the customer confirmed.** The outcome can change while a dialog is open: the deadline passes, or an appointment starts. *Recommended:* the request names the outcome the customer saw: for an appointment, whether the credit is returned; for a case, whether it's refund-eligible. If the server finds another, it answers 409 (`DEADLINE_PASSED`, `REFUND_ELIGIBILITY_CHANGED`), changes nothing, and the dialog shows the new outcome to confirm again. As with the price ([spec 03](03-buy-consultation-package.md#decisions), decision 3) and the expert ([spec 04](04-book-consultation-appointment.md#decisions), decision 12), the server never does what the customer didn't see. The appointments that a case cancellation cancels aren't checked: they're all of the case's future ones, by definition. *Alternative:* the server decides alone, and the answer says what happened.
5. **The refund texts.** UC-08 says "would be refunded" in the PoC and leaves the reason open. *Recommended:* the PoC says "would be refunded" and "would **not** be refunded" everywhere, in the dialog and on the cancelled case; the MVP says "will be" once money moves. The reason is the first that applies, in UC-08's order: "a session has already taken place", "the credit of a session cancelled late was forfeited", or "the free cancellation deadline of your appointment on <date, time> passed on <date, time>", naming the soonest such appointment. One reason decides the outcome; listing every one adds text and changes nothing. UC-08 changes. *Alternative:* list every reason that applies.
6. **The cancelled case.** *Recommended:* the case detail says "Cancelled on <date, time>" and the refund outcome, in place of the credits. It has no **Book appointment**, no **Cancel case**, no **Cancel** on an appointment and no "Book your first session"; its appointments stay visible. Its card on **My purchases** says "Cancelled" in place of the credits and the next appointment. UC-05 changes.
7. **Appointments cancelled with the case** follow UC-07's rule: one before its deadline gets its credit refunded, one after forfeits it. *Recommended:* yes. "Forfeited" keeps the glossary's meaning, and a case that wasn't refund-eligible because of such an appointment shows that appointment as forfeited afterwards. UC-08 changes. *Alternative:* all of them as refunded, since the case's refund flag decides the money anyway.
8. **The order of Past.** UC-05 sorts by "last date", and spec 04 sorts completed cases by their last appointment; a cancelled case may have none. *Recommended:* one order, newest first: a completed case by its last appointment's start, a cancelled case by its cancellation time. UC-05 changes. *Alternative:* by the last appointment, and a cancelled case without one by its purchase time.
9. **A booking whose free cancellation has already passed.** With a deadline longer than the 24 hours' notice, spec 04's confirmation would say "Free cancellation until" a time in the past. *Recommended:* the confirmation says "Free cancellation has already passed for this time: if you cancel, the credit isn't returned." instead. The server says so per slot, as it computes the free-cancellation time. UC-06 step 5 changes. *Alternative:* leave the confirmation as it is; the seeded deadline can't produce it.
10. **The race of this increment** (*technical*). Cancelling and booking both lock the case first ([D-27](../decisions.md#architecture)). *Recommended:* one concurrency test: cancelling a case and booking on it at the same moment. It must fail without either case lock: without the cancellation's, it misses the new appointment; without the booking's, the booking misses the cancellation. Two cancellations of one appointment or one case need no test: under the lock the second answers 409, and without it both write the same. [backend.md](../architecture/backend.md#testing)'s list of races gains this one.
11. **Only what exists is stored** (*technical*), as in specs 03 and 04. *Recommended:* the case gets a status (`ACTIVE`, `CANCELLED`), `cancelled_at` and `refund_eligible`, both set exactly when it's cancelled. The appointment's status gains `CANCELLED`, and `credit_refunded`, set exactly when it's cancelled. The appointment stores no cancellation time: nothing shows one. Open credits are the session count minus the booked and the forfeited appointments, until credit adjustments (UC-18) exist.
12. **The routes and the dialogs' preview** (*technical*). *Recommended:* `POST /api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel` and `POST /api/customer/consultation-cases/{id}/cancel`, like the offering's `publish` and `archive`; each answers 200, and the page reloads the case, as after a booking. The appointment is addressed under its case, so the case's scoped query decides ownership. The dialogs read their preview from the case detail, which the server computes (each upcoming appointment's free-cancellation time and whether cancelling it now returns the credit; the case's refund eligibility and its reason; whether it can be cancelled), and which a dialog reloads when it opens. No route of its own for a preview.

## Decisions

None yet besides the scope above. Once the open questions are answered, their answers move here with the reason, and the durable ones go into [decisions.md](../decisions.md), the use cases and the architecture docs in the same commit ([planning an increment](README.md#planning-an-increment)).

## Out of scope

- Cancelling a training enrollment: UC-09, with the trainings.
- Moving money and partial refunds: UC-08's out of scope (MVP).
- The expert or an admin cancelling appointments, and an admin cancelling a case: UC-07's and UC-08's out of scope, and the [future list](../use-cases/README.md#future-not-planned).
- Rescheduling and cancellation reasons: UC-07's out of scope; the customer cancels and books again.
- The expert's view of cancelled appointments and cases: [increment 06](06-expert-side-of-consultations.md) (UC-11).
- The admin's view of the cancellation and `refundEligible`, credit adjustments, and refunding a forfeited credit: UC-18.
- Email notifications: [future list](../use-cases/README.md#future-not-planned).

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

"Day 2", "day 3" and "day 5" are the dates two, three and five days from today.

1. Open `http://localhost:5173`, log in as Expert 2, open **Availability**, select day 2, day 3 and day 5, set 09:00–12:00, and save.
2. Log out, log in as Customer 1, buy 3 sessions of Career Coaching, and book Expert 2 on day 3, 10:00–11:00, and on day 5, 10:00–11:00. Each upcoming appointment says until when it can be cancelled for free: one day earlier, 10:00.
3. Click **Cancel** on day 3: the dialog names the appointment and says the credit will be returned. **Keep appointment** changes nothing. Cancel it: 2 of 3 credits open, and the appointment as "Cancelled, credit refunded". Click **Book appointment**: day 3 offers 10:00–11:00 again. Close the dialog.
4. In a private window, log in as Admin 1, open Career Coaching, set the cancellation deadline to 72 hours, and save.
5. As Customer 1, buy 3 sessions of Career Coaching again: the new case has the 72-hour deadline. Book Expert 2 on day 2, 11:00–12:00: the confirmation says free cancellation has already passed. Confirm, then click **Cancel**: the warning names the deadline that passed. Confirm: still 2 of 3 credits open, and the appointment as "Cancelled, credit forfeited".
6. Click **Cancel case** on this case: the price would not be refunded, because the credit of a session cancelled late was forfeited. Confirm: "Cancelled on …", without **Book appointment** or **Cancel case**.
7. Open the first case and click **Cancel case**: CHF 450.00 would be refunded, and day 5, 10:00–11:00 with Expert 2 is cancelled with it. Confirm.
8. Open **My purchases**: both cases are under Past, each "Cancelled".
9. As Admin 1, set Career Coaching's cancellation deadline back to 24 hours, and save.

Consumed appointments, the deadline passing while a dialog is open, and the race need time or timing, so tests with a fixed `Clock` cover them instead of the demo.

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

10. Open `http://localhost:8080`, log in as Customer 2, buy Career Coaching, book Expert 2 on day 3, and cancel the appointment in time.

## Risks

| Risk | Mitigation |
|---|---|
| The deadline is off by one or by an hour: a cancellation at the deadline counted as free, or the deadline computed in local time across a summer-time change. | The rule compares instants, `now < start − deadline`, never local times. Domain tests just before, at and after the deadline, and across both summer-time changes. |
| A dialog opened a while ago promises an outcome the server no longer gives. | The request names the outcome the customer confirmed, and a changed one is a 409 (question 4). |
| A booking slips in while the case is cancelled, and a cancelled case keeps a booked appointment. | Both lock the case first and check under the lock. A concurrency test makes the race certain and fails without either lock (question 10). |
| Refund eligibility has many cases, and the dialog and the stored flag disagree. | One domain rule decides both, the preview and the cancellation. Unit tests for each condition alone and together, at the deadline, and with a refunded credit that doesn't count. |
| Cancelled appointments leak into what counts only booked ones: busy time, open credits, Upcoming. | Every query of appointments filters by `BOOKED` since increment 04 ([spec 04](04-book-consultation-appointment.md#decisions), decision 15). REST tests check that a cancelled time is bookable again and counts as no booked credit. |
| Open credits and the groups are computed for the case detail, the list and the booking, and drift apart with forfeited credits. | One domain rule for credits and one for the groups, shared by every view, as increment 04's plan asks; the review checks that there's no copy. |
| A late cancellation needs time to pass in the demo. | The demo sets a 72-hour deadline, so a booking two days ahead is late from the start. |
