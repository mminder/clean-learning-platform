# 02 My purchases

| | |
|---|---|
| Increment | [03 Buy a consultation package](../../specs/03-buy-consultation-package.md) |
| Type | AFK |
| Blocked by | [01](01-buy-from-detail-page.md) |
| Status | Done |

## Goal

A customer sees their cases on **My purchases**, opens each from its card, and lands there after logging in. The header shows **My purchases** to every user with `CUSTOMER`.

## What to build

Read first: the landing page of [UC-05](../../use-cases/uc-05-view-my-purchases.md#main-flow-landing-page-my-purchases), UC-01's [landing page rule](../../use-cases/uc-01-login.md#business-rules), [D-17](../../decisions.md#views-and-calls), and the plan's [API](README.md#api), [ownership](README.md#ownership) and [pages](README.md#pages) sections.

Work in this order, test first:

1. **A failing REST test:** after Customer 1 buys twice, `GET /api/customer/consultation-cases` answers both cases, the older purchase first.
2. **Domain:** `CustomerConsultationCaseSummary`, with the credits as `CustomerConsultationCase` computes them.
3. **Port and adapter:** `LoadCustomerConsultationCasesPort` and `CustomerConsultationCaseSummaryRow`; the query is scoped to the customer, sorted by `purchased_at`, then id, and joins the offering in every status.
4. **Use case and API:** `ListCustomerConsultationCasesUseCase`; `GET` on `CustomerConsultationCaseController`; a role matrix row; regenerate `openapi.json`, then `npm run generate:api`.
5. **Frontend:**
   - `useCustomerConsultationCases` in `features/consultation/queries.ts`;
   - `MyPurchasesPage` at `/my` under `RequireRole role="CUSTOMER"`, with the **Needs booking** section and the empty state;
   - `ConsultationCaseCard`;
   - **Back to my purchases** on `ConsultationCasePage`, also in its not-found state;
   - **My purchases** in the header for users with `CUSTOMER`;
   - `landingPath`: `CUSTOMER` lands on `/my`, after `ADMIN` (test first).
6. **Docs:** UC-01's landing rule already names **My purchases** (changed while planning); check that it matches.

## Acceptance criteria

- [x] On the dev server, Customer 1 with two cases sees both under **Needs booking**, the older first, each with the offering's name, image or placeholder, "Not assigned yet", "3 of 3 credits open" (or 5) and the purchase date; a card opens its case, and **Back to my purchases** returns (Playwright).
- [x] Customer 2 without cases sees "You haven't bought anything yet." and **Go to the shop**.
- [x] Customer 1 and Expert 1 land on **My purchases** after logging in from the header; Admin 1 and Expert 2 land on **Consultations**; a login from a protected page still returns there.
- [x] The header shows **My purchases** to Customer 1 and Expert 1, not to Admin 1 or Expert 2; Admin 1 on `/my` sees "Page not found".
- [x] Customer 2's list doesn't hold Customer 1's cases, and Customer 1's holds only their own.
- [x] A case of an archived offering is still listed, with its name.
- [x] `openapi.json` and `schema.d.ts` have the route; the role matrix has its row; every check passes.

## Tests

- Domain: `CustomerConsultationCaseSummary` (credits)
- REST: the happy path; the order; an archived offering's case; an empty list
- Authorization: `RoleMatrixTest` row; the two customers' lists
- Frontend: `landingPath`

## Out of scope

- The Upcoming and Past groups: with UC-06 and UC-08 (spec decision 1).
- The expert's and the admin's landing pages: UC-11 and UC-18.

## Notes

- **`CustomerConsultationCaseSummary` holds the `SessionCount`, not all `PurchaseTerms`.** The list needs only the credits, so the row selects the session count alone; the credit rule is the same as `CustomerConsultationCase`'s, and `CustomerConsultationCaseSummaryTest` covers it.
- **Playwright** (by hand, against the single jar, port 8082): Customer 2 lands on **My purchases** with "You haven't bought anything yet." and **Go to the shop**; Customer 1 starts empty, buys 3 and 5 sessions of Career Coaching, and sees two cards under **Needs booking**, 3 of 3 first, each with the placeholder image, "Not assigned yet", "N of N credits open" and "Bought 9 Nov 2026"; a card opens its case, **Back to my purchases** returns; Customer 2 still sees no cases and "Case not found" with **Back to my purchases** on Customer 1's case; a login from the protected case URL returns there; Customer 1 and Expert 1 land on `/my` and have the header link; Admin 1 and Expert 2 land on **Consultations**, have no header link, and see "Page not found" on `/my`. A case of an archived offering is covered by the REST test.
