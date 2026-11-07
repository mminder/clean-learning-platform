# Coaching and Training Web Platform

## Goal
In this project I want to build a coaching and training platform for private, public customers on the internet. Customers can purchase and book consultation appointments or training courses. It should be a web application.

We start with a PoC implementation and extend and refine that into a MVP.

## Actors
* Customer: private person that purchases, books and takes part in consultation or training
* Expert: employee of the platform that coaches customers or moderates training courses
* Admin: backoffice employee of the platform that manages users and content

## Use Cases

### User logs in
There's a login button in the web header. For a first PoC, the user can just be selected by his name. We build the actual login later. We need two fixed users for each role. After selecting the user, the user is logged in. So 3 roles (Customer, Expert, Admin) and 6 users will be persisted per default in the DB.

### Customer browses public offering
* There's a shop page that lists all offerings as cards. It is marked whether an offering is training or consultation.
* Detail page of an offering can be visited.
* "Buy" button on both offering list and offering details.
* Out of scope: we don't have a shopping cart for MVP
* In a first PoC we don't implement actual payment. The checkout just consists of a confirmation dialog "do you really want to buy this". After clicking yes, the purchase is completed.

### Customer purchases consultation
* A consultaiton "case" consists of multiple session appointments. The case purchase itself doesn't book any appointments yet. The case as a number of bookable "session credits". We initially offer two variants: 3 or 5 session offering.
* The initial case without booked sessions also doesn't have an expert assigned yet
* as a first example we have the "career coaching" consultation offerings

### Customer purchases training
* There are different kinds of trainings, the course content differs. As a first example we have a Software Architecture course and a AI Augmented Software Engineering course. The Software Architecture course has two course days, the AI AUgmented only one day. So trainings must be configurable to have multiple days, each day has time slots (e.g. 09:00 - 12:00 and 13:00 - 17:00) so that it's clear when it starts, ends and when there are breaks. 
* Each training has runs, those are the dates when the course takes place. A customer purchases a specific training run and is then enrolled in that run
* each training run has a limited amount of seats for customers

### Customer views purchased products
* Directly after login, the customer sees a personal landing page where purchased offerings are displayed as cards. They are ordered by date, so that the next consultaiton appointment or training day comes first. 
* there is a detail page for these as well

### Customer books consultation appointment
* On the consultation case detail page the customer can see how many appointment (session) credit he has still open to book. Also he sees all booked future and also past appointments.
* If there are open session credits, the customer can book an appointment
* When booking the first appointment, an expert must be selected. This becomes the cases assigned expert. MVP the expert can't be changed. All appointments in the case are booked with the same expert
* Depending on the configured session duration, the customer now selects a day an time slot where the expert is available
* the appointment is booked directly after a confirmation dialog

### Customer cancels booked consultation appointment
* in the case detail page, the customer has the option to cancel booked appointments.
* the case offering has a configured cancellation deadline in days
* if cancelling before the deadline, the session credit is refunded into the case
* if cancelling after the deadline, the user is informed that the credit wont be refunded. Cancellation can still be performed when confirming.

### Customer cancels whole consultation case
* If the customer made a wrong purchase, he can cancel the whole case.
* refund of purchase only if all booked appointments within cancellation deadline and no appointment was consumed
* actual refund is not implemented for PoC, as we don't handle payment yet

### Customer cancels booked training
* the training has a cancellation deadline in days as well
* refund of purchase only if there was no training day consumed yet and the next training day is before the deadline
* actual refund is not implemented for PoC, as we don't handle payment yet

### Customer takes part in training or consultation call
* on the detail page, there is a teams link where the user can join the call
* the link is shown 1h before the call starts. If earlier, a hint is shown so that the user knows when the link will appear
* MVP just teams link, in the future embedded solution e.g. Jitsi

### Expert sees all his consultation cases
* case card shows the customers name, next booked appointment datetime, bookable session count, total session count
* completed cases come last
* details page shows all appointments

### Expert sees all his traininigs
* training card shows next run datetime
* detail page shows all runs and days

### Expert takes part in training or consultation call
MVP just teams link, in the future embedded solution e.g. Jitsi

### Expert manages his availability (calendar)
MVP keep it simple, just select days and fixed time ranges that are applied to all days

### Admin creates, updates or deletes training offering
* metadata form (name, description, image, etc)
* timeslots and runs can be created, edited and deleted
* expert can be assigned to training run

### Admin creates, updates or deletes consultation offering
* metadata form (name, description, image, etc)
* session counts and duration

### Expert takes consultation notes
* on a consultation case, the expert can take notes
* notes have a timestamp, title and text content
* notes can either be private (just for the expert) or public (shared with customer)

### Admin views all consultation cases
* admin can see all consultaiton cases with customer name, expert name, appointments, session counts
* admin mustn't see the notes
* this will be used in the future to support e.g. change expert in case they are sick, refund appointments for goodwill reasons even after cancellation deadline, etc
* as PoC there is just the read only info and the option to change the available session count (goodwill reasons)

### Admin views all trainings
* admin sees all trainings
* admin sees all runs on all traininigs
* admin can re-assign expert on training runs

## System Architecture
* Web platform
* React SPA frontend
* Spring boot backend
* Rest API between FE and BE
* Database (postgres)
* Oauth 2 login
* Monorepo

## Software Architecture

### Backend
* Modularized Monolith
* Hexagonal Architecure
* Each UseCase gets its own Spring Service and java class
* UseCase service contains only flow logic if possible. Business logic is extracted into POJO Domain Classes.
* Use DDD Value Objects
* YAGNI, only introduce as much complexity as absolutely necessary
* DDD "inspired" domain, but don't force strict aggregate rules. Keep it pragmatic. Use case specific objects are often simpler and lead to a more decoupled and maintainable system instead of forcing all use cases to use a shared domain which then couples them. 

### Backend layering / packages
I want these root level packages. Additional added if necessary, e.g. maybe a spring config package
* api: rest controllers
* application: use case services and ports. Don't do incoming ports for rest controller calls, they don't add any value IMHO.
* domain: pojo domain logic and value objects (DDD "inspired"). Domain model and core business logic.
* adapters.database: database connection implementation. Maps from hibernate entities to domain pojo objects. Implements application port interfaces.

### Backend testing concept
* extensive unit test coverage for domain package
* one happy case rest api integration test for each rest route
* application use case test only where it adds real value, not mandatory
* authorization of user roles and filtering what they can see must be tested! TBD where the best seam is. Either rest api integraiton or use case service

### Frontend
* React
* Tailwind
* TBD
