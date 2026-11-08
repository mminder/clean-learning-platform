# 02 Consultation offering list

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [01](01-poc-login.md) |
| Status | Done |

## Goal

An admin opens **Consultations** from the header and sees every consultation offering with its status, its number of packages and its number of eligible experts. The admin pages get their guard, and admins get their landing page.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) step 1, the plan's [API](README.md#api) and [auth in the SPA](README.md#auth-in-the-spa) sections, and how [06 of increment 01](../01-shop-tracer-bullet/06-shop-list.md) built the shop list.

Work in this order, test first:

1. **A failing REST test:** Admin 1 calls `GET /api/admin/consultation-offerings` and gets Career Coaching as `PUBLISHED` with 2 packages and 2 eligible experts.
2. **Domain:** `OfferingStatus` and `ConsultationOfferingSummary` (id, name, status, package count, eligible expert count).
3. **Status in the entity:** `OfferingEntity.status` becomes `OfferingStatus` (`@Enumerated(EnumType.STRING)`), and the shop's queries compare against the enum. The shop tests stay green unchanged.
4. **Port and adapter:** `LoadConsultationOfferingSummariesPort`, implemented by the new `ConsultationOfferingAdapter`. One query (no N+1) with both counts, sorted by name, then id. Mind that two joins multiply each other's rows; count in subqueries or count distinct.
5. **Use case and controller:** `ListConsultationOfferingsUseCase` (`@Transactional(readOnly = true)`) and `ConsultationOfferingController` with the route and response from the plan. Regenerate the contract, and add a role matrix row.
6. **Frontend:**
   - `features/offering/queries.ts` with `useConsultationOfferings()`;
   - `ConsultationOfferingsPage` at `/admin/consultations`: the heading "Consultations" and a table with the columns Name, Status (a badge: Draft, Published, Archived), Packages and Experts; loading, error with retry, and the empty state "No consultation offerings yet.";
   - `RequireRole` around the admin route, as in the plan;
   - **Consultations** in the header for users with `ADMIN`;
   - `landingPath` sends users with `ADMIN` to `/admin/consultations` (spec decision 3);
   - a 401 from any query or mutation other than `useMe` refetches `['me']`, so an expired session ends on the login page.

   The names aren't links yet; the detail page comes in [03](03-create-and-edit-consultation-offering.md).

## Acceptance criteria

- [x] Logging in as Admin 1 opens the list: Career Coaching, Published, 2 packages, 2 experts (Playwright). Expert 2, who has `EXPERT` and `ADMIN`, lands there too.
- [x] Draft and archived offerings are listed, sorted alphabetically by name (REST test with `@Sql` and the cleanup from the plan's [tests](README.md#tests)).
- [x] A visitor who opens `/admin/consultations` gets the login page and returns to the list after logging in as Admin 1.
- [x] Customer 1 at `/admin/consultations` sees "Page not found", and the header shows **Consultations** only to admins.
- [x] The role matrix has the new route, and every check passes.

## Tests

- Domain: `ConsultationOfferingSummary`, if it gets a rule
- REST: the seeded offering with its counts; every status, sorted by name
- Authorization: a `RoleMatrixTest` row for `GET /api/admin/consultation-offerings`
- Frontend: `landingPath` with `ADMIN`

## Out of scope

- The detail page, **New offering** and links from the list ([03](03-create-and-edit-consultation-offering.md)).
- Filtering or paging the list: the PoC has a handful of offerings.

## Notes

- **Logout from a protected page** (finding of task 01's review): `useLogout` no longer resets the query cache. `Layout` navigates to `/` and resets once the new location has rendered (an effect on the location key), because the reset empties `['me']` and `RequireRole` on the page being left would send the visitor to `/login?redirect=…`. Navigation runs in a React transition, so calling the reset right after `navigate` could race. Checked in the browser from the list and from the not-found page.
- **`ConsultationOfferingSummary` has a rule:** the counts can't be negative (`IllegalArgumentException`), so it has a domain test.
- **Tests reuse the shop's `@Sql` scripts** (`draft-and-archived-offerings.sql`, `more-published-offerings.sql`, `more-experts-and-packages.sql` and their cleanups) by absolute classpath. The shared cleanup script of the plan's [tests](README.md#tests) still comes with 03.
- **The shadcn CLI worked** from this container: `npx shadcn@latest add table`.
- **The 401 rule reads `ApiError`:** `queryClient.ts` reacts to an `ApiError` with status 401. A hook that doesn't throw one for a non-2xx answer wouldn't trigger the refetch; every hook so far does.
