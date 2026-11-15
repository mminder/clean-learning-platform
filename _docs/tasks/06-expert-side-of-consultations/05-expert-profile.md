# 05 The expert profile

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | – |
| Status | Done |

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

- [x] Expert 2 changes the display name, the bio and the Teams link and saves: "Saved", and the same values after a reload (REST and Playwright).
- [x] The shop's offering detail, the expert picker, Customer 1's case and its card on **My purchases** show the new display name (REST for the first three's routes, Playwright for the pages).
- [x] A blank display name, and a Teams link that isn't an `https://` link (`http://example.com`, `example.com`, a link with a space), answer 400 with a message next to the field, and nothing is saved (REST and Playwright).
- [x] An empty bio is saved as empty; an empty Teams link, `""` or `null`, removes the link (REST).
- [x] Expert 1 and Expert 2 each read and save only their own profile: the routes have no id (REST).
- [x] The bodies of the shop's offering detail, the eligible experts and the customer's case don't contain the saved Teams link (REST).
- [x] `expert_profile.teams_link` refuses a value that `TeamsLink` refuses (a test of the check constraint, as `NameCheckConstraintsTest`).
- [x] `/expert/profile` shows "Page not found" to a user without `EXPERT`; the role matrix has both rows, and its `PUT` changes no data.
- [x] `openapi.json` and `schema.d.ts` have both routes; every check passes.

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

- The check by hand ran a Playwright script (Playwright from the npx cache, not a project dependency) against the Vite dev server and the backend with the `poc` profile, which applied V12 to the dev database. Expert 2's header read **Shop**, **My cases**, **Availability**, **Profile**, **Consultations**; **Profile** showed the seeded name and bio and no link; `http://example.com`, `example.com` and a link with a space each answered 400 with "must be an https:// link" next to **Teams link**, and a blank display name "must not be blank" next to **Display name**, with the profile unchanged; "Expert Two", a new bio and `https://example.com/meet/expert-2` saved with "Saved" and came back after a reload. As a visitor, Career Coaching's detail showed "Expert Two" with the new bio; Customer 2's booking dialog (the dev database's case without an expert, nothing booked) showed them in the expert picker; neither page contained the link. The dev database's Customer 1 case has Expert 1, not Expert 2, so Expert 1 renamed themself "Expert One" on **Profile**, which left Expert 2's profile as it was, and Customer 1's card on **My purchases** and the case detail showed "Expert One". Customer 1 on `/expert/profile` read "Page not found". Both profiles were set back to the seed afterwards, `updated_at` included; nothing else was added.
- The check constraint's test is a class of its own, `TeamsLinkCheckConstraintTest`, next to `NameCheckConstraintsTest` and `ImageCheckConstraintsTest`: a link isn't a name. It runs the links of `TeamsLinkTest` against the column.
- `TeamsLink.PATTERN` is the one expression: the value object matches it, and `ExpertProfileRequest` wraps it as `^(…)?$`, so `""` passes Bean Validation and is no link, as `null` is. The migration repeats it as `'^https://[!-~]+$'`.
- A display name of only no-break spaces passes `@NotBlank` and is refused by `DisplayName`: a 400 whose `detail` shows above the form, not next to the field, as for an offering's name. A display name of ordinary spaces gets "must not be blank" next to the field.
- The **Teams link** input is a plain text input, not `type="url"`: the browser would refuse `example.com` itself before the backend's message could show.
- `ExpertProfileControllerTest.theTestCleanupRestoresTheSeededProfiles` runs `delete-test-data.sql` after saving both profiles and checks that both are as before, without a link; it fails with a changed bio in the script.
