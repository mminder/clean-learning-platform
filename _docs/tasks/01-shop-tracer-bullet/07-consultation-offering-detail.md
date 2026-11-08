# 07 Consultation offering detail

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [06](06-shop-list.md) |
| Status | Done |

## Goal

A shop card opens the consultation offering's detail page, with its packages and eligible experts. It adds the user tables with the six seed users, and the first error flow (404 as `ProblemDetail`).

## What to build

Read first: [UC-02](../../use-cases/uc-02-browse-shop.md) steps 3–4, [UC-01](../../use-cases/uc-01-login.md#business-rules) (seed users), [UC-14](../../use-cases/uc-14-manage-expert-profile.md) (profile fields), and the [technical plan](README.md). Follow the pattern from [06](06-shop-list.md).

Work in this order, test first:

1. **A failing REST test** for `GET /api/shop/offerings/{id}` with Career Coaching's id.
2. **Schema and seed:** `V2__create_user.sql` and `V2_1__seed_user.sql` as in the plan's [schema](README.md#schema) and [seed ids](README.md#seed-ids): the six users with their UC-01 roles, profiles for Expert 1 and Expert 2 (display name equal to the user's name, plus a short bio), and both experts eligible for Career Coaching. Add the ids to `SeedData`.
3. **Domain**, test first: `SessionDuration` (minutes > 0), `CancellationDeadline` (hours ≥ 0), `ConsultationPackageId`, `ConsultationPackage` (session count > 0, price), `UserId`, `EligibleExpert` (id, display name, bio), and `ShopConsultationOffering`, which holds them.
4. **Port and adapter:** `LoadShopOfferingPort` returns the offering only if it's `PUBLISHED`, with its packages (fewest sessions first) and eligible experts (by display name). The adapter joins `eligible_expert` and `expert_profile` ([D-23](../../decisions.md#architecture)).
5. **Use case and errors:** `GetShopOfferingUseCase` throws `NotFoundException` when nothing is found. `ApiExceptionHandler` maps it to 404, and a malformed id to 400, both as `ProblemDetail` ([backend.md](../../architecture/backend.md#errors)).
6. **Controller:** the route returns `ShopOfferingDetailResponse`, shaped as in the plan's [API](README.md#api) section. Regenerate `openapi.json`, and add a row to the role matrix.
7. **Frontend:**
   - regenerate the types; add `useShopOffering(id)`;
   - make the whole card a link to `/offerings/:id`;
   - `OfferingDetailPage` shows the name, the **Consultation** badge, the description with its line breaks, the session duration ("60 min"), the cancellation deadline ("Free cancellation up to 24 h before a session"), the packages ("3 sessions", "CHF 450.00"), the experts (photo placeholder, display name, bio) and a link back to the shop;
   - a 404 from the API, or a 400 for a malformed id, shows "Offering not found" with a link to the shop. Other errors show a retry.

## Acceptance criteria

- [x] Demo steps 2–4 of the [spec](../../specs/01-shop-tracer-bullet.md#demo) work on the dev server: the card opens the detail page, a reload keeps it, and an unknown id shows "Offering not found".
- [x] `GET /api/shop/offerings/{id}` returns the shape from the plan, with packages and experts in the agreed order.
- [x] Unknown, draft and archived offerings get 404, and a malformed id gets 400, each with an `application/problem+json` body.
- [x] The seed holds the six users with their UC-01 roles, and `SeedData` mirrors their ids.
- [x] `openapi.json` and `schema.d.ts` are regenerated, and every check passes.
- [x] The role matrix has the new route.

## Tests

- Domain: `SessionDuration`, `CancellationDeadline`, `ConsultationPackage`
- REST: the happy path, 404 for unknown, draft and archived, 400 for a malformed id
- Authorization: a `RoleMatrixTest` row for `GET /api/shop/offerings/{id}`

## Out of scope

- Expert photos and the Teams link (UC-14), Buy (UC-03), the training detail (step 5 of UC-02).
- JPA entities for `app_user` and `user_role`. Nothing reads them yet.

## Notes

- **Schema:** `expert_profile.display_name` is `collate "und-x-icu"`, as the plan says. `expert_profile.bio` is `not null` (an expert without a bio has an empty one), so `EligibleExpertResponse.bio` is a required string. `eligible_expert` has an index on `expert_id` for the later "offerings of this expert" lookups through its second key column; the primary key covers lookups by offering.
- **Seed:** the six users with the UC-01 roles (Expert 1 also `CUSTOMER`, Expert 2 also `ADMIN`), emails `customer1@example.com` and so on, profiles for both experts with a short bio. `SeedData` has the six user ids; `SeedDataTest` reads `app_user` and `user_role` with `JdbcClient` and checks names, emails and roles against them, since nothing reads users through the API yet.
- **Adapter:** three JPQL queries per detail, whatever the number of packages and experts (checked in the SQL log): the offering joined with `consultation_offering` (`PUBLISHED` only), its packages `order by session_count, id`, and its experts `order by display_name, user_id`. One joined query would repeat every package for every expert. New entities map only what the queries read: `ConsultationOfferingEntity`, `EligibleExpertEntity` (composite key as a record `@IdClass`) in `adapters.database.offering`, and `ExpertProfileEntity` in `adapters.database.user` (D-23); `ConsultationPackageEntity` gained `sessionCount`. No entities for `app_user` and `user_role`.
- **Ordering test:** `sortsPackagesBySessionCountAndExpertsAlphabeticallyByDisplayName` adds a 1-session package with a larger id and two experts, "Älma Muster" and "anna Beispiel", with larger ids, through `@Sql` with cleanup. It expects 1, 3, 5 sessions and Älma, anna, Expert 1, Expert 2. Mutation check: without the collation, the test failed.
- **Errors:** `ApiExceptionHandler` (`@RestControllerAdvice` in `api`) extends `ResponseEntityExceptionHandler`, so Spring MVC's own exceptions become `ProblemDetail` without extra code: a malformed id (`TypeMismatchException`) answers 400, an unknown route (`NoResourceFoundException`) 404, for example `GET /api/shop/nope`, which 08 relies on. `NotFoundException` maps to 404 with the message as `detail`. The 404 and 400 aren't in `openapi.json`: springdoc only documents advice methods that carry `@ResponseStatus`, so the contract stays as it was and the frontend reads the status. Mutation check: without the handler, all five error tests failed (500, or a non-problem content type). Security's 401 (empty body) and 403 (Boot's error JSON) don't go through the handler and aren't `ProblemDetail`, as before ([backend.md](../../architecture/backend.md#errors)).
- **Frontend:** `lib/api-error.ts` has `ApiError` (message and status), `isNotFound`, `isBadRequest`, `isClientError` and `retryUnlessClientError` (no retry on a 4xx, otherwise TanStack's three; the `QueryClient`'s default in `Providers`), with a Vitest test. `useShopOfferings` now throws `ApiError` too. The whole card is a link through a stretched link on the title (`after:absolute after:inset-0`), so the link's accessible name is the offering's name, and a later Buy button can sit above it. The detail page shows the description with `whitespace-pre-line`; it computes nothing but "1 session"/"N sessions".
- **Manual check** (Playwright, Chromium, dev server, `poc` backend): a click on the card's description opens `/offerings/00000000-0000-0000-0000-000000001001` with the badge, "60 min", "Free cancellation up to 24 h before a session", "3 sessions"/"CHF 450.00" before "5 sessions"/"CHF 700.00", and Expert 1 before Expert 2 with their bios and photo placeholders; a reload keeps it; "Back to the shop" works. `/offerings/00000000-0000-0000-0000-000000000000` shows "Offering not found" with a link to the shop after about 130 ms (no retries). A 500 for the detail (intercepted) shows "The offering could not be loaded." with "Try again", which loads the offering. `/v3/api-docs` of the running backend is byte for byte the committed `openapi.json`.
- **Malformed id (review):** `/offerings/abc` gets a 400 from the API. The page shows "Offering not found" for a 400 as for a 404, at once, since no client error is retried.
