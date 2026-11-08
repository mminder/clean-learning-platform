# 06 Publish, unpublish, archive, delete

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [05](05-eligible-experts.md) |
| Status | Todo |

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

- [ ] On the dev server, demo steps 5–7 of the [spec](../../specs/02-manage-consultation-offerings.md#demo) work: publish, unpublish, publish, archive; publishing an incomplete offering shows what's missing; delete removes a draft (Playwright).
- [ ] Each status change asks first, and cancelling changes nothing.
- [ ] Every cell of the plan's status table holds (domain tests), and the REST tests cover one allowed and one refused change per route.
- [ ] Publishing without a package or without an eligible expert answers 409 `OFFERING_INCOMPLETE`, and `detail` names what's missing.
- [ ] `PUT` on an archived offering answers 409 `WRONG_OFFERING_STATUS`.
- [ ] A deleted draft answers 404, and its packages and eligible experts are gone.
- [ ] The concurrency test passes with the lock and fails without it.
- [ ] `openapi.json` has `code` in `ProblemDetail`; the role matrix has the four routes; every check passes.

## Tests

- Domain: the status table, publishing completeness and its message
- REST: each route's happy path; `OFFERING_INCOMPLETE`; `WRONG_OFFERING_STATUS` for an edit of an archived offering and for deleting a published one; unknown id → 404; the shop after each change
- Authorization: `RoleMatrixTest` rows for the four routes
- Concurrency: the offering lock

## Out of scope

- The purchase guard (`OFFERING_HAS_PURCHASES`): with UC-03 (spec decision 5).
- The image: [07](07-offering-image.md) adds its routes to the status rules and its row to delete.

## Notes
