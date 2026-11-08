# 04 Packages

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [03](03-create-and-edit-consultation-offering.md) |
| Status | Todo |

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

- [ ] On the dev server, Admin 1 adds the packages 1 × CHF 160 and 4 × CHF 600 to "Agile Coaching", saves and reloads: both are there, fewest sessions first (Playwright).
- [ ] An edit keeps the ids of the packages it keeps (REST test, as in step 1).
- [ ] A package with 0 sessions, or a price of −1, 12.345 or 123456789 answers 400 with `packages[i].sessionCount` or `packages[i].price` in `errors`, and the form shows the message on that row.
- [ ] A package id of another offering answers 400, and so does any package id on `POST`.
- [ ] Removing every package of a published offering is saved, and the offering disappears from the shop list and detail ([UC-16](../../use-cases/uc-16-manage-consultation-offering.md#business-rules)).
- [ ] A changed price shows in the shop at once.
- [ ] Every check passes.

## Tests

- Domain: matching packages by id (kept, changed, new, dropped, an id of another offering)
- REST: create with packages; edit as in step 1; invalid session count and price; foreign id; the last package removed from a published offering
- No new routes, so no new role matrix rows

## Out of scope

- Rules on duplicate session counts (spec decision 10).
- Sorting packages by hand: the shop and the admin both sort by session count.

## Notes
