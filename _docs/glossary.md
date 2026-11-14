# Glossary

The ubiquitous language of the platform. Use these terms in code, API, UI and docs.

## Concept map

```
User ──* Role {CUSTOMER, EXPERT, ADMIN}
 ├─ ExpertProfile (display name, bio, photo, Teams link)        only with EXPERT
 └─ Availability (calendar dates + daily time ranges)           only with EXPERT

Offering (name, description, image, status)
 ├─ ConsultationOffering (session duration, cancellation deadline)
 │    ├─ Package *            (session count, price)
 │    └─ eligible Expert *
 └─ TrainingOffering (price, cancellation deadline, default seats)
      ├─ day template: TrainingDay * ── TimeSlot *
      └─ Run * (seats, Expert?, Teams link)
           └─ schedule: RunDay * (date) ── TimeSlot *

Customer buys a Package ──► Case (purchase terms, assigned Expert?)
                              ├─ Appointment *
                              ├─ Note *
                              └─ CreditAdjustment *
Customer buys a Run     ──► Enrollment (purchase terms)
```

## Users and roles

| Term | Meaning |
|---|---|
| User | A person with an account. Holds one or more roles. |
| Role | `CUSTOMER`, `EXPERT` or `ADMIN`. Roles are stored in our database, not in the IdP. |
| Visitor | Anyone who isn't logged in. Can browse the shop. |
| Customer | A user with `CUSTOMER`. Buys, books and attends. Always an individual buying for themself. |
| Expert | A user with `EXPERT`. Leads consultations and training runs. |
| Admin | A user with `ADMIN`. Manages offerings, cases and (MVP) users. |
| Expert profile | Display name, bio, photo and personal Teams link of an expert. Display name, bio and photo are public. In code, `ExpertProfile` is this public part, and `OwnExpertProfile` the whole profile with the Teams link, which only its expert loads. |
| CurrentUser | The authenticated user (id and roles) that controllers pass into use cases. |

## Catalog

| Term | Meaning |
|---|---|
| Offering | Something sold in the shop. Type is consultation or training. Has a name, description, image and status. |
| Offering status | `DRAFT` (not visible) → `PUBLISHED` (in the shop) → `ARCHIVED` (not buyable; existing purchases keep working). |
| Consultation offering | Offering with a session duration, a cancellation deadline, packages and eligible experts. |
| Package | A purchasable variant of a consultation offering: a session count and a price, e.g. 3 sessions for CHF 450. Called `ConsultationPackage` in code and `consultation_package` in the database, because Java already has `java.lang.Package`. |
| Eligible expert | An expert the admin assigned to a consultation offering. Customers pick their case's expert from this set. |
| Training offering | Offering with a price, a cancellation deadline, default seats, a day template and runs. |
| Day template | Ordered list of training days, each with time slots. Used to create runs. |
| Time slot | A local time range within a day, e.g. 09:00–12:00. Gaps between slots are breaks. |
| Run | One concrete occurrence of a training. Has one date per training day, a schedule copied from the day template, seats, an optional expert and a Teams link. |
| Run day | One date of a run, with its time slots. |
| Seats | Maximum number of active enrollments in a run. |
| Price | `Money` in CHF, including VAT. |

## Purchases

| Term | Meaning |
|---|---|
| Purchase terms | Values copied from the offering at purchase time: price paid, and for consultations also session count, session duration and cancellation deadline. Later offering edits never change them. |
| Case | What a customer gets by buying a package. Holds the purchase terms, the assigned expert, appointments, notes and credit adjustments. Called `ConsultationCase` in code (`ConsultationCaseId`, `NewConsultationCase`) and `consultation_case` in the database, because `case` is reserved in SQL and a Java keyword, and a class named `Case` reads like part of a use case's name. |
| Case status | `ACTIVE` or `CANCELLED` (stored). `COMPLETED` is derived: no open credits and no booked appointment that hasn't ended. |
| Enrollment | What a customer gets by buying a run. Holds the purchase terms. |
| Enrollment status | `ENROLLED` or `CANCELLED` (stored). `COMPLETED` is derived: the run's last time slot has ended. |
| Refund-eligible | Flag stored on a cancelled case or enrollment. No money moves in the PoC. |

## Consultation

| Term | Meaning |
|---|---|
| Assigned expert | The expert of a case. Chosen by the customer on the first booking. Can't change in PoC/MVP. |
| Credit (session credit) | The right to book one appointment in a case. |
| Total credits | Session count from the purchase terms plus the sum of all credit adjustments. |
| Open credits | `total credits − BOOKED appointments − forfeited credits`. Always computed, never stored. |
| Appointment | One booked session of a case with the assigned expert. Has a start and an end. |
| Appointment status | `BOOKED` or `CANCELLED` (stored). `CONSUMED` is derived: `BOOKED` and the start has passed. |
| Consumed | An appointment whose start time has passed. Attendance isn't tracked. |
| Cancelled appointment | An appointment its customer cancelled, alone or with its case. It keeps its start and end and whether its credit was refunded. It's no busy time, and the customer's pages don't show it. Called `CancelledAppointment` in code; an `Appointment` is a booked one. |
| Upcoming appointment | A `BOOKED` appointment that hasn't ended. One that has started is consumed, and still upcoming until its end, so the pages keep it, and its call link, within reach while the session runs. |
| Forfeited credit | The credit of an appointment cancelled after its cancellation deadline (`creditRefunded = false`). |
| Credit adjustment | A goodwill change by an admin: a delta (±), a mandatory reason, the admin, and a timestamp. |
| Note | Text an expert writes on a case: title, text, visibility (`PRIVATE` or `PUBLIC`), the time it was written and the time of its last edit. |
| Availability | An expert's selected calendar dates plus one set of daily time ranges that applies to all of them. |
| Busy time | The times a new appointment can't overlap. An expert's: their `BOOKED` appointments plus the time slots of runs they're assigned to. A customer's: the `BOOKED` appointments of all their cases. |
| Bookable slot | A start time on the 30-minute grid where the whole session fits inside the expert's availability, overlaps neither the expert's nor the customer's busy time, and is at least 24 h from now. |

## Cancellation

| Term | Meaning |
|---|---|
| Cancellation deadline | A duration in hours, configured per offering and copied into the purchase terms. A cancellation before `start − deadline` is free. |
| Free cancellation | Cancelling before the deadline: the appointment credit is refunded, or the case or enrollment is refund-eligible. |
| Late cancellation | Cancelling after the deadline: allowed after a warning, but the credit is forfeited or the purchase isn't refund-eligible. |

## Calls

| Term | Meaning |
|---|---|
| Teams link | Consultations use the assigned expert's personal link from the expert profile, any `https://` link. Trainings use the run's link, set by the admin. |
| Link window | Customers see a call's link from 1 h before it starts until it ends. Experts always see it. |

## Time

| Term | Meaning |
|---|---|
| Business time zone | `Europe/Zurich`. All local times (time slots, availability) are Zurich times, and all displayed times are Zurich times. |
