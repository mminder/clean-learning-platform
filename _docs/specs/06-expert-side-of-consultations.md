# 06 The expert's side of consultations

| | |
|---|---|
| Status | Draft |
| Use cases | [UC-11](../use-cases/uc-11-view-my-consultation-cases.md), all steps; [UC-15](../use-cases/uc-15-take-consultation-notes.md), all steps; [UC-14](../use-cases/uc-14-manage-expert-profile.md), all steps; [UC-10](../use-cases/uc-10-join-call.md), for consultation appointments; [UC-05](../use-cases/uc-05-view-my-purchases.md), the expert's photo, **Join** and the public notes on the case detail; [UC-06](../use-cases/uc-06-book-consultation-appointment.md) and [UC-02](../use-cases/uc-02-browse-shop.md), the expert's photo in the expert picker and on the offering detail; [UC-01](../use-cases/uc-01-login.md), the expert's landing page |
| Tasks | Not planned yet: they're written once the open questions are answered |

## Why

After increments 04 and 05, customers book and cancel appointments, but the expert sees none of it: a booking is only a time that's gone from their availability ([spec 04](04-book-consultation-appointment.md#decisions), decision 1). The expert doesn't know who booked them or when, customer and expert have no agreed place to meet, and the expert has nowhere to keep notes or to share advice. The shop and the booking dialog show a placeholder instead of the expert's photo, and an expert's name and bio are what the seed says.

This increment gives the expert their side, as agreed while planning increment 04: their cases (UC-11), their notes (UC-15), their profile with photo and Teams link (UC-14), and the call link for both sides (UC-10). They come together because they meet on the expert's case detail: its appointments carry the call link, which comes from the profile, and its notes are written there. It comes after the cancellations, so the expert's cases show cancelled appointments and cases from the start. With it, a consultation runs end to end: buy, book, meet, take notes, cancel.

It also brings what later use cases reuse:

