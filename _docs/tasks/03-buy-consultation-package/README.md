# 03 Buy a consultation package: technical plan

Spec: [03 Buy a consultation package](../../specs/03-buy-consultation-package.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same change. Everything from the plans of [increment 01](../01-shop-tracer-bullet/README.md) and [increment 02](../02-manage-consultation-offerings/README.md) still holds: toolchain, test setup, error conventions, writes, locks, forms and mutations.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [Buy a package from the detail page](01-buy-from-detail-page.md) | AFK | – |
| 02 | [My purchases](02-my-purchases.md) | AFK | 01 |
| 03 | [Buy from the card, and visitors](03-buy-from-card-and-visitors.md) | AFK | 01 |
| 04 | [Purchase guard on unpublish and delete](04-purchase-guard.md) | AFK | 01 |

```
                ┌─► 02 My purchases
01 Buy from ────┼─► 03 Buy from the card, and visitors
the detail page └─► 04 Purchase guard
```

01 builds the new `consultation` feature from the schema to the page, and sets the pattern for customer-owned data: review it with extra care, as task 03 of increment 02. 02, 03 and 04 then run in parallel, each in its own git worktree ([D-33](../../decisions.md#scope-and-docs)):

| Task | Touches | Shared with another task |
|---|---|---|
| 02 | `api.consultation` (a route), `application.consultation`, `adapters.database.consultation`, `features/consultation/`, `Layout`, `landing.ts`, `App.tsx`, `openapi.json` | `RoleMatrixTest` (a row) |
| 03 | `features/shop/` only | – |
| 04 | `domain.offering`, `application.offering`, `adapters.database.offering`, a concurrency test | – |

Only 02 changes `openapi.json` and `schema.d.ts`; 04's routes already declare their 409.

## Backend

### Packages and names

| Package | Classes | Task |
|---|---|---|
| `config` | `ClockConfig`: the `Clock` bean, `Clock.system(ZoneId.of("Europe/Zurich"))` (spec decision 14) | 01 |
| `api.consultation` | `CustomerConsultationCaseController` (`/api/customer/consultation-cases`) | 01, 02 |
| `api.consultation.request` | `PurchaseConsultationPackageRequest` | 01 |
| `api.consultation.response` | `CustomerConsultationCaseResponse` (detail), `PurchaseTermsResponse`, `CustomerConsultationCaseSummaryResponse` (list item, 02) | 01, 02 |
| `application.consultation` | `PurchaseConsultationPackageUseCase` with `PurchaseConsultationPackageCommand`, `GetCustomerConsultationCaseUseCase`, `ListCustomerConsultationCasesUseCase` (02) | 01, 02 |
| `application.consultation.port` | `LockConsultationOfferingForPurchasePort`, `SaveConsultationCasePort` (`create`), `LoadCustomerConsultationCasePort`, `LoadCustomerConsultationCasesPort` (02) | 01, 02 |
| `application.offering.port` | `CheckOfferingPurchasesPort` (`hasPurchases`) | 04 |
| `domain.consultation` | `ConsultationOfferingForPurchase` (the offering as a purchase needs it; holds UC-03's rules), `ConsultationCase` (a new case, as the purchase creates it), `CustomerConsultationCase` (the case as its customer sees it; computes the credits), `CustomerConsultationCaseSummary` (02) | 01, 02 |
| `domain.consultation.value` | `ConsultationCaseId`, `PurchaseTerms` (price paid, session count, session duration, cancellation deadline) | 01 |
| `domain.offering` | `ConsultationOffering`: `unpublish(boolean hasPurchases)`, `requireDeletable(boolean hasPurchases)`, `OFFERING_HAS_PURCHASES` | 04 |
| `adapters.database.consultation` | `ConsultationCaseAdapter`, which implements the consultation ports; `entity`: `ConsultationCaseEntity`; `row`: `CustomerConsultationCaseRow`, `CustomerConsultationCaseSummaryRow` (02) | 01, 02 |
| `adapters.database.offering` | `ConsultationOfferingAdapter` implements `CheckOfferingPurchasesPort` too | 04 |

- **Names** follow spec decision 8: `ConsultationCase` in code, `consultation_case` in the database, `consultation-cases` in routes. The customer's records carry the audience, `Customer…`, because the expert's (UC-11) and the admin's (UC-18) case responses will be different records ([backend.md](../../architecture/backend.md#rest-conventions)).
- **Other features' types.** The case uses the offering's value objects (`OfferingId`, `OfferingName`, `SessionCount`, `SessionDuration`, `CancellationDeadline`, `ConsultationPackageId`), `ConsultationPackage`, `ImageId` and `UserId` where they are, as the offering already uses `UserId` and `ImageId` ([D-23](../../decisions.md#architecture)). Nothing moves to `domain.shared.value`.
- **`ConsultationCaseAdapter`** reads the offering's tables for `LockConsultationOfferingForPurchasePort` and the case queries, and may use the offering's entities (D-23).
- **The use cases take the `CurrentUser`**: the purchase makes it the case's customer, and the queries are scoped to it.

### Schema

| Migration | Task | Content |
|---|---|---|
| `V6__create_consultation_case.sql` | 01 | the `consultation_case` table with its `updated_at` trigger and indexes |

```
consultation_case  id                          uuid PK,
                   customer_id                 uuid not null → app_user,
                   consultation_offering_id    uuid not null → consultation_offering (offering_id),
                   price_paid                  numeric(10,2) not null, >= 0,
                   session_count               int not null, > 0,
                   session_duration_minutes    int not null, > 0,
                   cancellation_deadline_hours int not null, >= 0,
                   purchased_at                timestamptz not null,
                   created_at, updated_at
indexes            (customer_id), (consultation_offering_id)
```

- **The purchase terms** are the four columns from `price_paid` to `cancellation_deadline_hours` ([D-08](../../decisions.md#catalog)), with the same checks as the offering's columns they're copied from ([D-36](../../decisions.md#architecture)).
- **No package column** (spec decision 11), **no status, assigned expert or cancellation columns** (spec decision 12). UC-06 and UC-08 add theirs in new migrations.
- **The foreign key to the offering** keeps an offering with purchases from being deleted even if the guard fails; 04's guard answers 409 before it's reached. `consultation_offering_id` follows `consultation_package`'s name.
- **`purchased_at`** is the purchase time from the `Clock`. `created_at` stays the database's technical time.
- **The `updated_at` trigger** comes with the table ([backend.md](../../architecture/backend.md#persistence)); `UpdatedAtTriggersTest` fails without it.
- No new seed (spec decision 10).

### API

| Route | Answer | Task |
|---|---|---|
| `POST /api/customer/consultation-cases` | `PurchaseConsultationPackageRequest` → 201 `CustomerConsultationCaseResponse` with a `Location` header; 400; 404 for an unknown offering; 409 (`OFFERING_NOT_AVAILABLE`, `PACKAGE_CHANGED`); 415 | 01 |
| `GET /api/customer/consultation-cases/{id}` | 200 `CustomerConsultationCaseResponse`; 404 for an unknown case or another customer's; 400 for a malformed id | 01 |
| `GET /api/customer/consultation-cases` | 200 `CustomerConsultationCaseSummaryResponse[]`: the customer's own cases, by purchase time, then id | 02 |

401 and 403 come from security: `/api/customer/**` needs `CUSTOMER` ([security.md](../../architecture/security.md#roles-by-route-prefix)). Each method declares its errors with `ProblemResponses` and its success with `@ResponseStatus` ([backend.md](../../architecture/backend.md#errors)). operationIds: `purchaseConsultationPackage`, `getCustomerConsultationCase`, `listCustomerConsultationCases`.

```jsonc
// PurchaseConsultationPackageRequest
{
  "offeringId": "…1001",                     // not null
  "packageId": "…1011",                      // not null
  "price": "450.00",                         // not null; the price the dialog showed; same pattern as ConsultationPackageRequest's price
  "sessionCount": 3                          // not null, > 0; the session count the dialog showed
}

// CustomerConsultationCaseResponse
{
  "id": "…",
  "offeringName": "Career Coaching",         // live
  "offeringImageId": null,                   // live; @Nullable
  "purchaseTerms": {                         // PurchaseTermsResponse
    "pricePaid": { "amount": "450.00", "currency": "CHF" },
    "sessionCount": 3,
    "sessionDurationMinutes": 60,
    "cancellationDeadlineHours": 24
  },
  "purchasedAt": "2026-11-09T14:03:12.345Z",
  "openCredits": 3,
  "totalCredits": 3
}

// CustomerConsultationCaseSummaryResponse (02)
{
  "id": "…",
  "offeringName": "Career Coaching",
  "offeringImageId": null,                   // @Nullable
  "purchasedAt": "2026-11-09T14:03:12.345Z",
  "openCredits": 3,
  "totalCredits": 3
}
```

- **The price in the request** reuses `ConsultationPackageRequest`'s pattern and message (make its two constants public). Responses use `MoneyResponse`.
- **The confirmed terms** are `price` and `sessionCount`: what the dialog's confirmation stated (spec decision 3). Session duration and deadline aren't confirmed; the detail page shows them before **Buy**.
- **`purchasedAt`** is an instant, ISO-8601 with offset ([backend.md](../../architecture/backend.md#rest-conventions)); it's the first one in the API.
- **Credits** are computed by `CustomerConsultationCase` and `CustomerConsultationCaseSummary`: total credits are the purchase terms' session count, and open credits equal them until appointments (UC-06) and credit adjustments (UC-18) exist (spec decision 12). Not stored ([D-11](../../decisions.md#consultation)).
- **The `Location`** is relative, `/api/customer/consultation-cases/{id}`, as `ResponseEntity.created` builds it for the offerings.
- New request and response names stay unique across `api` (`LayerRulesTest`).

### Purchase

`PurchaseConsultationPackageUseCase.invoke(CurrentUser, PurchaseConsultationPackageCommand)`:

1. **Lock** the offering with `LockConsultationOfferingForPurchasePort.lock(offeringId)`: `PESSIMISTIC_WRITE` on its `offering` row, as `ConsultationOfferingAdapter.lock` does, then its consultation fields and packages. An unknown id, or an offering that isn't a consultation, is a `NotFoundException` (404). The lock order stays offering → case → expert → run ([D-27](../../decisions.md#architecture)).
2. **The domain decides**: `ConsultationOfferingForPurchase.purchase(customerId, packageId, confirmedPrice, confirmedSessionCount, now)` returns the new `ConsultationCase`, or throws:

   | Code | When | `detail` |
   |---|---|---|
   | `OFFERING_NOT_AVAILABLE` | the offering isn't `PUBLISHED` | This offering is no longer available. |
   | `PACKAGE_CHANGED` | the package isn't one of the offering's, or its price or session count differs from the confirmed ones | This package has changed or is no longer available. Please choose again. |

   The status is checked first. A published offering without packages is out of the shop (UC-02), and buying from it is `PACKAGE_CHANGED`. The case gets a new id, the customer, the offering id, the purchase terms (the package's price and session count, the offering's session duration and deadline) and `now` as the purchase time. `now` comes from the injected `Clock` (`clock.instant()`).
3. **Write** with `SaveConsultationCasePort.create`: `EntityManager.persist`, as for every new row ([backend.md](../../architecture/backend.md#persistence)).
4. **Answer** with the new case as its customer sees it, loaded through `LoadCustomerConsultationCasePort` in the same transaction; the query flushes the insert first.

The codes are constants on `ConsultationOfferingForPurchase`, next to the rule.

### Ownership

- **Scoped queries.** `LoadCustomerConsultationCasePort.load(ConsultationCaseId, UserId customerId)` and `LoadCustomerConsultationCasesPort.loadAll(UserId customerId)` filter by `customer_id` in SQL. A case of another customer is empty, exactly like an unknown id, and the use case throws `NotFoundException` (404) ([security.md](../../architecture/security.md#ownership-in-use-cases)).
- **Every status.** The queries join the offering whatever its status: an archived offering's cases keep working (UC-16), and name and image are read live ([D-08](../../decisions.md#catalog)).

### Purchase guard

04, following [D-07](../../decisions.md#catalog) and spec 02's decision 5.

- **The use cases** `UnpublishConsultationOfferingUseCase` and `DeleteConsultationOfferingUseCase` lock the offering (as now), then ask `CheckOfferingPurchasesPort.hasPurchases(offeringId)`, then call the domain. The question runs under the lock, so a purchase either committed before it, and counts, or waits and then finds a draft.
- **The domain decides**: `unpublish(boolean hasPurchases)` and `requireDeletable(boolean hasPurchases)` check the status first, as now, then the purchases:

  | Change | Status allows it, with purchases | `detail` |
  |---|---|---|
  | Unpublish | 409 `OFFERING_HAS_PURCHASES` | The offering has purchases. Archive it instead. |
  | Delete | 409 `OFFERING_HAS_PURCHASES` | The offering has purchases and can't be deleted. |

  Archive, edit and the image don't look at purchases. A draft can't get purchases while unpublish needs none, so delete's check is a safety net.
- **The query** is an `exists` over `consultation_case` by `consultation_offering_id`, in `ConsultationOfferingAdapter`.
- **The frontend** needs no change: `OfferingStatusActions` shows a 409's `detail` above the form and reloads the offering (spec decision 6).

## Frontend

```
frontend/src
├─ app/               App: /my/cases/:id (01) and /my (02) under RequireRole role="CUSTOMER";
│                     Layout: My purchases for users with CUSTOMER (02)
├─ components/ui/     dialog, radio-group (01)
├─ lib/               date.ts: formatDate, formatDateTime, in Europe/Zurich (01)
└─ features/
   ├─ shop/           BuyConsultationButton (01: detail page; 03: the card, visitors), BuyConsultationDialog (01)
   ├─ consultation/   queries.ts: usePurchaseConsultationPackage, useCustomerConsultationCase (01),
   │                  useCustomerConsultationCases (02); ConsultationCasePage (01);
   │                  MyPurchasesPage, ConsultationCaseCard (02)
   └─ user/           landing.ts: CUSTOMER lands on /my (02)
```

| Route | Page | Access | Task |
|---|---|---|---|
| `/my/cases/:id` | `ConsultationCasePage` | customer | 01 |
| `/my` | `MyPurchasesPage` | customer | 02 |

`date-fns` and `@date-fns/tz` are installed with 01 ([frontend.md](../../architecture/frontend.md#stack)).

### Buy

- **`BuyConsultationButton`** (`offeringId`, `offeringName`) decides what **Buy** does, from `useMe()`:

  | User | **Buy** | Task |
  |---|---|---|
  | loading `me` | not shown | 01 |
  | visitor | shown; opens `/login?redirect=<this page>` (path, search and hash, as `RequireRole` builds it) | 03 |
  | with `CUSTOMER` | shown; opens the dialog | 01 |
  | without `CUSTOMER` | not shown ([UC-02](../../use-cases/uc-02-browse-shop.md#business-rules)) | 01 |

  It sits on the detail page under the description (01) and in the card's footer, next to the price (03).
- **`BuyConsultationDialog`** (shadcn `dialog`) loads the packages with `useShopOffering(offeringId)` while it's open; on the detail page they're already cached. Two steps:

  | Step | Content | Buttons |
  |---|---|---|
  | Choose | Title "Buy {name}"; "Choose a package."; one radio item per package (shadcn `radio-group`): "3 sessions", "CHF 450.00" | **Cancel**; **Continue**, enabled once a package is chosen |
  | Confirm | "Do you really want to buy {name}, {3 sessions} for {CHF 450.00}?" | **Back**; **Buy** |

  "1 session" in the singular, as the detail page writes it. Money through `formatMoney`. Closing the dialog (**Cancel**, Escape, the close button) creates nothing.
- **After the purchase** the case detail opens: `navigate('/my/cases/{id}')` with the id from the response.
- **Errors** show in the dialog (`role="alert"`):

  | Answer | Message | Then |
  |---|---|---|
  | 409 `PACKAGE_CHANGED` | the `detail` | back to **Choose**, with the reloaded packages and no package chosen |
  | 409 `OFFERING_NOT_AVAILABLE` | the `detail` | only **Close** is left |
  | 404 | "This offering is no longer available." | only **Close** is left |
  | anything else | "Buying failed. Please try again." | stays on **Confirm** |

  After a 409 `PACKAGE_CHANGED` the dialog fetches the offering past the cache and puts it into `['shop', 'offerings', id]`, so **Choose** and the detail page show the current packages; if that fetch answers 404 (the last package was removed, so the offering left the shop), the dialog shows "This offering is no longer available." with only **Close**. After any 409 or 404, `['shop']` is invalidated when the dialog closes, not before: the dialog is a child of the card and of the detail page, and reloading at once would take the card out of the list or turn the detail page into "Offering not found", so the dialog would vanish without its message. Once it's closed, an offering that left the shop shows "Offering not found" on its detail page, and leaves the shop's list.

### Queries

- **Query keys:** `['customer', 'consultation-cases']` (list, 02), `['customer', 'consultation-cases', id]` (detail, 01).
- **`usePurchaseConsultationPackage`** invalidates `['customer', 'consultation-cases']`, which includes every detail. On a 409 or 404 the dialog invalidates `['shop']` when it closes ([Buy](#buy)).
- After login the cache is reset, and after logout the page reloads ([frontend.md](../../architecture/frontend.md#navigation-and-auth)), so no customer's cases stay in memory.

### Pages

- **`ConsultationCasePage`** (01): `OfferingImage`, the offering's name as the heading, and a list of terms:

  | Label | Value |
  |---|---|
  | Expert | Not assigned yet |
  | Credits | 3 of 3 open |
  | Price paid | CHF 450.00 |
  | Sessions | 3 × 60 min |
  | Cancellation | Free cancellation up to 24 h before a session |
  | Bought | 9 Nov 2026, 15:03 (Zurich time) |

  A 404 or 400 shows "Case not found". 02 adds **Back to my purchases** at the top, and to the not-found state.
- **`MyPurchasesPage`** (02): the heading "My purchases"; a section **Needs booking** with a grid of `ConsultationCaseCard`s in the order the API returns them (spec decision 1). Without cases: "You haven't bought anything yet." and a link **Go to the shop**.
- **`ConsultationCaseCard`** (02), like `OfferingCard`: `OfferingImage`, the offering's name as the link (stretched over the card), "Not assigned yet", "3 of 3 credits open", "Bought 9 Nov 2026".
- **Header** (02): **My purchases** after **Shop** for a user with `CUSTOMER`, before **Consultations** ([D-03](../../decisions.md#access-and-roles)).
- **Landing page** (02): `landingPath` returns `/admin/consultations` with `ADMIN`, else `/my` with `CUSTOMER`, else `/` (spec decision 5).

### Dates

`lib/date.ts` (01) is the one place that formats a point in time, always in `Europe/Zurich` (`TZDate` from `@date-fns/tz`, `format` from `date-fns`):

| Function | Example |
|---|---|
| `formatDate(instant)` | 9 Nov 2026 |
| `formatDateTime(instant)` | 9 Nov 2026, 15:03 |

A page that shows a time adds "(Zurich time)" once, next to it ([use cases](../../use-cases/README.md#cross-cutting-rules)). [frontend.md](../../architecture/frontend.md#dates-times-and-money) gets the two functions (01).

### Components

shadcn/ui: `dialog` and `radio-group` (01), added with the CLI and then `npm run format` ([frontend.md](../../architecture/frontend.md#structure)).

## Tests

| What | How |
|---|---|
| Test data | Tests buy through the API, as Customer 1 or Customer 2, from Career Coaching or from an offering the test creates. Tests never change seed rows. 01 renames the cleanup script to `delete-test-data.sql` (`SeedData.DELETE_TEST_DATA`, in place of `DELETE_TEST_OFFERINGS`): it deletes every case first (the seed has none), then every offering that isn't in the seed, as before. A test that buys runs it after each test method. |
| Changing a bought offering | A test that edits, archives or removes packages after a purchase uses its own offering, never Career Coaching. |
| Time | REST tests check that `purchasedAt` lies between the instants before and after the request. Domain tests pass `now` in. |
| Role matrix | Rows for the three routes, all `CUSTOMER` (01, 02). `POST` sends no body (400). The `GET /api/customer/probe` row goes: the prefix has real routes now (01). |
| Ownership | With the two seeded customers ([security.md](../../architecture/security.md#testing)): Customer 2 on Customer 1's case → 404 (01); Customer 2's list doesn't hold Customer 1's case, and Customer 1's holds only their own (02). |
| Concurrency | One test for the purchase against an unpublish (04), deterministic as `ConsultationOfferingConcurrencyTest`: exactly one succeeds, the other gets 409, and the offering is never a draft with a case. It must fail when the purchase doesn't lock the offering. |
| Domain | `ConsultationOfferingForPurchase`: each code, the status before the package, the copied terms (01). `CustomerConsultationCase`: the credits (01). `ConsultationOffering`: unpublish and delete with and without purchases, after the status (04). |
| Frontend | Vitest for logic only ([D-29](../../decisions.md#architecture)): `lib/date.ts` in summer and winter time and around midnight UTC (01), `landingPath` (02). Pages and the dialog are checked by hand with Playwright. |

## Commands

No new commands. A task that adds one also adds it to the **Commands** section of [AGENTS.md](../../../AGENTS.md#commands).
