# 04 Packages

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [03](03-create-and-edit-consultation-offering.md) |
| Status | Done |

## Goal

An admin adds, changes and removes an offering's packages in the same form and with the same **Save**. A package keeps its id across edits, so UC-03 can later buy a package by its id.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) (packages, and removing them from a published offering), [UC-02](../../use-cases/uc-02-browse-shop.md#business-rules) (a published offering without packages is hidden), and the plan's [API](README.md#api) and [writes](README.md#writes) sections. Follow the pattern from [03](03-create-and-edit-consultation-offering.md).

Work in this order, test first:

1. **A failing REST test:** an offering with three packages; a `PUT` changes the first, leaves out the second and adds a new one. The response has the first one's id with the new values, no second one, and a new id for the new one.
2. **Domain**, test first: `ConsultationOffering` gets its packages, and the edit matches them by id as the plan says. A package id that isn't one of the offering's is an `IllegalArgumentException` (400). A new offering may already have packages, but none with an id.
3. **Adapter:** `ConsultationOfferingAdapter` loads the packages and writes only the difference: it deletes the dropped packages, updates the changed ones and inserts the new ones.
4. **API:**
   - `ConsultationPackageRequest` (`id` optional, `sessionCount` `@NotNull @Positive`, `price` with the plan's pattern and message) as a `@Valid @NotNull` list in `ConsultationOfferingRequest`;
   - `packages` in `ConsultationOfferingResponse`, fewest sessions first, then id;
   - `ConsultationPackageResponse` moves from `api.shop.response` to `api.response`, since the shop and the admin both return it; the shop's contract doesn't change.
5. **Frontend:** a packages section in `ConsultationOfferingForm` with `useFieldArray`: one row per package with Sessions, Price (CHF) and **Remove**, and **Add package** below. A row's id travels with it unseen. Field errors land on the row's fields (`packages.1.price`). A new row's price input starts empty, not at 0.

## Acceptance criteria

- [x] On the dev server, Admin 1 adds the packages 1 × CHF 160 and 4 × CHF 600 to "Agile Coaching", saves and reloads: both are there, fewest sessions first (Playwright).
- [x] An edit keeps the ids of the packages it keeps (REST test, as in step 1).
- [x] A package with 0 sessions, or a price of −1, 12.345 or 123456789 answers 400 with `packages[i].sessionCount` or `packages[i].price` in `errors`, and the form shows the message on that row.
- [x] A package id of another offering answers 400, and so does any package id on `POST`.
- [x] Removing every package of a published offering is saved, and the offering disappears from the shop list and detail ([UC-16](../../use-cases/uc-16-manage-consultation-offering.md#business-rules)).
- [x] A changed price shows in the shop at once.
- [x] Every check passes.

## Tests

- Domain: matching packages by id (kept, changed, new, dropped, an id of another offering)
- REST: create with packages; edit as in step 1; invalid session count and price; foreign id; the last package removed from a published offering
- No new routes, so no new role matrix rows

## Out of scope

- Rules on duplicate session counts (spec decision 10).
- Sorting packages by hand: the shop and the admin both sort by session count.

## Notes
- **Domain:** `ConsultationOffering` gained `packages`, and `create` and `edit` take `ConsultationPackageEntry`s: a record with `Optional<ConsultationPackageId>` (`kept(id, …)` and `added(…)`), because the domain can't use JSpecify's `@Nullable` (ArchUnit). `ConsultationOfferingCommand` carries the entries. `matchPackages` is the one place that matches by id; a package named twice is also an `IllegalArgumentException` (it would be a primary key violation, a 500). The plan's table lists `ConsultationPackageEntry`.
- **Sorting** is an invariant of `ConsultationOffering` (the constructor sorts), so the `PUT` response, built from the edited domain object, has the same order as a later `GET`. It compares the ids as text: `UUID.compareTo` compares signed numbers, so an id starting with `8`–`f` would sort differently from the database's `order by id` that the shop uses. A domain test covers it.
- **Adapter:** loads the packages as managed `ConsultationPackageEntity`s with a query in `ConsultationOfferingRepository`; `update` removes the dropped, sets the fields of the kept (Hibernate writes a row only if a value changed) and persists the new ones. The REST test proves the difference-only write with the packages' own timestamps: a kept, unchanged package's `updated_at` doesn't move, a changed one's does, and no kept `created_at` changes. `ConsultationPackageEntity` gained a constructor, getters and setters; backend.md's persistence section says how child rows are written.
- **Request validation:** the plan said one `@Pattern`; a missing price passes a pattern, and `Money.chf(null)` would be a 500, so the price is `@NotNull @Pattern`, and each entry of `packages` is `@NotNull @Valid` (a `null` entry would be a 500 too). The plan says so. The pattern is `\d{1,8}(\.\d{1,2})?`; `\d` is ASCII-only in Java. REST tests cover 0 sessions, −1, 12.345, 123456789, an empty string, `1e2`, `12,5`, nulls, and the largest and smallest valid prices.
- **`PUT` replaces the packages:** an existing test (`editsOfAPublishedOfferingShowInTheShopAtOnce`) sent `"packages": []` and so emptied the offering, which then left the shop. It now sends the offering's two packages by id. Every later task's `PUT` test has to send the packages (and from 05 the experts) it wants to keep.
- **Frontend:** `useFieldArray` in `ConsultationOfferingForm`; the row's `id` stays in the form's values, and `fields[i].id` is only the key (checked in the browser: the PUT body has the id for kept rows and none for new ones). After a save the form resets to the *saved offering* (`reset(toRequest(saved))`, so `onSave` now resolves with it): resetting to the submitted values would drop the ids of new rows, and the next save would insert them again. `toRequest` is in `queries.ts`, with a Vitest test. The price is a text input (`inputMode="decimal"`), a number input would turn `160.50` into a float. An empty list shows "No packages. An offering without packages is not in the shop." frontend.md's Forms section has the pattern.
- **Browser check** (Playwright, dev server, `poc` backend): Admin 1 adds two rows to a new offering, with 0 sessions and 12.345 in the second: "must be greater than 0" and the price message show under that row's fields only; fixed to 1 × 160 and 4 × 600, saved, reloaded: 1 × 160.00 and 4 × 600.00, fewest first. On the edit page: the first row changed to 2 × 200, the second removed, a third added (3 × 450): the PUT body names only the first row's id, the database keeps that id, drops the other and has a new one; saving again without a reload names the new package's id too and the ids stay. An error on the second of two rows stays with its row after the first is removed. The offering published through SQL (06 has no UI yet): the shop shows "from CHF 300.00" after the first package, "from CHF 250.50" after a price change, and no longer lists it after the last package is removed; its detail page says "Offering not found". The only answers ≥ 400 were the first `GET /api/me` (401), the intended 400, and that 404. The dev database was cleaned up afterwards, Career Coaching untouched.
