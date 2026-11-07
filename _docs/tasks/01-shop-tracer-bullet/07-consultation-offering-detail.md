# 07 Consultation offering detail

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [06](06-shop-list.md) |
| Status | In progress |

## Goal

A shop card opens the consultation offering's detail page, with its packages and eligible experts. It adds the user tables with the six seed users, and the first error flow (404 as `ProblemDetail`).

## What to build

Read first: [UC-02](../../use-cases/uc-02-browse-shop.md) steps 3–4, [UC-01](../../use-cases/uc-01-login.md#business-rules) (seed users), [UC-14](../../use-cases/uc-14-manage-expert-profile.md) (profile fields), and the [technical plan](README.md). Follow the pattern from [06](06-shop-list.md).

Work in this order, test first:

1. **A failing REST test** for `GET /api/shop/offerings/{id}` with Career Coaching's id.
2. **Schema and seed:** `V2__create_user.sql` and `V2_1__seed_user.sql` as in the plan's [schema](README.md#schema) and [seed ids](README.md#seed-ids): the six users with their UC-01 roles, profiles for Expert 1 and Expert 2 (display name equal to the user's name, plus a short bio), and both experts eligible for Career Coaching. Add the ids to `SeedData`.
3. **Domain**, test first: `SessionDuration` (minutes > 0), `CancellationDeadline` (hours ≥ 0), `ConsultationPackageId`, `ConsultationPackage` (session count > 0, price), `UserId`, `EligibleExpert` (id, display name, bio), and `ShopConsultationOffering`, which holds them.
4. **Port and adapter:** `LoadShopOfferingPort` returns the offering only if it's `PUBLISHED`, with its packages (fewest sessions first) and eligible experts (by display name). The adapter joins `eligible_expert` and `expert_profile` ([D-23](../../decisions.md#architecture)).
5. **Use case and errors:** `GetShopOfferingService` throws `NotFoundException` when nothing is found. `ApiExceptionHandler` maps it to 404, and a malformed id to 400, both as `ProblemDetail` ([backend.md](../../architecture/backend.md#errors)).
6. **Controller:** the route returns `ShopOfferingDetailResponse`, shaped as in the plan's [API](README.md#api) section. Regenerate `openapi.json`, and add a row to the role matrix.
7. **Frontend:**
   - regenerate the types; add `useShopOffering(id)`;
   - make the whole card a link to `/offerings/:id`;
   - `OfferingDetailPage` shows the name, the **Consultation** badge, the description with its line breaks, the session duration ("60 min"), the cancellation deadline ("Free cancellation up to 24 h before a session"), the packages ("3 sessions", "CHF 450.00"), the experts (photo placeholder, display name, bio) and a link back to the shop;
   - a 404 from the API shows "Offering not found" with a link to the shop. Other errors show a retry.

## Acceptance criteria

- [ ] Demo steps 2–4 of the [spec](../../specs/01-shop-tracer-bullet.md#demo) work on the dev server: the card opens the detail page, a reload keeps it, and an unknown id shows "Offering not found".
- [ ] `GET /api/shop/offerings/{id}` returns the shape from the plan, with packages and experts in the agreed order.
- [ ] Unknown, draft and archived offerings get 404, and a malformed id gets 400, each with an `application/problem+json` body.
- [ ] The seed holds the six users with their UC-01 roles, and `SeedData` mirrors their ids.
- [ ] `openapi.json` and `schema.d.ts` are regenerated, and every check passes.
- [ ] The role matrix has the new route.

## Tests

- Domain: `SessionDuration`, `CancellationDeadline`, `ConsultationPackage`
- REST: the happy path, 404 for unknown, draft and archived, 400 for a malformed id
- Authorization: a `RoleMatrixTest` row for `GET /api/shop/offerings/{id}`

## Out of scope

- Expert photos and the Teams link (UC-14), Buy (UC-03), the training detail (step 5 of UC-02).
- JPA entities for `app_user` and `user_role`. Nothing reads them yet.

## Notes
