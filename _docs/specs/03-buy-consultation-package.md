# 03 Buy a consultation package

| | |
|---|---|
| Status | Ready |
| Use cases | [UC-03](../use-cases/uc-03-purchase-consultation-package.md), all steps except step 7's hint (decision 2); [UC-05](../use-cases/uc-05-view-my-purchases.md), the landing page and the case detail for cases, as far as cases have data yet (decisions 1, 2); [UC-01](../use-cases/uc-01-login.md), login from **Buy** and the customer's landing page; [UC-02](../use-cases/uc-02-browse-shop.md), the **Buy** button; [UC-16](../use-cases/uc-16-manage-consultation-offering.md), the purchase guard on unpublish and delete |
| Tasks | [tasks/03-buy-consultation-package](../tasks/03-buy-consultation-package/README.md) |

## Why

After increments 01 and 02, admins can fill the shop and anyone can browse it, but nobody can buy anything: the shop has no **Buy** button. This increment closes that loop.

Buying creates a **case**, which UC-05 to UC-08, UC-10, UC-11, UC-15 and UC-18 all build on. It's also the first data a customer owns, so it brings what every later customer and expert use case needs, still without dates, slots or deadlines to evaluate:

- routes under `/api/customer`, and the `CurrentUser` as the owner of what a use case creates;
- ownership by scoped queries, a 404 for another customer's case, and the ownership tests with the two seeded customers ([security](../architecture/security.md#ownership-in-use-cases));
- purchase terms copied at purchase, so later edits of the offering never change them ([D-08](../decisions.md#catalog));
- the first point in time in the API and the UI, from the injected `Clock`, shown in `Europe/Zurich`;
- the purchase guard on unpublish and delete that [spec 02](02-manage-consultation-offerings.md) left for this increment (its decision 5), with the first race between a customer and an admin.

Booking needs availability (UC-13), the slot rules (D-10) and the expert lock, and gets an increment of its own. Trainings (UC-17) would add a second offering type before anything can be bought.

## What changes

- **Buy** on an offering's card and detail page opens a dialog: the customer picks a package, confirms the price, and lands on the new case.
- A visitor who clicks **Buy** logs in and comes back to the same page.
- A customer sees their cases on **My purchases**, which is now their landing page, and opens a case to see its purchase terms and credits.
- Later changes to the offering's packages, session duration or deadline never change a case. Its name and image are shown live.
- If the package changed while the dialog was open, the purchase is refused, and the dialog shows the current packages.
- An admin can no longer unpublish or delete an offering that has purchases; they archive it instead.

## User stories

1. As a customer, I want to buy a package from an offering's card or detail page, so that I get session credits.
2. As a customer, I want to confirm the package and its price before I buy, so that I don't buy by mistake.
3. As a customer, I want to be told when the package changed before I confirmed, so that I never buy at a price I didn't see.
4. As a visitor, I want to be asked to log in when I click **Buy** and then come back to the offering, so that I can carry on.
5. As a customer, I want to see everything I bought on **My purchases**, so that I know what I can still book.
6. As a customer, I want to open a case and see its purchase terms and credits, so that I know what I'm entitled to.
7. As a customer, I want my purchase terms to stay as they were when I bought, so that later price changes don't affect me.
8. As an admin, I want unpublishing and deleting refused while an offering has purchases, so that purchased data is never lost.
9. As a developer, I want one finished example of data a customer owns, with a customer route, scoped queries and ownership tests, so that later use cases follow one pattern.

## Requirements

### Requirement: Buy a package

UC-03 steps 1–7, without step 7's hint (decision 2). One dialog serves the card and the detail page ([D-05](../decisions.md#catalog)): the package picker, then the confirmation. The request names the offering, the package, and the price and session count the customer confirmed (decision 3).

#### Scenario: Buy from the detail page

- **GIVEN** Customer 1 is logged in
- **WHEN** they open Career Coaching, click **Buy**, pick 3 sessions and confirm
- **THEN** the case detail opens with Career Coaching, "Not assigned yet", 3 of 3 credits open, the price paid CHF 450.00, 3 × 60 min, free cancellation up to 24 h before a session, and the purchase time in Zurich time
- **AND** `POST /api/customer/consultation-cases` answered 201 with a `Location` header

#### Scenario: Buy from the card

- **WHEN** Customer 1 clicks **Buy** on Career Coaching's card in the shop
- **THEN** the same dialog opens, and buying works as from the detail page

#### Scenario: The confirmation states the price

- **WHEN** the customer picks 5 sessions and continues
- **THEN** the dialog asks "Do you really want to buy Career Coaching, 5 sessions for CHF 700.00?"

#### Scenario: The customer changes their mind

- **WHEN** the customer closes the dialog, at either step
- **THEN** no case is created

#### Scenario: Buying twice

- **WHEN** Customer 1 buys Career Coaching twice
- **THEN** they have two cases ([UC-03](../use-cases/uc-03-purchase-consultation-package.md#business-rules))

#### Scenario: The offering is no longer available

- **GIVEN** the dialog is open on a published offering
- **WHEN** an admin unpublishes or archives it, and the customer then confirms
- **THEN** the response is 409 with code `OFFERING_NOT_AVAILABLE`, the dialog says "This offering is no longer available.", and no case is created
- **AND** an unknown offering id answers 404

#### Scenario: The package changed

- **GIVEN** the customer is on the confirmation for 3 sessions at CHF 450.00
- **WHEN** an admin changes that package's price or session count, or removes it, and the customer then confirms
- **THEN** the response is 409 with code `PACKAGE_CHANGED`, the dialog shows the message and the current packages, and no case is created

#### Scenario: Invalid request

- **WHEN** the request has no package id, a price of −1 or 12.345, or a session count of 0
- **THEN** the response is 400 with a `ProblemDetail` that names each invalid field

### Requirement: Buy for visitors and customers only

UC-02's rule for **Buy** and UC-01's login from **Buy**. After the login, the visitor is back on the page and clicks **Buy** again (decision 4).

#### Scenario: A visitor clicks Buy

- **GIVEN** a visitor on Career Coaching's detail page
- **WHEN** they click **Buy** and log in as Customer 1
- **THEN** they're back on Career Coaching's detail page, with the dialog closed
- **AND** the same goes for **Buy** on a card in the shop, which returns to the shop

#### Scenario: Logged in without CUSTOMER

- **WHEN** Admin 1 opens the shop or an offering's detail page
- **THEN** there's no **Buy** button

#### Scenario: Only customers buy

- **WHEN** a user without `CUSTOMER` calls `POST /api/customer/consultation-cases`
- **THEN** the response is 403, and 401 without a login

### Requirement: Purchase terms are copied

[D-08](../decisions.md#catalog): price paid, session count, session duration and cancellation deadline are copied into the case. Name and image are read live. The case keeps no reference to the package (decision 11).

#### Scenario: The offering changes after the purchase

- **GIVEN** Customer 1 bought 3 sessions of an offering for CHF 450.00
- **WHEN** an admin changes that package's price and session count, the session duration and the deadline, and saves
- **THEN** the case still shows CHF 450.00, 3 sessions, the old duration and the old deadline

#### Scenario: The package is removed after the purchase

- **WHEN** an admin removes the package a customer bought and saves
- **THEN** the save succeeds, and the case is unchanged

#### Scenario: Name and image are live

- **WHEN** an admin renames the offering or changes its image
- **THEN** the case and its card show the new name and image
- **AND** after the offering is archived, the case still opens, with its name and image

### Requirement: My purchases

UC-05's landing page, for cases. Every case is `ACTIVE` with open credits and no appointment, so the page has one group, **Needs booking**, sorted by purchase time (decision 1). UC-01's landing page for customers (decision 5).

#### Scenario: The customer's cases

- **GIVEN** Customer 1 bought two packages
- **WHEN** they open **My purchases**
- **THEN** the page shows a **Needs booking** group with two case cards, the older purchase first, each with the offering's name and image (or the placeholder), "Not assigned yet", the open and total credits, and the purchase date
- **AND** a card opens the case detail

#### Scenario: No purchases

- **WHEN** Customer 2, who bought nothing, opens **My purchases**
- **THEN** the page says "You haven't bought anything yet." and links to the shop

#### Scenario: Header and landing page

- **WHEN** Customer 1 or Expert 1 logs in from the header
- **THEN** **My purchases** opens, and the header shows **My purchases**
- **AND** Admin 1 and Expert 2 still land on **Consultations**, and their header has no **My purchases**

### Requirement: Only the owner sees a case

[Security](../architecture/security.md#ownership-in-use-cases): ownership is a scoped query, and a case of another customer is a 404, like an unknown one.

#### Scenario: Another customer's case

- **GIVEN** Customer 1 has a case
- **WHEN** Customer 2 calls `GET /api/customer/consultation-cases/{id}` for it, or opens `/my/cases/{id}`
- **THEN** the response is 404, and the page says "Case not found"
- **AND** Customer 2's `GET /api/customer/consultation-cases` doesn't contain it

#### Scenario: Customer routes are for customers

- **WHEN** a user without `CUSTOMER` calls a route under `/api/customer`, or opens `/my`
- **THEN** the route answers 403 (401 without a login), and the page shows "Page not found"

### Requirement: No unpublish or delete with purchases

[D-07](../decisions.md#catalog) and UC-16's error flow, left for this increment by spec 02 (decision 5). The status check comes first; the purchase check only applies to a change the status allows. The admin's page shows the 409's message, as it does for every 409 (decision 6).

#### Scenario: Unpublish with purchases

- **GIVEN** a published offering with a case
- **WHEN** the admin unpublishes it
- **THEN** the response is 409 with code `OFFERING_HAS_PURCHASES`, the page shows "The offering has purchases. Archive it instead.", and the offering stays published

#### Scenario: Archive with purchases

- **WHEN** the admin archives a published offering with a case
- **THEN** it's archived, and the case still opens for its customer

#### Scenario: Delete with purchases

- **GIVEN** a draft with a case (only possible by writing to the database directly, so this check is a safety net)
- **WHEN** the admin deletes it
- **THEN** the response is 409 with code `OFFERING_HAS_PURCHASES`, and nothing is deleted

#### Scenario: Purchase and unpublish at once

- **GIVEN** a published offering
- **WHEN** a customer buys a package while an admin unpublishes it
- **THEN** exactly one of them succeeds, the other gets 409, and the offering is never a draft with a case

## Decisions

Agreed with the product owner while planning (2026-11-09): decisions 1 to 10, each as recommended. The ones marked *planning* were taken while writing the plan, following the docs; object to them before the first task starts.

1. **One group on My purchases.** Without appointments and cancellations, every case is `ACTIVE` with open credits and nothing booked, so **My purchases** shows only **Needs booking**. Upcoming arrives with UC-06, Past with UC-08.
2. **No Book button and no "Book your first session" hint** until UC-06, so there are no buttons that do nothing. The case detail shows what a case holds now: offering, purchase terms, purchase time, "Not assigned yet", and credits.
3. **The server checks the confirmed terms.** The request carries the price and session count the dialog showed. If the package no longer has them, or no longer exists, the answer is 409 `PACKAGE_CHANGED`, and the dialog shows the current packages. [UC-03](../use-cases/uc-03-purchase-consultation-package.md) now says so. Buying silently at a new price would break the cross-cutting rule that the confirmation states the price, and it matters once money moves (MVP).
4. **Login from Buy returns to the page**, and the customer clicks **Buy** again. The dialog doesn't reopen by itself, as [UC-01](../use-cases/uc-01-login.md#alternative-and-error-flows) describes.
5. **Landing page until the expert and admin landing pages exist:** a user with `ADMIN` lands on **Consultations**, otherwise a user with `CUSTOMER` on **My purchases**, otherwise on the shop. So Expert 1 (expert and customer) lands on **My purchases** until UC-11 exists. UC-01 now says so.
6. **The admin's page doesn't hide Unpublish or Delete** when an offering has purchases. The 409 and its message are enough.
7. **A published offering without eligible experts stays buyable**, as spec 02 decided ([UC-16](../use-cases/uc-16-manage-consultation-offering.md#business-rules)). Its customers can't book until an expert is eligible. The booking increment decides what the customer sees then.
8. **`ConsultationCase` in code, `consultation_case` in the database**, routes under `/api/customer/consultation-cases`. `case` is reserved in SQL and a Java keyword, and a class named `Case` reads like part of a use case's name. The [glossary](../glossary.md#purchases) says so, as it does for `ConsultationPackage`.
9. **The purchase locks the offering's row first**, like every change to an offering ([D-27](../decisions.md#architecture), extended), and checks under the lock that the offering is published. Unpublish and delete check for purchases under the same lock, so a purchase and an unpublish can't both pass.
10. **No seed cases.** Tests buy through the API, and the demo buys through the UI.
11. **No reference to the package** (*planning*). The case copies the purchase terms, so it needs no package. UC-16 lets an admin remove a package of a published offering, which a foreign key from the case would turn into a 500.
12. **Only what exists is stored** (*planning*). The case has no status, no assigned expert and no cancellation columns yet: every case is `ACTIVE` and unassigned until UC-06 and UC-08 add them. Open credits equal the session count until appointments and credit adjustments exist.
13. **The request names the offering and the package** (*planning*), so the purchase can lock the offering before it reads the package (D-27's order) and check that the package is the offering's.
14. **The first `Clock` bean** (*planning*): the purchase time comes from the injected `java.time.Clock`, as AGENTS.md requires. It's `Clock.system(ZoneId.of("Europe/Zurich"))`: instants don't depend on the zone, and later rules that need local dates get the business time zone.
15. **Executed as in [D-33](../decisions.md#scope-and-docs):** task 01 first, as the pattern for customer-owned data; then 02, 03 and 04 in parallel, each in its own git worktree. They touch different files.

## Out of scope

- Booking, the **Book appointment** button and the "Book your first session" hint: UC-06 (decision 2).
- The assigned expert, appointments, the Upcoming group and the call link: UC-06, UC-07, UC-10.
- Cancelling a case and the Past group: UC-08.
- Notes: UC-15.
- Enrollments, training purchases and the enrollment cards: UC-04, UC-09, UC-17.
- The admin's case list and credit adjustments: UC-18. The expert's cases: UC-11.
- Reopening the dialog after the login (decision 4).
- Payment, invoices and receipts: MVP.
- An availability check before purchase: [future list](../use-cases/README.md#future-not-planned).

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

1. As a visitor, open `http://localhost:5173` and click **Buy** on Career Coaching's card: the login page opens. Pick Customer 1: the shop opens again, and the header shows **My purchases**.
2. Click **Buy** on the card: the dialog lists 3 sessions for CHF 450.00 and 5 sessions for CHF 700.00. Pick 3 sessions and continue: "Do you really want to buy Career Coaching, 3 sessions for CHF 450.00?" Close the dialog: **My purchases** is still empty ("You haven't bought anything yet.").
3. Open Career Coaching's detail page, click **Buy**, pick 3 sessions, continue and confirm: the case detail opens with "Not assigned yet", 3 of 3 credits open, CHF 450.00, 3 × 60 min, 24 h and the purchase time (Zurich time).
4. Click **My purchases**: one card under **Needs booking**.
5. In a private window, log in as Admin 1, open Career Coaching and change the 3-session package to CHF 500.00. Save. Customer 1's case still says CHF 450.00.
6. As Customer 1, click **Buy** on Career Coaching, pick 3 sessions and continue: the dialog says CHF 500.00. Leave it open. As Admin 1, set the price back to CHF 450.00 and save. As Customer 1, confirm: the dialog says the package has changed and shows CHF 450.00 again. Close it.
7. As Admin 1, click **Unpublish** on Career Coaching and confirm: "The offering has purchases. Archive it instead." It stays published. (Don't archive it: the demo database would keep it archived until `docker compose down -v`.)
8. Log out, log in as Customer 2: **My purchases** says "You haven't bought anything yet." Paste the URL of Customer 1's case: "Case not found".
9. Log out, log in as Admin 1: the shop has no **Buy** buttons. Log out, log in as Expert 1: **My purchases** opens.

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

10. Open `http://localhost:8080`, log in as Customer 2, buy 5 sessions of Career Coaching, and open **My purchases**.

## Risks

| Risk | Mitigation |
|---|---|
| A foreign key from the case to the package would make removing a bought package a 500. | The case copies the terms and keeps no package reference (decision 11). A REST test removes a bought package. |
| A customer could see another customer's case. | Every case query is scoped to the customer in SQL. The ownership tests use both seeded customers, on the list and the detail. |
| The purchase and the unpublish race, leaving a draft with a case. | Both lock the offering's row first (decision 9). A concurrency test makes the race certain, as `ConsultationOfferingConcurrencyTest` does, and fails without the purchase's lock. |
| Purchases of one offering wait for each other at its row lock. | Fine at PoC scale. A shared lock for purchases would let them run side by side, if it's ever needed. |
| Tests now buy, and their cases refer to the seed offering and the seed users. | The cleanup script deletes every case before the test offerings (the seed has none). Tests never change seed rows. |
| The first instant in the UI: a wrong time zone shows wrong dates around midnight and the summer time change. | `lib/date.ts` formats in `Europe/Zurich` with `@date-fns/tz`, with Vitest tests in summer and winter time and around midnight UTC. |
| The card's link stretches over the whole card and could swallow the **Buy** click. | Task 03 lifts the button above the link and checks the click in the browser. |
| Task 01 is the largest: a new feature from schema to page. | It sets the pattern for customer-owned data, as task 03 of increment 02 did for writes; it gets the most careful review. The list, the card button, visitors and the guard are separate tasks. |
