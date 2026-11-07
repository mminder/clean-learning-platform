# 06 Shop list: the tracer bullet

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [03](03-frontend-skeleton.md), [04](04-security-baseline.md), [05](05-ci.md) |
| Status | Todo |

## Goal

The seeded Career Coaching offering travels from Postgres through every backend layer and the generated API client to a card in the React shop. This task sets the pattern that every later use case copies, so follow the architecture docs closely.

## What to build

Read first: [UC-02](../../use-cases/uc-02-browse-shop.md), [backend.md](../../architecture/backend.md), [frontend.md](../../architecture/frontend.md), and the whole [technical plan](README.md).

Work in this order, test first:

1. **A failing REST test** for `GET /api/shop/offerings` that expects Career Coaching at "450.00".
2. **Schema and seed:** `V1__create_offering.sql` and `V1_1__seed_offering.sql` as in the plan's [schema](README.md#schema). Give Career Coaching a description of a few sentences, so the card has to shorten it.
3. **Domain**, test first:
   - `Money` in `domain.shared`: CHF only, two decimals, not negative, compared by value (`450.0` equals `450.00`);
   - `OfferingId` and `OfferingType` (`CONSULTATION`);
   - `ShopOffering` (id, type, name, description, price). A factory for consultation offerings takes the package prices and picks the cheapest, and it rejects an empty list.
4. **Port and adapter:** `LoadShopOfferingsPort` returns the published offerings sorted by name. `ShopOfferingAdapter` loads them with their package prices in one query (no N+1) and maps them by hand.
5. **Use case and controller:** `ListShopOfferingsService` (`@Transactional(readOnly = true)`), and `ShopController` returning `ShopOfferingResponse` with `MoneyResponse`, shaped as in the plan's [API](README.md#api) section. The REST test turns green.
6. **Visibility rules:** tests for "draft and archived hidden" and "sorted by name", with the extra offerings inserted through `@Sql`.
7. **Contract:** the springdoc API-only starter; `/v3/api-docs` public in `SecurityConfig` and in [security.md](../../architecture/security.md#roles-by-route-prefix); response fields marked required; `OpenApiContractTest`; and the committed `backend/openapi.json`.
8. **Frontend client:** openapi-typescript and openapi-fetch, the scripts `generate:api` and `check:api`, the committed `src/api/schema.d.ts`, and `src/api/client.ts`.
9. **Frontend page**, in `features/shop/`:
   - `formatMoney` in `lib/money.ts`, test first;
   - `useShopOfferings()` with the query key from the plan;
   - `ShopPage`: a heading and a grid of cards, plus loading, error (with retry) and empty ("No offerings yet") states;
   - `OfferingCard`: an image placeholder, the name, the description cut to three lines, a **Consultation** badge, and "from" plus the formatted price. The card isn't clickable yet, and there's no Buy button.
10. **CI and docs:** add `npm run check:api` to the frontend job. Add the new commands to AGENTS.md.

## Acceptance criteria

- [ ] With docker-compose, the backend (`poc`) and the dev server running, `http://localhost:5173` shows the card from the spec scenario "A visitor sees the seeded offering". In a cloud session, check it with a Playwright screenshot.
- [ ] Anonymous `GET /api/shop/offerings` returns the JSON shape from the plan, with `"amount": "450.00"`.
- [ ] Draft and archived offerings are hidden, and offerings are sorted by name (REST tests).
- [ ] `backend/openapi.json` is committed. `OpenApiContractTest` fails on drift and names the regenerate command (tried once by hand).
- [ ] The generated TypeScript types make response fields required (`name: string`, not `name?: string`).
- [ ] `npm run check:api` passes, fails when `schema.d.ts` is stale, and runs in CI.
- [ ] `/v3/api-docs` is public and listed in the role matrix and in security.md.
- [ ] The frontend computes no price. The "from" price comes from the API.

## Tests

- Domain: `MoneyTest`, `ShopOfferingTest`
- REST: the happy path, drafts and archived hidden, sorted by name
- Authorization: `RoleMatrixTest` rows for `GET /api/shop/offerings` and `GET /v3/api-docs`
- Contract: `OpenApiContractTest`
- Frontend: `money.test.ts` (including `CHF 1,800.00`)

## Out of scope

- The detail page and the card link ([07](07-consultation-offering-detail.md)).
- Images, the Buy button and trainings (see the spec's out of scope).

## Notes
