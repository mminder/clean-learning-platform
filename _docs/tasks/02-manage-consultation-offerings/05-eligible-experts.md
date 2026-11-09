# 05 Eligible experts

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [04](04-packages.md) |
| Status | Done |

## Goal

The admin chooses an offering's eligible experts from the users with `EXPERT`, in the same form and with the same **Save**.

## What to build

Read first: [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) (eligible experts), [D-06](../../decisions.md#catalog), and the plan's [API](README.md#api) and [writes](README.md#writes) sections.

Work in this order, test first:

1. **A failing REST test:** `GET /api/admin/experts` returns Expert 1 and Expert 2, in that order.
2. **Experts:** `Expert` in `domain.user` (id, display name), `LoadExpertsPort` and `ListExpertsUseCase` in `application.user`, implemented in `UserAdapter`: users with `EXPERT` that have an expert profile, sorted by display name, in one query. `ExpertController` in `api.user` serves the route with `ExpertResponse`. Add a role matrix row.
3. **Domain:** `ConsultationOffering` gets its eligible expert ids, as a set.
4. **Use cases:** create and edit check the requested ids against `LoadExpertsPort` (the PoC has a handful of experts, so loading all of them is fine) and throw `IllegalArgumentException` (400) for an id that isn't an expert. The two use cases need exactly what `ListExpertsUseCase` needs, so they share the port ([backend.md](../../architecture/backend.md#ports)).
5. **Adapter:** `ConsultationOfferingAdapter` loads the ids and writes the `eligible_expert` rows.
6. **API:** `eligibleExpertIds` in `ConsultationOfferingRequest` (`@NotNull`, may be empty) and in `ConsultationOfferingResponse` (sorted).
7. **Frontend:** an "Eligible experts" section in `ConsultationOfferingForm`: one checkbox per expert from `useExperts()` (query key `['admin', 'experts']`), labelled with the display name.

## Acceptance criteria

- [x] On the dev server, Admin 1 checks Expert 2 for "Agile Coaching", saves and reloads: Expert 2 is checked, Expert 1 isn't (Playwright).
- [x] `GET /api/admin/experts` returns the experts sorted by display name, without Customer 1 or Admin 1.
- [x] An id of a user without `EXPERT`, such as Customer 1's, answers 400, and so does an unknown id.
- [x] Removing every eligible expert of a published offering is saved, and it stays in the shop.
- [x] The shop's detail page lists the experts the admin chose.
- [x] The role matrix has the new route, and every check passes.

## Tests

- REST: the expert list; create and edit with experts; a customer's id; all experts removed from a published offering
- Authorization: a `RoleMatrixTest` row for `GET /api/admin/experts`

## Out of scope

- Expert profiles (UC-14). Every seeded expert has one; a user with `EXPERT` but no profile isn't offered.
- What happens to an eligible expert who loses `EXPERT` (UC-19, MVP).

## Notes

- **Experts:** `Expert` (id, display name) in `domain.user`; `LoadExpertsPort.loadExperts()` is named so, not `loadAll()`, because `UserAdapter` already implements `LoadUsersPort.loadAll()` and two methods with that name and different return types can't live in one class. The query joins `expert_profile` with `user_role` (`EXPERT`) and sorts by the ICU-collated display name, then id. The role matrix has its row.
- **The expert check is a domain rule:** `ConsultationOffering.create` and `edit` take the eligible expert ids and the list of experts and throw `IllegalArgumentException` ("User {id} is not an expert.") for an id that isn't in it. The use cases only load the experts through `LoadExpertsPort` (the same port as `ListExpertsUseCase`) and pass them in, so they stay flow only. The domain holds the ids as a `Set<UserId>`, the command too; the response sorts them as text, like the packages.
- **A user with `EXPERT` but no profile** is not offered and answers 400 if chosen (`eligible_expert.expert_id` references `expert_profile`, so saving it would be a foreign key violation, a 500). So does a profile without `EXPERT`. Two throwaway users in `api/user/users-that-are-not-offered-as-experts.sql` cover both in `ExpertControllerTest` and `ConsultationOfferingControllerTest`.
- **Adapter:** like the packages, it writes the difference to the loaded `EligibleExpertEntity`s: it removes the dropped rows and persists the new ones; a kept row isn't touched (a REST test checks its `updated_at`). Replacing all rows would delete and insert the same key in one flush, and Hibernate inserts first. `EligibleExpertEntity` gained a constructor and a getter.
- **Request:** `List<@NotNull UUID> eligibleExpertIds`, not a `Set`: a `Set` would put `uniqueItems` in the contract, and duplicates are allowed (they count once). A `null` entry answers 400 with `eligibleExpertIds[0]`; a malformed uuid answers Spring's 400 without `errors`.
- **Frontend:** `useExperts()` is in `features/user/queries.ts` (the experts are users; query key `['admin', 'experts']`). The form's `eligibleExpertIds` is a `Controller` over one `Checkbox` per expert (shadcn `checkbox`). `toRequest` and the new form carry the ids.
- **Not handled (UC-19, MVP):** an eligible expert who loses `EXPERT` is not offered in the form, but the form still sends their id, so a save answers 400 until UC-19 decides what happens to such an expert.
- **Existing `PUT` tests** now send `eligibleExpertIds` (`[]`); the shared bodies and `withPackages` have it.
- **UC-16** says the experts are users with `EXPERT` and an expert profile.
- **Browser check** (Playwright, dev server, `poc` backend): the form offers Expert 1 and Expert 2 and no customer or admin; a new "Agile Coaching" with Expert 2 checked, saved and reloaded: Expert 2 checked, Expert 1 not; the list shows 0 packages and 1 expert. After adding a package and Expert 1, the database has two `eligible_expert` rows and, once published through SQL, the shop's detail page lists Expert 1 and Expert 2. With both unchecked and saved there are no rows, and the offering stays in the shop. The only answer >= 400 was the first `GET /api/me` (401). The dev database was cleaned up afterwards.
