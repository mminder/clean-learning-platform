# 04 Purchase guard on unpublish and delete

| | |
|---|---|
| Increment | [03 Buy a consultation package](../../specs/03-buy-consultation-package.md) |
| Type | AFK |
| Blocked by | [01](01-buy-from-detail-page.md) |
| Status | Todo |

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

- [ ] Unpublishing a published offering with a case answers 409 `OFFERING_HAS_PURCHASES`, "The offering has purchases. Archive it instead.", and the offering stays published; without a case it's still 200.
- [ ] Deleting a draft with a case (inserted with `@Sql`) answers 409 `OFFERING_HAS_PURCHASES`, "The offering has purchases and can't be deleted.", and nothing is deleted.
- [ ] Archiving a published offering with a case answers 200, and the case still opens for its customer.
- [ ] Every earlier cell of the status table still holds, and an archived or draft offering's refusals keep `WRONG_OFFERING_STATUS` with or without purchases (domain tests).
- [ ] The concurrency test passes with the purchase's lock and fails without it.
- [ ] On the dev server, after Customer 1 buys Career Coaching, Admin 1's **Unpublish** shows "The offering has purchases. Archive it instead." above the form, and Career Coaching stays published (Playwright).
- [ ] Every check passes.

## Tests

- Domain: unpublish and delete with and without purchases, in every status
- REST: unpublish and delete refused with purchases; archive allowed with purchases
- Concurrency: purchase against unpublish

## Out of scope

- Hiding or disabling **Unpublish** and **Delete** when there are purchases (spec decision 6).
- Showing the number of purchases in the admin's list: not in UC-16.

## Notes
