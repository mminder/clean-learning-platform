# 07 Offering image

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [06](06-offering-lifecycle.md) |
| Status | Todo |

## Goal

The admin uploads, replaces and removes an offering's image. Images are stored in Postgres, served publicly with long cache headers, and shown in the admin page and in the shop instead of the placeholder.

## What to build

Read first: [D-21](../../decisions.md#cross-cutting), [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) (image), [UC-14](../../use-cases/uc-14-manage-expert-profile.md) (the same rules, for photos later), and the plan's [schema](README.md#schema), [API](README.md#api) and [writes](README.md#writes) sections.

Work in this order, test first:

1. **A failing REST test:** Admin 1 uploads a small PNG for an offering; `GET /api/images/{imageId}` returns the same bytes as `image/png`.
2. **Schema:** `V5__create_image.sql` as in the plan's [schema](README.md#schema).
3. **Domain**, test first, in `domain.image`:
   - `ImageType` (`JPEG`, `PNG`, `WEBP`) with its content type, detected from the file's first bytes, not from the file name or the declared content type;
   - `Image` (id, type, bytes): not empty, at most 2 MB; anything else is an `IllegalArgumentException` (400);
   - `ConsultationOffering` gets an optional image id; changing it on an archived offering is refused (`WRONG_OFFERING_STATUS`).
4. **Port and adapter:** `ImageStorePort` (`save`, `load`, `delete`) in `application.image`, implemented by `ImageAdapter` with `ImageEntity`.
5. **Use cases:** `UploadConsultationOfferingImageUseCase` and `RemoveConsultationOfferingImageUseCase` lock the offering first, and replacing deletes the old image, as the plan's [writes](README.md#writes) section says. `GetImageUseCase` loads one image (`NotFoundException` for an unknown id). `DeleteConsultationOfferingUseCase` deletes the offering's image too.
6. **API:**
   - the routes from the plan: `PUT` and `DELETE` on `…/{id}/image`, and `ImageController` with `GET /api/images/{id}`;
   - multipart allows 2 MB per file (Spring's default is 1 MB); a larger upload answers 400, like a wrong type ([UC-14](../../use-cases/uc-14-manage-expert-profile.md#alternative-and-error-flows));
   - `GET /api/images/{id}` sends `Cache-Control: public, max-age=31536000, immutable`. Spring Security writes `no-cache` headers by default; make sure the response keeps this one;
   - `GET /api/images/**` is public in `SecurityConfig`, as [security.md](../../architecture/security.md#roles-by-route-prefix) already lists it;
   - `imageId` (`@Nullable`) in `ConsultationOfferingResponse`, `ShopOfferingResponse` and `ShopOfferingDetailResponse`;
   - role matrix rows: the image upload with no body, the removal with an unknown offering id, and `GET /api/images/{id}` with an unknown id.
7. **Cleanup:** the shared test cleanup script removes the images of the offerings it deletes.
8. **Frontend:**
   - `OfferingImage` in `components/`: the image when there's an `imageId`, otherwise `ImagePlaceholder`, in the same frame;
   - `OfferingImageSection` on `ConsultationOfferingPage` (not on the new page): the image, **Upload image** or **Replace image** (a file input that accepts JPEG, PNG and WebP) and **Remove image**; disabled for an archived offering; a 400 shows its `detail`;
   - the upload goes through the generated client with a `FormData` body;
   - the shop's `OfferingCard` and `OfferingDetailPage` use `OfferingImage`. Expert photos stay placeholders (UC-14).
9. **Docs:** [backend.md](../../architecture/backend.md) and [D-21](../../decisions.md#cross-cutting) say `ImageStorePort` and detection from the first bytes since planning; check that they match what you built.

## Acceptance criteria

- [ ] On the dev server, demo steps 4 and 5 of the [spec](../../specs/02-manage-consultation-offerings.md#demo) work: the admin page shows the uploaded PNG, and the shop shows it on the card and the detail page (Playwright).
- [ ] Replacing an image serves the new one under a new id, and the old id answers 404. After removing it, the shop shows the placeholder.
- [ ] A GIF, a text file named `photo.png`, and a JPEG of 2 MB plus one byte each answer 400, and the offering keeps its image. A JPEG of exactly 2 MB is accepted.
- [ ] `GET /api/images/{id}` works without a login, with the image's content type and `Cache-Control: public, max-age=31536000, immutable`.
- [ ] Changing the image of an archived offering answers 409 `WRONG_OFFERING_STATUS`.
- [ ] Deleting a draft with an image deletes the image too.
- [ ] `openapi.json` and `schema.d.ts` are regenerated; the role matrix has the three routes; every check passes, including the jar build.

## Tests

- Domain: `ImageType` detection (JPEG, PNG, WebP, other), the size limit
- REST: upload, replace, remove, get; the invalid files; archived; delete with image; `imageId` in the shop
- Authorization: `RoleMatrixTest` rows for the three routes

## Out of scope

- Expert photos ([UC-14](../../use-cases/uc-14-manage-expert-profile.md)): they reuse the table, the port and `GET /api/images/{id}`.
- Resizing, cropping or converting images ([D-21](../../decisions.md#cross-cutting)).

## Notes
