# 06 Publish, unpublish, archive, delete

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [05](05-eligible-experts.md) |
| Status | Done |

## Goal

The admin publishes, unpublishes, archives and deletes consultation offerings, each after a confirmation. The status rules answer 409 with a stable code, an archived offering is read-only, and concurrent status changes can't both pass.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) step 5 and its business rules, [D-07](../../decisions.md#catalog), [D-27](../../decisions.md#architecture), [D-34](../../decisions.md#architecture), and the plan's [errors](README.md#errors), [writes](README.md#writes) and [confirmations](README.md#confirmations) sections.

Work in this order, test first:

1. **A failing REST test:** publishing a draft with a package and an eligible expert answers 200 with `PUBLISHED`, and the shop lists it.
2. **Domain**, test first:
   - `BusinessRuleException` in `domain.shared`, with a code and a message for the user;
   - on `ConsultationOffering`: `publish()`, `unpublish()`, `archive()` and a delete check, following the plan's status table; publishing checks completeness and names what's missing; an edit of an archived offering is refused.

   Test every cell of the status table.
3. **Ports and adapter:** `DeleteConsultationOfferingPort`, deleting in the order the plan's [writes](README.md#writes) section gives.
4. **Use cases:** `PublishConsultationOfferingUseCase`, `UnpublishConsultationOfferingUseCase`, `ArchiveConsultationOfferingUseCase` and `DeleteConsultationOfferingUseCase`, each locking the offering first, as `EditConsultationOfferingUseCase` does.
5. **API:**
   - the four routes on `ConsultationOfferingController`; the three status changes answer with `ConsultationOfferingResponse`, delete with 204;
   - `ApiExceptionHandler` maps `BusinessRuleException` to 409 with `code`, and the `ProblemDetail` schema gets `code`;
   - role matrix rows, following the plan's rule for mutating rows.
6. **Concurrency:** one test for the lock, as in the plan's [tests](README.md#tests). Make it deterministic, for example by holding the row lock in the test's own transaction while a request waits for it. Check that it fails without the lock.
7. **Frontend:**
   - `OfferingStatusActions` on `ConsultationOfferingPage`: **Publish** and **Delete** for a draft, **Unpublish** and **Archive** for a published offering, none for an archived one;
   - each opens `ConfirmDialog` (shadcn `alert-dialog`) with the plan's title and text; **Cancel** changes nothing;
   - a 409 shows its `detail` above the form and refetches the offering;
   - an archived offering's page is read-only: disabled fields, no **Save**, and a line saying it can't be changed;
   - after **Delete**, the list opens.
8. **Docs:** [AGENTS.md](../../../AGENTS.md) and [backend.md](../../architecture/backend.md#concurrency) already list the offering lock (planned with this increment); check that they match what you built.

## Acceptance criteria

- [x] On the dev server, demo steps 5–7 of the [spec](../../specs/02-manage-consultation-offerings.md#demo) work: publish, unpublish, publish, archive; publishing an incomplete offering shows what's missing; delete removes a draft (Playwright).
- [x] Each status change asks first, and cancelling changes nothing.
- [x] Every cell of the plan's status table holds (domain tests), and the REST tests cover one allowed and one refused change per route.
- [x] Publishing without a package or without an eligible expert answers 409 `OFFERING_INCOMPLETE`, and `detail` names what's missing.
- [x] `PUT` on an archived offering answers 409 `WRONG_OFFERING_STATUS`.
- [x] A deleted draft answers 404, and its packages and eligible experts are gone.
- [x] The concurrency test passes with the lock and fails without it.
- [x] `openapi.json` has `code` in `ProblemDetail`; the role matrix has the four routes; every check passes.

## Tests

- Domain: the status table, publishing completeness and its message
- REST: each route's happy path; `OFFERING_INCOMPLETE`; `WRONG_OFFERING_STATUS` for an edit of an archived offering and for deleting a published one; unknown id → 404; the shop after each change
- Authorization: `RoleMatrixTest` rows for the four routes
- Concurrency: the offering lock

## Out of scope

- The purchase guard (`OFFERING_HAS_PURCHASES`): with UC-03 (spec decision 5).
- The image: [07](07-offering-image.md) adds its routes to the status rules and its row to delete.

## Notes

- **Domain:** `ConsultationOffering` gained `publish()`, `unpublish()`, `archive()` (each returns the changed offering) and `requireDeletable()`, the delete check; `edit` refuses an archived offering. The codes are constants on `ConsultationOffering` (`OFFERING_INCOMPLETE`, `WRONG_OFFERING_STATUS`); `BusinessRuleException` (`domain.shared`) carries the code and a message for the user. The status comes first: an archived offering without packages answers `WRONG_OFFERING_STATUS`, not `OFFERING_INCOMPLETE`. The messages: "The offering is archived and can't be changed any more." for every change of an archived offering, "The offering is {a draft, published}. Only {a draft, a published offering} can be {published, unpublished, archived, deleted}." otherwise, and "The offering needs at least one package [and at least one eligible expert] before it can be published." `ConsultationOfferingStatusTest` covers every cell of the status table.
- **Use cases:** the three status changes lock, ask the domain and write through `SaveConsultationOfferingPort.update`, which also walks the unchanged packages and experts, so Hibernate writes only the `offering` row. Delete locks, calls `requireDeletable()` and then `DeleteConsultationOfferingPort.delete`. The adapter removes the managed entities in the order of the plan's writes section (eligible experts, packages, `consultation_offering`, `offering`); Hibernate deletes in the order of the `remove` calls.
- **409 in `openapi.json`:** `ApiExceptionHandler.handleBusinessRule` carries `@ResponseStatus(CONFLICT)`, so springdoc lists 409 on every route, `GET /api/me` and the shop included, like 400 and 404 before (task 03's note). Accepted noise; backend.md's errors section and the plan's errors section say so, and the plan's route table says which routes really answer 409. `ProblemDetailSchemaCustomizer` adds the optional `code`. operationIds: `publishConsultationOffering`, `unpublishConsultationOffering`, `archiveConsultationOffering`, `deleteConsultationOffering`.
- **REST tests** are in their own class, `ConsultationOfferingLifecycleTest` (the controller test has 800 lines): each route's happy path and a refused change, `OFFERING_INCOMPLETE` with the whole body, the read-only archived offering (`PUT`, every status change and delete answer 409), a deleted draft's rows and its 404, unknown and malformed ids, and the shop after each change.
- **Concurrency:** `ConsultationOfferingConcurrencyTest` archives and unpublishes one published offering at once. To make the race certain, it holds the offering's row lock in its own JDBC transaction while both requests start, waits until `pg_stat_activity` shows two transactions waiting for a lock, then commits: with the lock, both wait at the lock and one gets 409; without it, both read `PUBLISHED` and wait only at their update. Checked: with `lock()` changed to a plain `find`, the test failed three runs out of three with `[200, 200]`; with the lock it passed five runs out of five and in every build. backend.md's concurrency section describes the pattern for the later races.
- **Frontend:** `useChangeConsultationOfferingStatus(id)` (`publish`, `unpublish`, `archive`) and `useDeleteConsultationOffering(id)` in `queries.ts`. Every offering mutation, the edit included, refetches the offerings on a 409 (`isConflict`), so a stale page reloads. Delete doesn't refetch the deleted offering's detail, which would answer 404 before the list opens: it marks it stale and drops the list from the cache, so the list opens freshly loaded. `OfferingStatusActions` shows a 409's `detail` (`problemMessage`, new in `lib/problem.ts`, which `showSaveError` now uses too) under its buttons, above the form; the message stays when the reload shows the offering archived meanwhile. `ConfirmDialog` (`components/`) takes the trigger button as `children`; the confirm buttons of Archive and Delete are destructive. An archived offering's form wraps its fields in a disabled `<fieldset>` and has no **Save**; the page says "This offering is archived and can't be changed any more." shadcn: `alert-dialog` (the CLI asked to overwrite `button.tsx`; answered no, it skipped it as identical). frontend.md's Forms section has the 409, read-only and confirmation patterns.
- **Worth knowing, not changed:** a status change acts on the saved offering, so unsaved edits in the form stay unsaved after **Publish**. In the stale case the 409's message and the read-only line say nearly the same thing.
- **Browser check** (Playwright, dev server, `poc` backend): Admin 1 creates Agile Coaching (1 × 160, 4 × 600, Expert 2): a draft with **Publish** and **Delete** only. **Cancel** and Escape close the dialogs without a request. Publish (dialog title and text as in the plan): Published, and the shop shows Agile Coaching "from CHF 160.00" before Career Coaching. Unpublish: Draft, gone from the shop. Publish, Archive: Archived, the read-only line, every field, package row, checkbox and **Add package** disabled, no **Save**, no status buttons, not in the shop. "Test" without packages or experts: **Publish** shows "The offering needs at least one package and at least one eligible expert before it can be published." and stays a draft; **Delete** opens the list without it, and its old URL says "Offering not found". Stale pages, with a second tab: an offering archived in the other tab, then **Unpublish** here: "The offering is archived and can't be changed any more.", and the page reloads as Archived and read-only; the same for a **Save** of an edited form; a draft published in the other tab, then **Delete** here: "The offering is published. Only a draft can be deleted.", and the page reloads as Published and stays open. The only API answers >= 400 were the first `GET /api/me` (401), the intended 409s and the 404 of opening the deleted offering on purpose. The dev database was cleaned up afterwards; Career Coaching is as seeded (published, 2 packages, 2 experts).
