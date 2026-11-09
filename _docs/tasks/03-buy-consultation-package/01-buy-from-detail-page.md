# 01 Buy a package from the detail page

| | |
|---|---|
| Increment | [03 Buy a consultation package](../../specs/03-buy-consultation-package.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

A logged-in customer clicks **Buy** on an offering's detail page, picks a package, confirms the price, and lands on the new case's detail page. The case holds the purchase terms, and only its customer can open it. This task builds the `consultation` feature from the schema to the page, and sets the pattern for customer-owned data.

## What to build

Read first: [UC-03](../../use-cases/uc-03-purchase-consultation-package.md), the case detail of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-case-detail), [D-08](../../decisions.md#catalog), [D-27](../../decisions.md#architecture), [security.md](../../architecture/security.md#ownership-in-use-cases), and the plan's [packages and names](README.md#packages-and-names), [schema](README.md#schema), [API](README.md#api), [purchase](README.md#purchase), [ownership](README.md#ownership), [buy](README.md#buy), [pages](README.md#pages) and [dates](README.md#dates) sections.

Work in this order, test first:

1. **A failing REST test:** Customer 1 buys Career Coaching's 3-session package; `POST /api/customer/consultation-cases` answers 201 with a `Location`, and `GET` on it answers the case with CHF 450.00, 3 sessions, 60 minutes, 24 hours and 3 of 3 open credits.
2. **Schema:** `V6__create_consultation_case.sql`, as the plan's schema section says, with the `updated_at` trigger.
3. **`Clock`:** `ClockConfig` in `config` (spec decision 14).
4. **Domain**, test first:
   - `ConsultationCaseId` and `PurchaseTerms` in `domain.consultation.value`;
   - `ConsultationOfferingForPurchase` with `purchase(...)`, `OFFERING_NOT_AVAILABLE` and `PACKAGE_CHANGED`, following the plan's purchase table: the status first, then the package, its price and its session count;
   - `ConsultationCase`: the new case;
   - `CustomerConsultationCase`: the case as its customer sees it, with `totalCredits()` and `openCredits()` (spec decision 12).
5. **Ports and adapter:** `LockConsultationOfferingForPurchasePort`, `SaveConsultationCasePort`, `LoadCustomerConsultationCasePort`; `ConsultationCaseAdapter` with `ConsultationCaseEntity` and `CustomerConsultationCaseRow`. The lock is the same as `ConsultationOfferingAdapter.lock`'s. The case query is scoped to the customer and joins the offering in every status.
6. **Use cases:** `PurchaseConsultationPackageUseCase` (lock, decide, write, answer, as the plan's purchase section says) and `GetCustomerConsultationCaseUseCase` (404 for an unknown case or another customer's).
7. **API:**
   - `CustomerConsultationCaseController` with `POST` and `GET /{id}`, the request and the responses as the plan shows them, `ProblemResponses` per route;
   - `PurchaseConsultationPackageRequest` reuses `ConsultationPackageRequest`'s price pattern and message;
   - role matrix rows for both routes; drop the `GET /api/customer/probe` row;
   - regenerate `openapi.json`, then `npm run generate:api`.
8. **Test data:** rename `delete-test-offerings.sql` to `delete-test-data.sql` and `SeedData.DELETE_TEST_OFFERINGS` to `DELETE_TEST_DATA`, delete every case at its top, and update the tests that use it ([tests](README.md#tests)).
9. **Frontend:**
   - install `date-fns` and `@date-fns/tz`; `lib/date.ts` with `formatDate` and `formatDateTime`, test first;
   - shadcn `dialog` and `radio-group`;
   - `features/consultation/queries.ts`: `usePurchaseConsultationPackage`, `useCustomerConsultationCase`;
   - `BuyConsultationButton` and `BuyConsultationDialog` in `features/shop/`, on `OfferingDetailPage` under the description: shown only to users with `CUSTOMER` (visitors come with 03); the two steps, the error table and the navigation as the plan's buy section says;
   - `ConsultationCasePage` at `/my/cases/:id`, under `RequireRole role="CUSTOMER"`, as the plan's pages section shows it.
10. **Docs:** [frontend.md](../../architecture/frontend.md#dates-times-and-money) gets `lib/date.ts`'s two functions; check that [backend.md](../../architecture/backend.md#concurrency)'s lock table has the purchase (added while planning).

## Acceptance criteria

- [ ] On the dev server, Customer 1 buys 3 sessions of Career Coaching from its detail page: the dialog lists both packages, the confirmation says "Do you really want to buy Career Coaching, 3 sessions for CHF 450.00?", and the case detail opens with the plan's values and the purchase time in Zurich time (Playwright).
- [ ] Closing the dialog at either step creates no case.
- [ ] A visitor and Admin 1 see no **Buy** on the detail page.
- [ ] Buying twice creates two cases.
- [ ] An unpublished or archived offering answers 409 `OFFERING_NOT_AVAILABLE`; a changed price, a changed session count or a removed package answers 409 `PACKAGE_CHANGED`; an unknown offering answers 404; each creates no case. In the dialog, `PACKAGE_CHANGED` goes back to the packages, reloaded (Playwright, with a second tab as admin).
- [ ] An invalid request (no package id, a price of −1 or 12.345, a session count of 0) answers 400 with each field in `errors`.
- [ ] After a purchase, editing the offering's price, session count, duration and deadline, and removing the bought package, all succeed and leave the case's terms unchanged; a new name shows on the case, and an archived offering's case still opens.
- [ ] Customer 2 gets 404 for Customer 1's case, and `/my/cases/{id}` says "Case not found".
- [ ] `formatDate` and `formatDateTime` give Zurich dates and times in summer and winter time and around midnight UTC (Vitest).
- [ ] `openapi.json` and `schema.d.ts` have both routes; the role matrix has both rows and no customer probe; every check passes.

## Tests

- Domain: `ConsultationOfferingForPurchase` (each code; the status before the package; the copied terms; the purchase time), `CustomerConsultationCase` (credits), `PurchaseTerms`
- REST: the happy path of both routes; the 409s, 404 and 400s above; terms unchanged after the offering's edits; live name; archived offering's case; buying twice
- Authorization: `RoleMatrixTest` rows; Customer 2 on Customer 1's case → 404
- Frontend: `lib/date.ts`

## Out of scope

- The list and the header link: [02](02-my-purchases.md).
- **Buy** on the card and for visitors: [03](03-buy-from-card-and-visitors.md).
- The purchase guard on unpublish and delete, and the concurrency test: [04](04-purchase-guard.md). This task's purchase already locks the offering.

## Notes
