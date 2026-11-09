# 04 Purchase guard on unpublish and delete

| | |
|---|---|
| Increment | [03 Buy a consultation package](../../specs/03-buy-consultation-package.md) |
| Type | AFK |
| Blocked by | [01](01-buy-from-detail-page.md) |
| Status | Done |

## Goal

An admin can't unpublish or delete an offering that has purchases: the answer is 409 `OFFERING_HAS_PURCHASES`, and the page says to archive it instead. A purchase and an unpublish at the same moment can't both pass.

## What to build

Read first: [D-07](../../decisions.md#catalog), [D-27](../../decisions.md#architecture), UC-16's [error flows](../../use-cases/uc-16-manage-consultation-offering.md#alternative-and-error-flows), [backend.md](../../architecture/backend.md#concurrency), and the plan's [purchase guard](README.md#purchase-guard) section. `ConsultationOfferingConcurrencyTest` and task 06 of increment 02's [notes](../02-manage-consultation-offerings/06-offering-lifecycle.md#notes) show the concurrency test's pattern.

Work in this order, test first:

1. **A failing REST test:** Customer 1 buys a package of a published test offering; the admin's unpublish answers 409 `OFFERING_HAS_PURCHASES` with the plan's `detail`, and the offering stays published.
2. **Domain:** `unpublish(boolean hasPurchases)` and `requireDeletable(boolean hasPurchases)` on `ConsultationOffering`, and the code `OFFERING_HAS_PURCHASES`. The status comes first: an archived offering with purchases is still `WRONG_OFFERING_STATUS`. Remove the "the check comes with UC-03" remarks.
3. **Port and adapter:** `CheckOfferingPurchasesPort.hasPurchases(OfferingId)` in `application.offering.port`, implemented in `ConsultationOfferingAdapter` with an `exists` query.
4. **Use cases:** `UnpublishConsultationOfferingUseCase` and `DeleteConsultationOfferingUseCase` ask the port after the lock and pass the answer to the domain.
5. **Concurrency:** one test, a purchase against an unpublish of one published offering, as the plan's [tests](README.md#tests) section says. Check that it fails when the purchase doesn't lock the offering (for example, with the lock port's `find` made plain for the run).
6. **Frontend:** nothing to build. Check in the browser that the page shows the 409's `detail` and reloads (spec decision 6).
7. **Docs:** [backend.md](../../architecture/backend.md#errors)'s 409 examples already name "offering has purchases"; check that UC-16's message matches the plan's `detail` (aligned while planning). Increment 02's plan is a record and stays as it is.

## Acceptance criteria

- [x] Unpublishing a published offering with a case answers 409 `OFFERING_HAS_PURCHASES`, "The offering has purchases. Archive it instead.", and the offering stays published; without a case it's still 200.
- [x] Deleting a draft with a case (inserted with `@Sql`) answers 409 `OFFERING_HAS_PURCHASES`, "The offering has purchases and can't be deleted.", and nothing is deleted.
- [x] Archiving a published offering with a case answers 200, and the case still opens for its customer.
- [x] Every earlier cell of the status table still holds, and an archived or draft offering's refusals keep `WRONG_OFFERING_STATUS` with or without purchases (domain tests).
- [x] The concurrency test passes with the purchase's lock and fails without it.
- [x] On the dev server, after Customer 1 buys Career Coaching, Admin 1's **Unpublish** shows "The offering has purchases. Archive it instead." above the form, and Career Coaching stays published (Playwright).
- [x] Every check passes.

## Tests

- Domain: unpublish and delete with and without purchases, in every status
- REST: unpublish and delete refused with purchases; archive allowed with purchases
- Concurrency: purchase against unpublish

## Out of scope

- Hiding or disabling **Unpublish** and **Delete** when there are purchases (spec decision 6).
- Showing the number of purchases in the admin's list: not in UC-16.

## Notes

- **Domain:** `unpublish(boolean hasPurchases)` and `requireDeletable(boolean hasPurchases)` check the status first, then the purchases, and throw `OFFERING_HAS_PURCHASES` (a constant on `ConsultationOffering`) with the plan's `detail`s. `ConsultationOfferingStatusTest` runs each refusal by status with and without purchases, and adds the two purchase cells (unpublish a published offering, delete a draft).
- **Port and adapter:** `CheckOfferingPurchasesPort.hasPurchases(OfferingId)`; `ConsultationOfferingAdapter` implements it with `ConsultationOfferingRepository.existsCase`, a JPQL `select exists (select c.id from ConsultationCaseEntity c where …)` over the consultation feature's entity (D-23). The use cases lock, ask the port, then call the domain, so the question runs even when the status refuses: one cheap query, and the flow stays the plan's.
- **REST tests** in `ConsultationOfferingLifecycleTest`: unpublish refused after Customer 1 buys through the API, with the whole body, and the offering stays published and in the shop; delete refused for a draft with a case from `api/offering/draft-with-a-case.sql` (the API can't make one), with the case and the offering's package still there; archive allowed with a case, and the case still opens for Customer 1. Unpublish without a case is the existing happy path.
- **Concurrency:** `ConsultationOfferingConcurrencyTest.aPurchaseAndAnUnpublishAtOnceCantBothPass`, next to the archive-and-unpublish race, whose helpers it shares. The offering's row lock alone isn't enough here: a purchase without its lock would never wait for it and simply commit first, which passes. So the test also holds Agile Coaching's `consultation_offering` row `for update`, which the new case's foreign key check (`for key share`) waits for. The purchase starts first and waits (at the offering with its lock, at the foreign key without it), then the unpublish waits at the offering. The test lets go of the offering, waits until the unpublish is done or waits for the purchase (`pg_blocking_pids`), and only then lets go of the foreign key. It asserts exactly one success and one 409, and that the offering is published with one case or a draft with none. Checked: with `ConsultationCaseAdapter.lock`'s `find` made plain, it failed three runs out of three with `[201, 200]`; with the unpublish asking for purchases before it locks, also three out of three with `[201, 200]`; with the code as it is, it passed five runs out of five and in `./mvnw verify`. backend.md's concurrency section has a sentence on the second lock.
- **Browser check** (Playwright with the installed Chrome, against the jar with the `poc` profile on its own port and Postgres container, not the dev server, to stay off the ports other sessions use): Customer 1 bought Career Coaching, 3 sessions, and the case opened; Admin 1's **Unpublish** and its confirmation showed "The offering has purchases. Archive it instead." under the status buttons, above the form; the page still showed Published with **Unpublish** and **Archive**, and the shop still listed Career Coaching. The only answers >= 400 were the two `GET /api/me` 401s before logging in and the intended 409. The container was removed afterwards.
- **Docs:** UC-16's error flow already had both messages, and backend.md's 409 examples name "offering has purchases"; nothing else changed.
