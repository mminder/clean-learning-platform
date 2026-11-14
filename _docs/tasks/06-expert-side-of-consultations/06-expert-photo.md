# 06 The expert photo

| | |
|---|---|
| Increment | [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md) |
| Type | AFK |
| Blocked by | [05](05-expert-profile.md) |
| Status | Todo |

## Goal

An expert uploads, replaces and removes their photo on **Profile**. It shows as a round picture in the shop's offering detail, in the expert picker and on the customer's case, in place of the placeholder. The expert photo is the second image owner after the offering.

## What to build

Read first: [UC-14](../../use-cases/uc-14-manage-expert-profile.md), [D-21](../../decisions.md#cross-cutting), the spec's decisions 9 and 13, its scenarios [Upload, replace and remove the photo](../../specs/06-expert-side-of-consultations.md#scenario-upload-replace-and-remove-the-photo) and [An invalid photo](../../specs/06-expert-side-of-consultations.md#scenario-an-invalid-photo), and the plan's [schema](README.md#schema), [API](README.md#api), [the expert profile](README.md#the-expert-profile) and [profile](README.md#profile) sections. The offering's image is the pattern for every layer ([task 07 of increment 02](../02-manage-consultation-offerings/07-offering-image.md)): `UploadConsultationOfferingImageUseCase`, `RemoveConsultationOfferingImageUseCase`, the controller's two routes, `OfferingImageSection` and its mutations. Read its notes.

Work in this order, test first:

1. **A failing REST test:** `PUT /api/expert/profile/photo` as Expert 2 with a JPEG answers 200 with a `photoId`; `GET /api/images/{photoId}` answers the bytes; the shop's Career Coaching detail has that `photoId` on Expert 2 and none on Expert 1.
2. **Test data:** `delete-test-data.sql` also sets the seeded profiles' photo back to none, and then deletes every image that neither an offering nor a profile refers to.
3. **Domain:** `ExpertProfile` gains `Optional<ImageId> photoId`; `OwnExpertProfile` gains `changePhoto` and `removePhoto`.
4. **Schema and adapter:** `V13__expert_photo.sql`; `ExpertProfileEntity` maps `photoId`; `ExpertProfileAdapter` reads and writes it. Every query that builds an `ExpertProfile` selects it: the shop's offering detail, the eligible experts and the customer's case, with their rows.
5. **Use cases and API:** `UploadExpertPhotoUseCase` and `RemoveExpertPhotoUseCase` (lock the expert, then as the offering's); the two routes on `ExpertProfileController`; `photoId` in `ExpertProfileResponse`, `EligibleExpertResponse` and `AssignedExpertResponse`; role matrix rows. Regenerate `openapi.json`, then `npm run generate:api`.
6. **Frontend:** `ExpertPhoto` in `components/`; `useUploadExpertPhoto` and `useRemoveExpertPhoto`; `ExpertPhotoSection` on `ExpertProfilePage`; `ExpertPhoto` in place of the round `ImagePlaceholder` on `OfferingDetailPage`, in `BookAppointmentDialog` and on `ConsultationCasePage`.
7. **Docs:** [backend.md](../../architecture/backend.md#persistence)'s images name the profile's `photo_id` next to `offering.image_id`; [frontend.md](../../architecture/frontend.md#forms) names `ExpertPhoto`.
8. **Check by hand** on the dev server with Playwright: upload a JPEG, replace it with a PNG, remove it; a text file and a file over 2 MB; the photo in the shop, in the expert picker and on the customer's case, round and not distorted for a wide and for a tall picture.

## Acceptance criteria

- [ ] After an upload, **Profile**, the shop's offering detail, the expert picker and Customer 1's case show the photo instead of the placeholder (REST for the routes' `photoId`, Playwright for the pages).
- [ ] A replacement has a new `photoId`, and the old image's URL answers 404; after a removal the placeholder shows again everywhere, and the photo's URL answers 404 (REST).
- [ ] A file that isn't a JPEG, PNG or WebP, and one larger than 2 MB, answer 400; the section shows the message, and the photo stays as it was (REST and Playwright).
- [ ] A body that isn't multipart answers 415.
- [ ] Each expert changes only their own photo: the routes have no id (REST).
- [ ] No image is left behind: after a replacement and a removal, the `image` table has no row that nothing refers to (REST).
- [ ] The offering's image still uploads, replaces and removes as before (the existing tests pass).
- [ ] The role matrix has both rows; `openapi.json` and `schema.d.ts` have the routes and `photoId`; every check passes.

## Tests

- Domain: `OwnExpertProfile` (`changePhoto`, `removePhoto`), `ExpertProfile` with and without a photo
- REST: upload, replace and remove; the old image's 404; an invalid type, a file over 2 MB (as `ImageUploadLimitTest`), 415; `photoId` on the shop's detail, the eligible experts and the customer's case
- Authorization: the role matrix rows; each expert's own photo
- Frontend: none beyond the checks

## Out of scope

- Cropping or resizing the photo: UC-14's out of scope.
- The photo on **My purchases**' cards, on **My cases** and in the admin's list of experts.
- A concurrency test for two uploads at once (spec decision 13).

## Notes
