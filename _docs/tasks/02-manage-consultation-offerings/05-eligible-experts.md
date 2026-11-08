# 05 Eligible experts

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [04](04-packages.md) |
| Status | Todo |

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

- [ ] On the dev server, Admin 1 checks Expert 2 for "Agile Coaching", saves and reloads: Expert 2 is checked, Expert 1 isn't (Playwright).
- [ ] `GET /api/admin/experts` returns the experts sorted by display name, without Customer 1 or Admin 1.
- [ ] An id of a user without `EXPERT`, such as Customer 1's, answers 400, and so does an unknown id.
- [ ] Removing every eligible expert of a published offering is saved, and it stays in the shop.
- [ ] The shop's detail page lists the experts the admin chose.
- [ ] The role matrix has the new route, and every check passes.

## Tests

- REST: the expert list; create and edit with experts; a customer's id; all experts removed from a published offering
- Authorization: a `RoleMatrixTest` row for `GET /api/admin/experts`

## Out of scope

- Expert profiles (UC-14). Every seeded expert has one; a user with `EXPERT` but no profile isn't offered.
- What happens to an eligible expert who loses `EXPERT` (UC-19, MVP).

## Notes