- the expert as the one a case is shown to: queries scoped to the assigned expert, and Expert 2 on Expert 1's case a 404 ([security](../architecture/security.md#ownership-in-use-cases));
- one resource shaped per audience: the customer's case has only the public notes, the expert's has all of them, and the admin's (UC-18) will have none ([D-14](../decisions.md#consultation));
- a response whose content depends on the time: the Teams link inside the link window only ([D-19](../decisions.md#views-and-calls)), which the training days (UC-12, UC-17) reuse;
- the second image owner after the offering, the expert photo ([D-21](../decisions.md#cross-cutting));
- the first rows a user creates, edits and deletes in a list of their own: notes.

## What changes

- An expert lands on **My cases**: the cases they're assigned to, grouped into Upcoming, Needs booking and Past, each with the customer, the offering, the next appointment and the credits.
- The expert's case detail shows the customer's name and email, the terms, every appointment with its status, and the notes.
- The expert adds, edits and deletes notes on a case, each private or public. The customer sees the public ones on their case.
- An expert edits their **Profile**: display name, bio, photo and Teams link. The photo shows in the shop, in the booking dialog and on the customer's case.
- Each upcoming appointment has **Join**, which opens the expert's Teams link. The expert always sees it; the customer from one hour before the start until the end.

## User stories

1. As an expert, I want to see my cases and what's next, so that I'm prepared for each session.
2. As an expert, I want to see the customer's name and email on a case, so that I can reach them ([D-18](../decisions.md#views-and-calls)).
3. As an expert, I want to see the consumed and cancelled appointments of a case, so that I know its history.
4. As an expert, I want to keep private notes on a case, so that I remember what we discussed.
5. As an expert, I want to share some notes with the customer, so that they have my advice in writing.
6. As a customer, I want to read my expert's shared notes on my case, so that I can follow up.
7. As an expert, I want to edit my display name, bio and photo, so that customers can choose me.
8. As an expert, I want to set my Teams link once, so that every appointment has a place to meet.
9. As a customer, I want **Join** on my appointment shortly before it starts, so that I find the call without searching.
10. As an expert, I want **Join** on each of my appointments at any time, so that I can open the call early.
11. As a developer, I want one finished example of a response shaped by audience and by time, so that the admin's cases (UC-18) and the training days follow one pattern.

## Requirements

In the scenarios, Career Coaching has 60-minute sessions and a 24-hour cancellation deadline, as seeded. Customer 1's case of 3 sessions has Expert 2 as its expert and an appointment on Tue 17 Nov 2026, 10:00–11:00; Customer 2's case has Expert 1. Both experts' profiles have a Teams link, unless a scenario says otherwise. "Now" is Mon 9 Nov 2026, 15:00, unless a scenario says otherwise; tests fix it with the `Clock`. All times are Zurich times.

### Requirement: My cases

UC-11 steps 1–3 and its rules: a case becomes the expert's with the customer's first booking ([D-14](../decisions.md#consultation)).

#### Scenario: The expert's landing page

- **WHEN** Expert 1 or Expert 2 logs in
- **THEN** **My cases** opens, and the header shows **My cases**, **Availability** and **Profile** (question 1)
- **AND** Admin 1 still lands on **Consultations**, and Customer 1 on **My purchases**

#### Scenario: Only assigned cases

- **GIVEN** Customer 1's case has Expert 2, Customer 2's has Expert 1, and Customer 2 has another case without an expert
- **WHEN** Expert 2 opens **My cases**
- **THEN** it shows Customer 1's case only, and the case without an expert is in no expert's list

#### Scenario: The groups

- **GIVEN** Expert 2 has a case with an upcoming appointment, an active case with open credits and nothing upcoming, a completed case and a cancelled case
- **WHEN** Expert 2 opens **My cases**
- **THEN** the first is under **Upcoming**, the second under **Needs booking**, and the other two under **Past**, which is collapsed
- **AND** Upcoming is sorted by the next appointment, Needs booking by purchase time (question 3), and Past newest first, as on **My purchases** ([spec 05](05-cancel-consultation-appointment-or-case.md#open-questions), question 8)

#### Scenario: The case card

- **THEN** each card shows the customer's name, the offering's name, the next appointment's date and time, and the open and total credits

#### Scenario: No cases yet

- **WHEN** an expert without cases opens **My cases**
- **THEN** it says "No cases yet. A case appears here once a customer books you."

#### Scenario: The expert keeps their cases

- **GIVEN** an admin removed Expert 2 from Career Coaching's eligible experts
- **THEN** Customer 1's case is still on Expert 2's **My cases**: the assigned expert never changes (UC-06)

#### Scenario: An expert who booked themself

- **GIVEN** Expert 1, who is also a customer, booked themself on their own case
- **THEN** the case is on Expert 1's **My cases**, with Expert 1 as the customer, and on their **My purchases** ([D-06](../decisions.md#catalog))

### Requirement: The expert's case detail

UC-11 step 4.

#### Scenario: The detail

- **GIVEN** Customer 1's case has a consumed appointment, one cancelled late, and the upcoming one on 17 Nov 2026
- **WHEN** Expert 2 opens it
- **THEN** it shows Customer 1's name and email, Career Coaching, the terms (question 4), the open and total credits, the upcoming appointment with **Join**, the consumed one, and the cancelled one as "Cancelled, credit forfeited"
- **AND** the notes, private and public, newest first
- **AND** `GET /api/expert/consultation-cases/{id}` answered 200

#### Scenario: A cancelled case

- **GIVEN** Customer 1 cancelled the case
- **THEN** Expert 2's detail says "Cancelled on <date, time>", without the refund outcome (question 4)

#### Scenario: Another expert's case

- **WHEN** Expert 1 opens Customer 1's case, by its URL or through the API
- **THEN** the page says "Case not found", and the API answers 404

### Requirement: Notes

UC-15 with its rules.

#### Scenario: Add a private note

- **WHEN** Expert 2 clicks **Add note** on Customer 1's case, enters the title "First session" and a text of two lines, keeps **Private**, and saves
- **THEN** the note is first in the list, with the badge "Private", the time it was written (question 5) and its line break
- **AND** `POST /api/expert/consultation-cases/{id}/notes` answered 201

#### Scenario: Public notes reach the customer

- **GIVEN** Expert 2 wrote a private and a public note on the case
- **WHEN** Customer 1 opens the case
- **THEN** it shows the public note only, under "Notes from your expert", with its title, text and time (question 6)
- **AND** the customer's case response contains no private note, not even its id

#### Scenario: Edit a note

- **WHEN** Expert 2 changes the private note's text and makes it public, on 10 Nov 2026 at 09:00
- **THEN** it says "edited 10 Nov 2026, 09:00", keeps its place in the list, and Customer 1 sees it
- **AND** when Expert 2 makes it private again, Customer 1 no longer sees it

#### Scenario: Delete a note

- **WHEN** Expert 2 deletes a note and confirms "Delete the note "First session"? This can't be undone."
- **THEN** it's gone for both of them

#### Scenario: Invalid note

- **WHEN** a note has no title or no text, or only spaces
- **THEN** the response is 400, the form shows a message next to each invalid field, and nothing is saved

#### Scenario: Notes in any status

- **THEN** Expert 2 can add, edit and delete notes on an active, a completed and a cancelled case (UC-15)

#### Scenario: Two tabs, one note

- **GIVEN** a note is open in two tabs
- **WHEN** Expert 2 deletes it in one tab and then saves it in the other
- **THEN** the save answers 404, and the notes reload (question 7)

#### Scenario: An expert who is also the customer

- **GIVEN** Expert 1 booked themself and wrote a private and a public note on the case
- **WHEN** Expert 1 opens the case from **My purchases**
- **THEN** it shows the public note only: the customer's view is the customer's, whoever holds it

### Requirement: The expert profile

UC-14 with its rules and [D-21](../decisions.md#cross-cutting).

#### Scenario: Edit the profile

- **WHEN** Expert 2 opens **Profile**, changes the display name to "Expert Two", the bio and the Teams link, and saves
- **THEN** the page says "Saved", and after a reload it shows the same values
- **AND** Career Coaching's detail in the shop, the booking dialog's experts, Customer 1's case and Customer 1's case card show "Expert Two" at once
- **AND** `PUT /api/expert/profile` answered 200

#### Scenario: Invalid profile

- **WHEN** the display name is blank, or the Teams link isn't an `https://` URL
- **THEN** the response is 400, the form shows a message next to each invalid field, and nothing is saved
- **AND** an empty bio and an empty Teams link are valid (question 8)

#### Scenario: Upload, replace and remove the photo

- **WHEN** Expert 2 uploads a JPEG photo on **Profile**
- **THEN** the profile, Career Coaching's detail in the shop, the booking dialog's experts and Customer 1's case show it instead of the placeholder (question 9)
- **WHEN** they replace it with a PNG
- **THEN** the new photo has a new URL, and the old one answers 404
- **WHEN** they remove it
- **THEN** the placeholder shows again everywhere, and the photo's URL answers 404

#### Scenario: An invalid photo

- **WHEN** the file isn't a JPEG, PNG or WebP, or is larger than 2 MB
- **THEN** the response is 400, the page shows the message, and the photo stays as it was

#### Scenario: Only their own profile

- **WHEN** Expert 1 and Expert 2 each open **Profile**
- **THEN** each sees and edits only their own: the profile's routes have no id

#### Scenario: The Teams link stays private

- **THEN** the shop, the booking dialog's experts and the customer's case show the expert's display name, bio and photo, but never the Teams link outside the link window (UC-10)

### Requirement: Join a call

UC-10 for consultation appointments, with [D-19](../decisions.md#views-and-calls): the link is the assigned expert's Teams link, read from their profile when the case is shown. The customer's window starts one hour before the appointment and ends with it: it includes 09:00 and excludes 11:00 for the appointment 10:00–11:00.

#### Scenario: The expert always sees the link

- **WHEN** Expert 2 opens Customer 1's case, at any time before the appointment on 17 Nov 2026 ends
- **THEN** the appointment has **Join**, which opens Expert 2's Teams link in a new tab

#### Scenario: The customer before the window

- **GIVEN** now is Tue 17 Nov 2026, 08:59
- **WHEN** Customer 1 opens the case
- **THEN** the appointment says "The link will be available from 09:00 (Zurich time)." instead of **Join**
- **AND** the customer's case response contains no link

#### Scenario: The customer inside the window

- **GIVEN** now is 17 Nov 2026, 09:00, or 10:30 while the session runs
- **THEN** Customer 1's appointment has **Join** with Expert 2's link (question 2)

#### Scenario: After the call

- **GIVEN** now is 17 Nov 2026, 11:00
- **THEN** neither side has **Join**, and the customer's case response contains no link

#### Scenario: No link yet

- **GIVEN** Expert 2's profile has no Teams link, and now is inside the window
- **THEN** Customer 1's appointment says "The meeting link has not been provided yet.", and Expert 2's says "Add your Teams link in your profile." with a link to **Profile** (question 10)

#### Scenario: The link changes

- **WHEN** Expert 2 changes their Teams link
- **THEN** each of Expert 2's upcoming appointments uses the new link

#### Scenario: Waiting on the page

- **GIVEN** Customer 1 opened the case at 08:55 on 17 Nov 2026 and leaves it open
- **THEN** **Join** appears at 09:00 without a reload (question 11)

### Requirement: Only experts, and only the assigned expert

[Security](../architecture/security.md#ownership-in-use-cases): the expert's case queries are scoped to the assigned expert in SQL, and notes are addressed under their case. Another expert's case is a 404, like an unknown one.

#### Scenario: Expert 2 on Expert 1's case

- **WHEN** Expert 2 asks for Customer 2's case, which has Expert 1, or reads, adds, edits or deletes its notes
- **THEN** the response is 404, and nothing changes
- **AND** a note of another case, under this case's path, answers 404 too

#### Scenario: Routes by role

- **WHEN** a user without `EXPERT` calls a route under `/api/expert/consultation-cases` or `/api/expert/profile`, or opens `/expert/cases` or `/expert/profile`
- **THEN** the route answers 403 (401 without a login), and the page shows "Page not found"

## Open questions

Each question has a recommended answer. The product owner decides; until then this spec stays a draft. The ones marked *technical* change no behavior a user sees, but the tasks rely on them.

Agreed with the product owner while planning increment 04 (2026-11-09): the scope, the expert's side, UC-11, UC-14, UC-10 and UC-15 together ([spec 04](04-book-consultation-appointment.md#decisions), decision 1), after the cancellations.

1. **The header and the landing page.** UC-01's priority Expert → Admin → Customer sends experts to **My cases**, and [spec 04](04-book-consultation-appointment.md#decisions) (decision 10) put **Availability** between **My purchases** and **Consultations**. *Recommended:* every user with `EXPERT` lands on **My cases**, so Expert 1 and Expert 2 do; Admin 1 lands on **Consultations** until **Cases** (UC-18) exists. The header shows the expert's entries as **My cases**, **Availability**, **Profile**, after **My purchases** and before **Consultations**. UC-01's interim rule changes.
2. **A running appointment.** An appointment is consumed from its start ([D-11](../decisions.md#consultation)), but its call lasts until its end, and UC-10 shows the link until then. Under spec 04's rules, a running appointment is already among the past appointments, and its case may have left Upcoming. *Recommended:* for what the pages show, an appointment is upcoming until it ends: the case detail shows it under Upcoming with "Now" and **Join**, without **Cancel**, and the lists keep its case under Upcoming until then, on both sides. For the rules it's consumed from its start, as before: its credit is used, it can't be cancelled, and it makes the case not refund-eligible. **My purchases**' grouping from spec 04 changes accordingly, and so do UC-05 and UC-11. *Alternative:* the past appointments show **Join** while one runs, and the lists stay as they are.
3. **Needs booking on My cases.** UC-11 sorts it by "first booking date", which nothing stores. *Recommended:* by purchase time, oldest first, like the customer's Needs booking: no new data, and both lists read alike. UC-11 changes. *Alternative:* store when the expert was assigned, and sort by it.
4. **What the expert sees of the purchase.** UC-11 shows "the purchase terms", which include the price paid, and a cancelled case's refund outcome concerns the customer and the admin (UC-08). *Recommended:* the expert sees the session count, the session duration, the cancellation deadline and the purchase time, and "Cancelled on <date, time>" for a cancelled case, but neither the price paid nor the refund outcome. The expert's case response has no such fields, as the admin's has no notes. UC-11 changes. *Alternative:* the expert sees every purchase term.
5. **The times of a note** (*partly technical*). UC-15 stores `createdAt` and shows "edited <updatedAt>". Every table's `created_at` and `updated_at` are set by the database ([D-20](../decisions.md#cross-cutting)), not by the `Clock`, which the tests set. *Recommended:* the note stores when it was written and when it was last edited, from the `Clock`, as the purchase stores its time ([spec 03](03-buy-consultation-package.md#decisions), decision 14). "edited" shows once it was edited; a change of visibility alone is an edit. The audit columns stay as they are.
6. **Notes on the customer's case.** UC-05 lists "the expert's public notes, newest first". *Recommended:* a section "Notes from your expert" below the appointments, with each note's title, text, time and "edited"; hidden while there's none, like the appointment lists.
7. **Two tabs edit one note** (*technical*). *Recommended:* the last save wins, without a lock or a version: a note is one expert's text, and no rule spans it and other rows. Editing or deleting a note that's gone answers 404, and the notes reload. No concurrency test.
8. **The profile's input rules** beyond UC-14's. *Recommended:* the bio and the Teams link may be empty, and an empty link removes it. The Teams link is any `https://` URL, not only Microsoft's: it's a value object with a check constraint ([D-36](../decisions.md#architecture)). The display name stays a `DisplayName`, never blank. *Alternative:* only links to `https://teams.microsoft.com/`.
9. **The photo.** *Recommended:* uploaded, replaced and removed like the offering image ([task 07 of increment 02](../tasks/02-manage-consultation-offerings/07-offering-image.md)): with buttons of its own on **Profile**, each saved at once, apart from the form's **Save**. It shows as a round picture, cropped by the browser and never resized (D-21), in the expert picker, on the offering detail, on the customer's case and on **Profile**; without one, the placeholder stays.
10. **The expert without a Teams link.** *Recommended:* the expert's appointments say "Add your Teams link in your profile." with a link to **Profile**. The customer's say UC-10's "The meeting link has not been provided yet." inside the window, and "The link will be available from 09:00 (Zurich time)." before it, link or not: the expert may still add one.
11. **The window opens while the page is open.** The API returns the link only inside the window, so a page loaded at 08:55 has none at 09:00. *Recommended:* the case detail reloads itself when the next window opens, at the time the server already sends for "available from". *Alternative:* the customer reloads the page.
12. **Every expert has a profile** (*technical*). In the PoC, every user with `EXPERT` has a seeded profile; UC-19 (MVP) creates an empty one when it grants `EXPERT`, which is where UC-14's "display name defaults to the user's name" applies. *Recommended:* this increment reads and edits profiles but never creates one, and a missing profile is a programming error (500), not a 404.
13. **Profile writes lock the expert** (*technical*). Replacing a photo deletes the old image and stores a new one, so two replacements at once could leave an image behind that nothing refers to. *Recommended:* saving the profile and uploading or removing the photo lock the expert's user row first, the lock that saving availability takes ([D-27](../decisions.md#architecture)), as every change to an offering locks the offering. No concurrency test: it isn't a race between users, and [backend.md](../architecture/backend.md#concurrency)'s lock table gains the row.

## Decisions

None yet besides the scope above. Once the open questions are answered, their answers move here with the reason, and the durable ones go into [decisions.md](../decisions.md), the use cases and the architecture docs in the same commit ([planning an increment](README.md#planning-an-increment)).

## Out of scope

- **My trainings** (UC-12), and **Join** and the run's link on training days (UC-10): with the trainings (UC-17).
- The admin's cases, which never show notes: UC-18.
- The expert booking, cancelling or rescheduling appointments, and messaging customers: UC-11's out of scope and the [future list](../use-cases/README.md#future-not-planned).
- Customer replies, attachments, notes per appointment and rich text: UC-15's out of scope.
- Creating Teams meetings automatically, an embedded video call and attendance tracking: UC-10's out of scope and the [future list](../use-cases/README.md#future-not-planned).
- Cropping or resizing the photo, and profiles for customers or admins: UC-14's out of scope.
- Creating a profile when `EXPERT` is granted: UC-19, MVP (question 12).
- Changing a case's expert, and email notifications: [future list](../use-cases/README.md#future-not-planned).

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

"Day 2" is the date two days from today. You need a JPEG or PNG under 2 MB to use as a photo.

1. Open `http://localhost:5173`, log in as Expert 2: **My cases** opens and says "No cases yet. …". The header shows **My cases**, **Availability** and **Profile**.
2. Open **Profile**: the seeded name and bio, the placeholder, no Teams link. Set the Teams link to `http://example.com`: a message next to it, nothing saved. Set it to `https://example.com/meet/expert-2`, change the bio and save. Upload the photo.
3. Open **Availability**, select day 2, set 09:00–12:00, and save.
4. Log out and open Career Coaching in the shop: Expert 2 has the photo and the new bio.
5. Log in as Customer 1, buy Career Coaching, and book Expert 2 on day 2, 10:00–11:00: the expert picker shows the photo. The case shows it, and the appointment says "The link will be available from 09:00 (Zurich time)."
6. In a private window, log in as Expert 2: **My cases** shows Customer 1's case under **Upcoming**, with day 2, 10:00–11:00 and 2 of 3 credits open. Open it: Customer 1's name and email, the terms without the price, and the appointment with **Join**, which opens the link in a new tab.
7. Add the private note "First session" with two lines of text, and a public note "Homework". Edit "Homework": "edited …".
8. As Customer 1, reload the case: "Notes from your expert" shows "Homework" only.
9. As Expert 2, make "First session" public and delete "Homework". As Customer 1, reload: "First session" only.
10. As Customer 1, cancel the appointment in time. As Expert 2, reload **My cases**: the case is under **Needs booking**, and its detail shows the appointment as "Cancelled, credit refunded".
11. In the private window, log out, log in as Expert 1, and paste the URL of the case from step 6: "Case not found".
12. In the private window, log out, log in as Expert 2 again, and remove the photo on **Profile**. Open Career Coaching in the shop: the placeholder is back.

The customer's **Join** inside the window and a running appointment need an appointment within the hour, which the 24 hours' notice doesn't allow, so tests with a fixed `Clock` cover them instead of the demo.

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

13. Open `http://localhost:8080`, log in as Expert 2: **My cases** opens with Customer 1's case. Open it and add a note.

## Risks

| Risk | Mitigation |
|---|---|
| The Teams link reaches a customer outside the window, or anyone through the shop or the expert picker. | The domain decides at the request's time whether the customer's response carries the link; the shop's and the picker's responses have no link field at all. Tests with a fixed `Clock` just before, at and after both ends of the window, and on the shapes of the shop's and the picker's responses. |
| A private note reaches the customer. | The customer's case loads only `PUBLIC` notes, in SQL, and its response has no visibility field to get wrong. The security test from [security.md](../architecture/security.md#testing): a case with both kinds, read as its customer. |
| An expert reaches another expert's case or notes. | Queries scoped to the assigned expert in SQL, notes addressed under their case, and ownership tests with Expert 1 and Expert 2, as the customer side does with Customer 1 and Customer 2. |
| One case is shown to two audiences, soon three (UC-18), and its credits, statuses and groups drift apart between them. | A response per audience, but one domain rule each for the credits, the appointment's status and the groups, shared by every view, as increment 04's plan asks for the customer's two views. |
| A running appointment is consumed for the rules and upcoming for the pages, and the two get mixed up. | Question 2 keeps them apart by name; domain tests at the start and the end of an appointment, for both. |
| A replaced or removed photo leaves an image behind. | Every profile write locks the expert first, and the old image is deleted in the same transaction, as for the offering image (question 13). |
| The increment is the largest so far: four use cases, three pages, notes with create, edit and delete, an upload and a time window. | The plan cuts it into tasks per use case, starting with **My cases** and the expert's case detail, on which notes and **Join** build. If it's still too large, the profile and the call link (UC-14, UC-10) can become an increment of their own: they don't depend on the notes. |
