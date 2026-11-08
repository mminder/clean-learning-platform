# 06 Shop list: the tracer bullet

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [03](03-frontend-skeleton.md), [04](04-security-baseline.md), [05](05-ci.md) |
| Status | Done |

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
5. **Use case and controller:** `ListShopOfferingsUseCase` (`@Transactional(readOnly = true)`), and `ShopController` returning `ShopOfferingResponse` with `MoneyResponse`, shaped as in the plan's [API](README.md#api) section. The REST test turns green.
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

- [x] With docker-compose, the backend (`poc`) and the dev server running, `http://localhost:5173` shows the card from the spec scenario "A visitor sees the seeded offering". In a cloud session, check it with a Playwright screenshot.
- [x] Anonymous `GET /api/shop/offerings` returns the JSON shape from the plan, with `"amount": "450.00"`.
- [x] Draft and archived offerings are hidden, and offerings are sorted by name (REST tests).
- [x] `backend/openapi.json` is committed. `OpenApiContractTest` fails on drift and names the regenerate command (tried once by hand).
- [x] The generated TypeScript types make response fields required (`name: string`, not `name?: string`).
- [x] `npm run check:api` passes, fails when `schema.d.ts` is stale, and runs in CI.
- [x] `/v3/api-docs` is public and listed in the role matrix and in security.md.
- [x] The frontend computes no price. The "from" price comes from the API.

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

- **Versions:** springdoc-openapi 3.2.0 (`springdoc-openapi-starter-webmvc-api`, the latest 3.x on Maven Central; its parent is Spring Boot 4.1.2, with swagger-core 2.2.55, and it serves OpenAPI 3.1). openapi-typescript 7.13.0 and openapi-fetch 0.17.0. For the cards: class-variance-authority 0.7.1, radix-ui 1.8.1 and lucide-react 1.70.0 (the icon library in `components.json`, used for the image placeholder).
- **openapi-typescript and TypeScript 6:** openapi-typescript 7.13 declares the peer `typescript@^5.x`, which `npm install` rejects next to our `~6.0`. `package.json` has an `overrides` entry that gives it our TypeScript instead; generation works with 6.0. Drop the override once openapi-typescript supports TypeScript 6.
- **Deterministic document:** `OpenApiConfig` sets the info and a fixed server `/` (otherwise springdoc writes the request's host). `application.yml` turns on the pretty printer, sorted keys, `application/json` as the response media type (instead of `*/*`) and springdoc's Java `@Nullable` converter. Checked by hand: `/v3/api-docs` of the running `poc` backend is byte for byte the committed `openapi.json`.
- **Required response fields:** `RequiredResponseFieldsCustomizer` marks the properties of every `*Response` schema as required, except nullable ones; request schemas aren't touched. Checked with a temporary `@Nullable` field: springdoc wrote `"type": ["string", "null"]` and left it out of `required`. `RequiredResponseFieldsCustomizerTest` covers the rule.
- **Drift, tried by hand:** a new field on `MoneyResponse` made `OpenApiContractTest` fail with "backend/openapi.json is out of date. Regenerate it in backend/ with: ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true, then run npm run generate:api in frontend/". Surefire passes `-Dopenapi.write=true` through to the test. `npm run check:api` (`openapi-typescript --check`) exited 1 both for a hand-edited `schema.d.ts` and for a changed `openapi.json`.
- **Generated types and tooling:** `src/api/schema.d.ts` is in `.prettierignore` and ESLint's `globalIgnores`, so the file stays exactly what the generator writes and `--check` can compare it.
- **Adapter:** one JPQL query (checked in the SQL log) joins `offering` with `consultation_package` and returns one row per package price, sorted by name, then id; the adapter groups the rows and `ShopOffering.consultation` picks the cheapest. It joins `consultation_package.consultation_offering_id` to `offering.id` directly, because `consultation_offering` shares the offering's id. There's no `ConsultationOfferingEntity` yet, since nothing reads the session duration or deadline before 07. The entities map only the columns the query reads and have no getters. `offering.status` is a plain string in the entity: there's no `OfferingStatus` in the domain until UC-16 needs one.
- **For UC-16:** the inner join hides a published consultation offering without packages. Publishing should require a package. (2026-11-08: the product owner decided that removing the last package is allowed and the detail answers 404 too, see [UC-16](../../use-cases/uc-16-manage-consultation-offering.md#business-rules).)
- **Sorting (after review):** `postgres:18-alpine`'s default `en_US.utf8` sorts byte-wise under musl (`Career | Zen | agile | Ärger`). `offering.name` is now `collate "und-x-icu"` (the image has ICU: `pg_collation` lists `und-x-icu` with provider `i`), so the shop sorts alphabetically, regardless of case and umlauts (`agile | Ärger | Career | Zen`), then by id for equal names. The sorting test adds "Ärger-Coaching" and expects Agile, Ärger, Career; it failed with byte order before the collation. The rule is in [backend.md](../../architecture/backend.md#persistence) and the plan's schema section; 07 applies it to `expert_profile.display_name`.
- **Test data:** `ShopControllerTest` adds rows with `@Sql` and deletes them with a second `@Sql` in `AFTER_TEST_METHOD` (`delete-extra-offerings.sql`), which runs even when the test fails. A test transaction with rollback would also work here, but not for the later concurrency tests, so the explicit cleanup is the pattern. Mutation check: without the status filter, the draft-and-archived test failed.
- **`LayerRulesTest`:** no empty layers or rules allowed any more. Checked by hand: mistyping the domain package fails with "Layer 'domain' is empty" and "failed to check any classes".
- **`RoleMatrixTest`:** the `/api/shop/probe` row is now the real `GET /api/shop/offerings`; `GET /v3/api-docs` has its own row. `SecurityConfig` lists `/v3/api-docs/**` as public next to `/api/shop/**`.
- **Frontend:** `ImagePlaceholder` lives in `src/components/` (outside `ui/`), because expert photos will use it too. `useShopOfferings` throws on any non-2xx answer (07 adds the 404 handling for the detail). TanStack Query retries three times by default, so the error state shows after about 7 s. `formatMoney` passes the amount string to `Intl.NumberFormat`, which formats decimal strings exactly. Card, Badge and Button were copied from `new-york-v4` as in [03](03-frontend-skeleton.md#notes), since the CLI was blocked then; the CLI gives the same files now.
- **Manual check** (Playwright, Chromium, dev server, `poc` backend): one card with the image placeholder, the **Consultation** badge, "Career Coaching", the description cut to three lines with an ellipsis, and "from CHF 450.00". The empty state ("No offerings yet.", with the seed set to `DRAFT`) and the error state (backend stopped; "Try again" loaded the card once the backend was back) work too.
- **CI:** `npm run check:api` is the last step of the `frontend` job; `actionlint` passes. Its first run shows on the increment's pull request.
- **`npm audit`** reports 10 moderate findings, all in the ESLint toolchain and the same as before this task.
