# 05 The expert profile

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

An expert opens **Profile** and edits their display name, bio and Teams link. The new name and bio show in the shop, in the expert picker and on the customer's case at once. The Teams link is stored, and shown to nobody but its expert until [07](07-join-call.md).

## What to build

Read first: [UC-14](../../use-cases/uc-14-manage-expert-profile.md), [D-19](../../decisions.md#views-and-calls), [D-27](../../decisions.md#architecture), [D-36](../../decisions.md#architecture), the spec's decisions 1, 8, 12, 13, 15 and 16, its requirement [The expert profile](../../specs/06-expert-side-of-consultations.md#requirement-the-expert-profile), and the plan's [packages and names](README.md#packages-and-names), [schema](README.md#schema), [API](README.md#api), [the expert profile](README.md#the-expert-profile), [profile](README.md#profile), [queries](README.md#queries) and [tests](README.md#tests) sections. The availability is the pattern for data an expert owns ([task 01 of increment 04](../04-book-consultation-appointment/01-manage-availability.md): routes without an id, the expert's lock), and `ConsultationOfferingForm` for the form.

Work in this order, test first:

1. **A failing REST test:** `PUT /api/expert/profile` as Expert 2 with the display name "Expert Two", a new bio and the Teams link `https://example.com/meet/expert-2` answers 200 with them, and `GET` answers the same. The shop's Career Coaching detail then names "Expert Two" with the new bio.
2. **Test data:** `delete-test-data.sql` sets both seeded profiles back to the seed's display name and bio, without a Teams link. `SeedData`'s comment says so.
3. **Domain:** `TeamsLink` in `domain.user.value`; `OwnExpertProfile` with `edit`.
4. **Schema and adapter:** `V12__expert_teams_link.sql`; `ExpertProfileEntity` maps `teamsLink` and gains its setters; `ExpertProfileAdapter` implements `LoadOwnExpertProfilePort` and `SaveOwnExpertProfilePort`.
5. **Use cases and API:** `GetExpertProfileUseCase`; `SaveExpertProfileUseCase` (lock the expert, load, edit, save, answer); `ExpertProfileController` with `@PreAuthorize("hasRole('EXPERT')")`, `ExpertProfileRequest` and `ExpertProfileResponse`, without `photoId`, which comes with 06; role matrix rows. Regenerate `openapi.json`, then `npm run generate:api`.
6. **Frontend:** `useExpertProfile` and `useSaveExpertProfile`; `ExpertProfilePage` with `ExpertProfileForm` at `/expert/profile` under `RequireRole role="EXPERT"`; **Profile** in the header, after **Availability**.
7. **Docs:** [frontend.md](../../architecture/frontend.md#navigation-and-auth) gets **Profile** in the header's order.
8. **Check by hand** on the dev server with Playwright: the spec's demo, step 2 without the photo: `http://example.com` shows a message next to the Teams link and saves nothing; a valid link and a new bio save and show "Saved"; the shop's offering detail and the expert picker show the new name and bio.

## Acceptance criteria

- [ ] Expert 2 changes the display name, the bio and the Teams link and saves: "Saved", and the same values after a reload (REST and Playwright).
- [ ] The shop's offering detail, the expert picker, Customer 1's case and its card on **My purchases** show the new display name (REST for the first three's routes, Playwright for the pages).
- [ ] A blank display name, and a Teams link that isn't an `https://` link (`http://example.com`, `example.com`, a link with a space), answer 400 with a message next to the field, and nothing is saved (REST and Playwright).
- [ ] An empty bio is saved as empty; an empty Teams link, `""` or `null`, removes the link (REST).
- [ ] Expert 1 and Expert 2 each read and save only their own profile: the routes have no id (REST).
- [ ] The bodies of the shop's offering detail, the eligible experts and the customer's case don't contain the saved Teams link (REST).
- [ ] `expert_profile.teams_link` refuses a value that `TeamsLink` refuses (a test of the check constraint, as `NameCheckConstraintsTest`).
- [ ] `/expert/profile` shows "Page not found" to a user without `EXPERT`; the role matrix has both rows, and its `PUT` changes no data.
- [ ] `openapi.json` and `schema.d.ts` have both routes; every check passes.

## Tests

- Domain: `TeamsLink` (`https://` only, not empty after it, no spaces, any host), `OwnExpertProfile.edit`
- REST: both routes' happy paths; the 400s with their fields; an empty bio; removing the link; the new name on the shop's detail; no Teams link in the public bodies
- Authorization: the role matrix rows; each expert's own profile
- Database: the check constraint on `teams_link`
- Frontend: none beyond the checks

## Out of scope

- The photo: [06](06-expert-photo.md).
- The link on appointments: [07](07-join-call.md).
- Creating a profile when `EXPERT` is granted: UC-19, MVP (spec decision 12).
- Profiles for customers or admins: UC-14's out of scope.

## Notes
